package br.com.moreira.tv;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

public class PlayerActivity extends Activity {
    private ExoPlayer player;private PlayerView playerView;private String server,user,pass,type,id,ext,name,playingUrl;
    private TextView status;private boolean fallback=false;
    @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);getWindow().setStatusBarColor(Color.BLACK);
        Intent i=getIntent();server=i.getStringExtra("server");user=i.getStringExtra("username");pass=i.getStringExtra("password");type=i.getStringExtra("type");id=i.getStringExtra("id");ext=i.getStringExtra("ext");name=i.getStringExtra("title");
        if(server==null||user==null||pass==null||type==null||id==null||ext==null){finish();return;}
        FrameLayout layout=new FrameLayout(this);layout.setBackgroundColor(Color.BLACK);setContentView(layout);
        playerView=new PlayerView(this);playerView.setUseController(true);layout.addView(playerView,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout heading=new LinearLayout(this);heading.setOrientation(LinearLayout.HORIZONTAL);heading.setGravity(Gravity.CENTER_VERTICAL);heading.setPadding(12,5,12,5);heading.setBackgroundColor(0xCC101828);
        Button close=new Button(this);close.setText("‹ VOLTAR");close.setOnClickListener(v->finish());heading.addView(close);
        TextView title=new TextView(this);title.setText(name==null?"Moreira TV":name);title.setTextColor(Color.WHITE);title.setTextSize(17);title.setSingleLine(true);title.setPadding(10,0,10,0);heading.addView(title,new LinearLayout.LayoutParams(0,-2,1));
        Button external=new Button(this);external.setText("OUTRO PLAYER");external.setOnClickListener(v->openExternal());heading.addView(external);
        FrameLayout.LayoutParams hp=new FrameLayout.LayoutParams(-1,-2,Gravity.TOP);layout.addView(heading,hp);
        status=new TextView(this);status.setTextColor(Color.WHITE);status.setTextSize(14);status.setPadding(18,13,18,13);status.setBackgroundColor(0xCC0A1934);status.setText("Preparando transmissão...");FrameLayout.LayoutParams sp=new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM);layout.addView(status,sp);
        player=new ExoPlayer.Builder(this).build();playerView.setPlayer(player);
        player.addListener(new Player.Listener(){@Override public void onPlayerError(PlaybackException error){if("live".equals(type)&&!fallback){fallback=true;status.setText("Testando formato alternativo TS...");start("ts");}else{status.setText("Formato indisponível neste dispositivo ou acesso recusado. Tente OUTRO PLAYER.");}}@Override public void onPlaybackStateChanged(int state){if(state==Player.STATE_READY)status.setText("▶ Reprodução pronta");else if(state==Player.STATE_BUFFERING)status.setText("Carregando vídeo...");}});
        start(ext);
    }
    private String escape(String v){return Uri.encode(v);}
    private void start(String format){
        String prefix="live".equals(type)?"live":"movie".equals(type)?"movie":"series";
        playingUrl=server+"/"+prefix+"/"+escape(user)+"/"+escape(pass)+"/"+escape(id)+"."+format;
        MediaItem.Builder b=new MediaItem.Builder().setUri(Uri.parse(playingUrl));
        if(format.equalsIgnoreCase("m3u8"))b.setMimeType(MimeTypes.APPLICATION_M3U8);
        player.setMediaItem(b.build());player.prepare();player.play();
    }
    private void openExternal(){try{Intent intent=new Intent(Intent.ACTION_VIEW);intent.setDataAndType(Uri.parse(playingUrl),"video/*");intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);startActivity(Intent.createChooser(intent,"Abrir vídeo com"));}catch(ActivityNotFoundException e){status.setText("Instale um player compatível, como VLC para Android.");}}
    @Override protected void onStop(){if(player!=null)player.pause();super.onStop();}
    @Override protected void onStart(){super.onStart();if(player!=null&&player.getMediaItemCount()>0)player.play();}
    @Override protected void onDestroy(){if(player!=null){player.release();player=null;}super.onDestroy();}
}
