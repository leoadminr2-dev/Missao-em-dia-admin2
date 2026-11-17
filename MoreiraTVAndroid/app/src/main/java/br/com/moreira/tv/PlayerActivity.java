package br.com.moreira.tv;

import android.app.Activity;
import android.app.ActivityNotFoundException;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.Toast;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

/**
 * Player em tela cheia: sem barra superior de titulo e sem barra inferior de status.
 * Os controles do Media3 aparecem ao tocar no video e desaparecem automaticamente.
 * Pressionar e segurar o video abre as opcoes (voltar / player externo).
 */
public class PlayerActivity extends Activity {
    private ExoPlayer player;
    private PlayerView playerView;
    private String server,user,pass,type,id,ext,name,playingUrl;
    private boolean fallback=false;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        enableFullscreen();
        Intent i=getIntent();
        server=i.getStringExtra("server");
        user=i.getStringExtra("username");
        pass=i.getStringExtra("password");
        type=i.getStringExtra("type");
        id=i.getStringExtra("id");
        ext=i.getStringExtra("ext");
        name=i.getStringExtra("title");
        if(server==null||user==null||pass==null||type==null||id==null||ext==null){finish();return;}

        FrameLayout layout=new FrameLayout(this);
        layout.setBackgroundColor(Color.BLACK);
        setContentView(layout);
        playerView=new PlayerView(this);
        playerView.setUseController(true);
        playerView.setControllerAutoShow(false);
        playerView.setControllerShowTimeoutMs(3000);
        playerView.setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING);
        playerView.setContentDescription("Toque para mostrar controles. Segure para opcoes.");
        playerView.setLongClickable(true);
        playerView.setOnLongClickListener(v->{showOptions();return true;});
        layout.addView(playerView,new FrameLayout.LayoutParams(-1,-1));

        player=new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);
        player.addListener(new Player.Listener() {
            @Override public void onPlayerError(PlaybackException error) {
                if("live".equals(type)&&!fallback){
                    fallback=true;
                    Toast.makeText(PlayerActivity.this,"Ajustando formato do canal...",Toast.LENGTH_SHORT).show();
                    start("ts");
                } else {
                    new AlertDialog.Builder(PlayerActivity.this)
                        .setTitle("Nao foi possivel reproduzir")
                        .setMessage("Este canal ou video nao abriu neste dispositivo. Voce pode tentar outro player.")
                        .setPositiveButton("Outro player",(d,w)->openExternal())
                        .setNegativeButton("Voltar",(d,w)->finish())
                        .setNeutralButton("Fechar",null)
                        .show();
                }
            }
        });
        start(ext);
    }

    private void enableFullscreen() {
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
            View.SYSTEM_UI_FLAG_FULLSCREEN |
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }

    @Override public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if(hasFocus)enableFullscreen();
    }

    private void showOptions() {
        new AlertDialog.Builder(this)
            .setTitle(name==null?"Moreira TV":name)
            .setItems(new String[]{"Voltar a programacao","Abrir em outro player","Continuar assistindo"},(d,choice)->{
                if(choice==0)finish();
                else if(choice==1)openExternal();
                else enableFullscreen();
            })
            .show();
    }

    private String escape(String v){return Uri.encode(v);}
    private void start(String format) {
        String prefix="live".equals(type)?"live":"movie".equals(type)?"movie":"series";
        playingUrl=server+"/"+prefix+"/"+escape(user)+"/"+escape(pass)+"/"+escape(id)+"."+format;
        MediaItem.Builder builder=new MediaItem.Builder().setUri(Uri.parse(playingUrl));
        if(format.equalsIgnoreCase("m3u8"))builder.setMimeType(MimeTypes.APPLICATION_M3U8);
        player.setMediaItem(builder.build());
        player.prepare();
        player.play();
    }

    private void openExternal() {
        try {
            Intent intent=new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.parse(playingUrl),"video/*");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(Intent.createChooser(intent,"Abrir video com"));
        } catch(ActivityNotFoundException e) {
            Toast.makeText(this,"Instale um player compativel, como VLC para Android.",Toast.LENGTH_LONG).show();
        }
    }

    @Override protected void onStop(){if(player!=null)player.pause();super.onStop();}
    @Override protected void onStart(){super.onStart();if(player!=null&&player.getMediaItemCount()>0)player.play();}
    @Override protected void onDestroy(){if(player!=null){player.release();player=null;}super.onDestroy();}
}
