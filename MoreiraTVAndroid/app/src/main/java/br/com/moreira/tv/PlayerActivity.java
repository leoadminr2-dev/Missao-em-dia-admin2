package br.com.moreira.tv;

import android.app.Activity;
import android.app.ActivityNotFoundException;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.Toast;
import java.util.ArrayList;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;
import androidx.media3.ui.AspectRatioFrameLayout;

/** Player Moreira PLAY: tela cheia por padrao, swipe entre canais e recuperacao de sinal. */
public class PlayerActivity extends Activity {
    private ExoPlayer player;
    private PlayerView playerView;
    private String server,user,pass,type,id,ext,name,playingUrl,account,activeFormat;
    private ArrayList<String> channelIds,channelNames;
    private int channelIndex=-1, retries=0;
    private boolean fallback=false,fillScreen=true,resumeApplied=false,controllerVisible=false;
    private long savedPosition=0;
    private final Handler mainHandler=new Handler(Looper.getMainLooper());

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        enableFullscreen();
        Intent i=getIntent();
        server=i.getStringExtra("server");user=i.getStringExtra("username");
        pass=i.getStringExtra("password");type=i.getStringExtra("type");
        id=i.getStringExtra("id");ext=i.getStringExtra("ext");
        name=i.getStringExtra("title");account=i.getStringExtra("account");
        savedPosition=i.getLongExtra("resume_position",0);
        channelIds=i.getStringArrayListExtra("channel_ids");
        channelNames=i.getStringArrayListExtra("channel_names");
        channelIndex=i.getIntExtra("channel_index",-1);
        if(server==null||user==null||pass==null||type==null||id==null||ext==null){finish();return;}
        if(account==null)account=LocalLibrary.account(server,user);

