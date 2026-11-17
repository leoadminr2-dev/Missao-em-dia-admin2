package br.com.moreira.tv;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.widget.Toast;
import androidx.core.content.FileProvider;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Locale;

/** Atualizador privado do Moreira PLAY; instala apenas pacote de identidade e hash validados. */
public final class UpdateManager {
    private static final String HOST="moreira-play-updates.pages.dev";
    private static final String MANIFEST="https://moreira-play-updates.pages.dev/manifest.json";
    private static final long MAX_APK_BYTES=65L*1024*1024;
    private static boolean busy=false;
    private static File pendingInstaller;
    private UpdateManager(){}

    private static void ui(Activity a, Runnable r){
        a.runOnUiThread(()->{if(!a.isFinishing()&&!a.isDestroyed())r.run();});
    }
    private static HttpURLConnection connect(String address) throws Exception {
        URL url=new URL(address);
        if(!"https".equalsIgnoreCase(url.getProtocol()) || !HOST.equalsIgnoreCase(url.getHost()))
            throw new SecurityException("Servidor de atualizacao nao autorizado");
        HttpURLConnection conn=(HttpURLConnection)url.openConnection();
        conn.setConnectTimeout(12000);conn.setReadTimeout(25000);
        conn.setRequestProperty("User-Agent","MoreiraPLAY/"+BuildConfig.VERSION_NAME);
        conn.setUseCaches(false);conn.setInstanceFollowRedirects(false);
        return conn;
    }
    private static String manifestText() throws Exception {
        HttpURLConnection c=connect(MANIFEST);
        try {
            if(c.getResponseCode()!=200)throw new IllegalStateException("Central indisponivel ("+c.getResponseCode()+")");
            ByteArrayOutputStream out=new ByteArrayOutputStream();
            try(InputStream in=c.getInputStream()){
                byte[] buf=new byte[4096];int n;
                while((n=in.read(buf))!=-1){
                    if(out.size()+n>20000)throw new SecurityException("Manifesto excessivo");
                    out.write(buf,0,n);
                }
            }
            return out.toString("UTF-8");
        }finally{c.disconnect();}
    }
    public static void check(Activity activity, boolean manual) {
        synchronized(UpdateManager.class){if(busy)return;busy=true;}
        new Thread(()->{
            try{
                JSONObject json=new JSONObject(manifestText());
                String slug=json.optString("app_slug");
                String ver=json.optString("version");
                int code=json.optInt("version_code");
                String url=json.optString("apk_url");
                String sha=json.optString("sha256").toLowerCase(Locale.ROOT);
                if(!"moreira-play".equals(slug)||code<1||!sha.matches("[0-9a-f]{64}"))
                    throw new SecurityException("Manifesto Moreira PLAY invalido");
                URL candidate=new URL(url);
                if(!"https".equalsIgnoreCase(candidate.getProtocol())||!HOST.equalsIgnoreCase(candidate.getHost())||!candidate.getPath().endsWith(".apk"))
                    throw new SecurityException("APK nao autorizado");
                if(code<=BuildConfig.VERSION_CODE){
                    if(manual)ui(activity,()->Toast.makeText(activity,"Moreira PLAY "+BuildConfig.VERSION_NAME+" atualizado.",Toast.LENGTH_LONG).show());
                    return;
                }
                int last=activity.getSharedPreferences("moreira_play_updates",Activity.MODE_PRIVATE).getInt("last_offered",0);
                if(!manual && last==code)return;
                ui(activity,()->{
                    activity.getSharedPreferences("moreira_play_updates",Activity.MODE_PRIVATE).edit().putInt("last_offered",code).apply();
                    new AlertDialog.Builder(activity)
                        .setTitle("Atualizacao Moreira PLAY")
                        .setMessage("Versao "+ver+" disponivel. Atualizar sem perder seu acesso e seus favoritos?")
                        .setNegativeButton("Depois",null)
                        .setPositiveButton("Baixar e atualizar",(d,w)->download(activity,url,sha,code))
                        .show();
                });
            }catch(Exception ex){
                if(manual)ui(activity,()->Toast.makeText(activity,"Nao foi possivel consultar atualizacoes: "+ex.getClass().getSimpleName(),Toast.LENGTH_LONG).show());
            }finally{synchronized(UpdateManager.class){busy=false;}}
        },"moreira-play-check").start();
    }
    private static String sha256(File f) throws Exception{
        MessageDigest md=MessageDigest.getInstance("SHA-256");
        try(InputStream in=new java.io.FileInputStream(f)){
            byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)md.update(b,0,n);
        }
        StringBuilder out=new StringBuilder();
        for(byte b:md.digest())out.append(String.format(Locale.ROOT,"%02x",b&255));
        return out.toString();
    }
    private static boolean sameSigner(PackageManager pm,PackageInfo expected,PackageInfo actual){
        try {
            Signature[] installed,downloaded;
            if(Build.VERSION.SDK_INT>=28){
                if(expected.signingInfo==null||actual.signingInfo==null)return false;
                installed=expected.signingInfo.getApkContentsSigners();
                downloaded=actual.signingInfo.getApkContentsSigners();
            }else{
                installed=expected.signatures;downloaded=actual.signatures;
            }
            return installed!=null&&downloaded!=null&&installed.length==1&&downloaded.length==1&&Arrays.equals(installed[0].toByteArray(),downloaded[0].toByteArray());
        }catch(Exception e){return false;}
    }
    private static void validate(Activity a,File file,int version) throws Exception{
        PackageManager pm=a.getPackageManager();
        int flags=Build.VERSION.SDK_INT>=28?PackageManager.GET_SIGNING_CERTIFICATES:PackageManager.GET_SIGNATURES;
        PackageInfo current=pm.getPackageInfo(a.getPackageName(),flags);
        PackageInfo apk=pm.getPackageArchiveInfo(file.getAbsolutePath(),flags);
        if(apk==null||!a.getPackageName().equals(apk.packageName))throw new SecurityException("Pacote de outro aplicativo");
        long target=Build.VERSION.SDK_INT>=28?apk.getLongVersionCode():apk.versionCode;
        if(target<=BuildConfig.VERSION_CODE||target!=version)throw new SecurityException("Versao do APK invalida");
        if(!sameSigner(pm,current,apk))throw new SecurityException("Assinatura digital da atualizacao nao confere");
    }
    private static void download(Activity activity,String url,String expectedSha,int targetVersion) {
        Toast.makeText(activity,"Baixando atualizacao Moreira PLAY...",Toast.LENGTH_LONG).show();
        new Thread(()->{
            File temp=null;
            try{
                File dir=new File(activity.getCacheDir(),"moreira-updates");
                if(!dir.exists()&&!dir.mkdirs())throw new IllegalStateException("Sem espaco para download");
                temp=new File(dir,"pending.apk");if(temp.exists())temp.delete();
                HttpURLConnection c=connect(url);
                try{
                    if(c.getResponseCode()!=200)throw new IllegalStateException("Download indisponivel");
                    if(c.getContentLengthLong()>MAX_APK_BYTES)throw new SecurityException("APK excessivo");
                    try(InputStream in=c.getInputStream();FileOutputStream out=new FileOutputStream(temp)){
                        byte[] b=new byte[8192];int n;long total=0;
                        while((n=in.read(b))!=-1){total+=n;if(total>MAX_APK_BYTES)throw new SecurityException("APK excessivo");out.write(b,0,n);}
                        out.getFD().sync();
                    }
                }finally{c.disconnect();}
                if(!sha256(temp).equals(expectedSha))throw new SecurityException("Integridade do APK nao confere");
                validate(activity,temp,targetVersion);
                File ready=new File(dir,"moreira-play-update.apk");
                if(ready.exists())ready.delete();
                if(!temp.renameTo(ready))throw new IllegalStateException("Falha ao preparar pacote");
                pendingInstaller=ready;
                ui(activity,()->install(activity,ready));
            }catch(Exception e){
                if(temp!=null)temp.delete();
                ui(activity,()->new AlertDialog.Builder(activity).setTitle("Atualizacao interrompida")
                    .setMessage("O Moreira PLAY atual permanece funcionando. Motivo: "+e.getMessage())
                    .setPositiveButton("OK",null).show());
            }
        },"moreira-play-download").start();
    }
    private static void install(Activity activity,File apk) {
        if(Build.VERSION.SDK_INT>=26&&!activity.getPackageManager().canRequestPackageInstalls()){
            new AlertDialog.Builder(activity).setTitle("Permitir instalacao")
                .setMessage("O Android precisa autorizar o Moreira PLAY a instalar sua atualizacao. Volte ao aplicativo apos conceder a permissao.")
                .setNegativeButton("Agora nao",null)
                .setPositiveButton("Abrir configuracao",(d,w)->{
                    Intent i=new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,Uri.parse("package:"+activity.getPackageName()));
                    activity.startActivity(i);
                }).show();
            return;
        }
        try{
            Uri uri=FileProvider.getUriForFile(activity,activity.getPackageName()+".fileprovider",apk);
            Intent i=new Intent(Intent.ACTION_VIEW);
            i.setDataAndType(uri,"application/vnd.android.package-archive");
            i.setClipData(ClipData.newRawUri("Moreira PLAY",uri));
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            activity.startActivity(i);
            pendingInstaller=null;
        }catch(Exception e){Toast.makeText(activity,"Nao foi possivel abrir instalador Android.",Toast.LENGTH_LONG).show();}
    }
    public static void resumePendingInstall(Activity a){
        if(pendingInstaller!=null&&pendingInstaller.exists()&&(Build.VERSION.SDK_INT<26||a.getPackageManager().canRequestPackageInstalls()))
            install(a,pendingInstaller);
    }
}