        FrameLayout layout=new FrameLayout(this);
        layout.setBackgroundColor(Color.BLACK);setContentView(layout);
        playerView=new PlayerView(this);
        playerView.setUseController(true);
        playerView.setControllerAutoShow(false);
        playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_ZOOM);
        playerView.setControllerShowTimeoutMs(3000);
        playerView.setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING);
        playerView.setContentDescription("Toque para mostrar controles. Deslize para trocar de canal. Segure para opcoes.");
        layout.addView(playerView,new FrameLayout.LayoutParams(-1,-1));

        GestureDetector detector=new GestureDetector(this,new GestureDetector.SimpleOnGestureListener(){
            @Override public boolean onDown(MotionEvent e){return true;}
            @Override public boolean onSingleTapConfirmed(MotionEvent e){
                if(controllerVisible){playerView.hideController();controllerVisible=false;}
                else{playerView.showController();controllerVisible=true;}
                return true;
            }
            @Override public boolean onFling(MotionEvent first,MotionEvent last,float velocityX,float velocityY){
                if(first==null||last==null||!"live".equals(type))return false;
                float dx=last.getX()-first.getX(),dy=last.getY()-first.getY();
                if(Math.abs(dx)<100*getResources().getDisplayMetrics().density || Math.abs(dx)<Math.abs(dy)*1.4f)return false;
                switchChannel(dx<0?1:-1);return true;
            }
            @Override public void onLongPress(MotionEvent e){showOptions();}
        });
        playerView.setOnTouchListener((view,event)->{detector.onTouchEvent(event);return true;});

        player=new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);
        player.addListener(new Player.Listener(){
            @Override public void onPlaybackStateChanged(int state){
                if(state==Player.STATE_READY){
                    retries=0;
                    if(!"live".equals(type)&&!resumeApplied&&savedPosition>30000){
                        resumeApplied=true;
                        player.seekTo(savedPosition);
                    }
                }else if(state==Player.STATE_ENDED && !"live".equals(type)){
                    LocalLibrary.savePosition(PlayerActivity.this,account,type,id,0);
                }
            }
            @Override public void onPlayerError(PlaybackException error){
                if("live".equals(type)&&!fallback&&"m3u8".equalsIgnoreCase(activeFormat)){
                    fallback=true;
                    Toast.makeText(PlayerActivity.this,"Ajustando formato do canal…",Toast.LENGTH_SHORT).show();
                    start("ts");return;
                }
                if(retries<2){
                    int attempt=++retries;
                    String retryFormat=activeFormat;
                    Toast.makeText(PlayerActivity.this,"Reconectando transmissão ("+attempt+"/2)…",Toast.LENGTH_SHORT).show();
                    mainHandler.postDelayed(()->{
                        if(!isFinishing()&&player!=null&&retryFormat.equals(activeFormat))start(retryFormat);
                    },1200L*attempt);
                    return;
                }
                new AlertDialog.Builder(PlayerActivity.this)
                    .setTitle("Transmissão indisponível")
                    .setMessage("Não foi possível recuperar este conteúdo. Você pode tentar outro player ou voltar à programação.")
                    .setPositiveButton("Outro player",(d,w)->openExternal())
                    .setNegativeButton("Voltar",(d,w)->finish())
                    .setNeutralButton("Fechar",null).show();
            }
        });
        start(ext);
    }
    private void enableFullscreen(){
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY|View.SYSTEM_UI_FLAG_FULLSCREEN|
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }
    private void applyVideoLayout(){
        enableFullscreen();
        if(playerView!=null){
            playerView.setResizeMode(fillScreen?AspectRatioFrameLayout.RESIZE_MODE_ZOOM:AspectRatioFrameLayout.RESIZE_MODE_FIT);
            playerView.hideController();controllerVisible=false;
        }
    }
    @Override protected void onResume(){super.onResume();applyVideoLayout();}
    @Override public void onWindowFocusChanged(boolean focus){super.onWindowFocusChanged(focus);if(focus)enableFullscreen();}

    private boolean canChangeChannel(){return "live".equals(type)&&channelIds!=null&&channelIds.size()>1;}
    private void switchChannel(int direction){
        if(!canChangeChannel())return;
        int next=channelIndex+direction;
        if(next<0||next>=channelIds.size()){
            Toast.makeText(this,"Fim da lista de canais",Toast.LENGTH_SHORT).show();
            return;
        }
        channelIndex=next;id=channelIds.get(next);
        name=channelNames!=null&&next<channelNames.size()?channelNames.get(next):"Canal";
        fallback=false;retries=0;resumeApplied=false;savedPosition=0;
        LocalLibrary.remember(this,account,"live",id,name,"m3u8","");
        Toast.makeText(this,name,Toast.LENGTH_SHORT).show();
        start("m3u8");
    }
    private void showOptions(){
        ArrayList<String> options=new ArrayList<>();
        options.add("Voltar à programação");
        if(canChangeChannel()){options.add("Canal anterior");options.add("Próximo canal");}
        options.add("Abrir em outro player");
        options.add(fillScreen?"Mostrar imagem inteira (sem cortar)":"Preencher tela inteira");
        options.add("Continuar assistindo");
        new AlertDialog.Builder(this).setTitle(name==null?"Moreira PLAY":name)
            .setItems(options.toArray(new String[0]),(d,which)->{
                String selected=options.get(which);
                if(selected.startsWith("Voltar"))finish();
                else if(selected.equals("Canal anterior"))switchChannel(-1);
                else if(selected.equals("Próximo canal"))switchChannel(1);
                else if(selected.equals("Abrir em outro player"))openExternal();
                else if(selected.startsWith("Mostrar")||selected.startsWith("Preencher")){
                    fillScreen=!fillScreen;
                    applyVideoLayout();
                }else applyVideoLayout();
            }).setNegativeButton("Fechar",null).show();
    }
    private String escape(String v){return Uri.encode(v);}
    private void start(String format){
        activeFormat=format;
        applyVideoLayout();
        String prefix="live".equals(type)?"live":"movie".equals(type)?"movie":"series";
        playingUrl=server+"/"+prefix+"/"+escape(user)+"/"+escape(pass)+"/"+escape(id)+"."+format;
        MediaItem.Builder builder=new MediaItem.Builder().setUri(Uri.parse(playingUrl));
        if("m3u8".equalsIgnoreCase(format))builder.setMimeType(MimeTypes.APPLICATION_M3U8);
        player.setMediaItem(builder.build());
        player.prepare();player.play();
    }
    private void openExternal(){
        try{
            Intent intent=new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.parse(playingUrl),"video/*");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(Intent.createChooser(intent,"Abrir vídeo com"));
        }catch(ActivityNotFoundException e){
            Toast.makeText(this,"Instale um player compatível, como VLC para Android.",Toast.LENGTH_LONG).show();
        }
    }
    private void rememberPosition(){
        if(player==null||"live".equals(type)||player.getPlaybackState()==Player.STATE_ENDED)return;
        long position=player.getCurrentPosition();
        if(position>=0)LocalLibrary.savePosition(this,account,type,id,position);
    }
    @Override public boolean onKeyDown(int code,KeyEvent event){
        if(code==KeyEvent.KEYCODE_CHANNEL_UP){switchChannel(-1);return true;}
        if(code==KeyEvent.KEYCODE_CHANNEL_DOWN){switchChannel(1);return true;}
        return super.onKeyDown(code,event);
    }
    @Override protected void onStop(){rememberPosition();if(player!=null)player.pause();super.onStop();}
    @Override protected void onStart(){super.onStart();if(player!=null&&player.getMediaItemCount()>0)player.play();}
    @Override protected void onDestroy(){
        rememberPosition();mainHandler.removeCallbacksAndMessages(null);
        if(player!=null){player.release();player=null;}super.onDestroy();
    }
}
