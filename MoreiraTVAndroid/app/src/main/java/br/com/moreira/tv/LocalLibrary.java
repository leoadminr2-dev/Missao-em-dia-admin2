package br.com.moreira.tv;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.Set;

/** Biblioteca local por conta, sem salvar logins, URLs privadas ou conteudo de video. */
final class LocalLibrary {
    private LocalLibrary() {}

    static String account(String server, String username) {
        try {
            MessageDigest sha=MessageDigest.getInstance("SHA-256");
            byte[] raw=sha.digest((server+"|"+username).getBytes("UTF-8"));
            StringBuilder sb=new StringBuilder();
            for(int i=0;i<12;i++) sb.append(String.format(java.util.Locale.ROOT,"%02x",raw[i]&255));
            return sb.toString();
        }catch(Exception ignored){return "local";}
    }
    private static SharedPreferences prefs(Context context,String account){
        return context.getSharedPreferences("moreira_play_library_"+account,Context.MODE_PRIVATE);
    }
    private static String key(String kind,String id){return kind+":"+id;}
    private static Set<String> favorites(Context c,String account){
        return new HashSet<>(prefs(c,account).getStringSet("favorites",new HashSet<>()));
    }
    static boolean isFavorite(Context c,String account,String kind,String id) {
        return favorites(c,account).contains(key(kind,id));
    }
    static boolean toggleFavorite(Context c,String account,String kind,String id){
        Set<String> values=favorites(c,account);
        boolean added=values.add(key(kind,id));
        if(!added)values.remove(key(kind,id));
        prefs(c,account).edit().putStringSet("favorites",values).apply();
        return added;
    }
    static JSONArray recent(Context c,String account) {
        try{return new JSONArray(prefs(c,account).getString("recent","[]"));}
        catch(Exception e){return new JSONArray();}
    }
    static boolean isRecent(Context c,String account,String kind,String id){
        JSONArray data=recent(c,account);
        for(int i=0;i<data.length();i++){
            JSONObject item=data.optJSONObject(i);
            if(item!=null&&kind.equals(item.optString("kind"))&&id.equals(item.optString("id")))return true;
        }
        return false;
    }
    static void remember(Context c,String account,String kind,String id,String title,String ext,String image){
        try{
            JSONArray old=recent(c,account),newItems=new JSONArray();
            JSONObject first=new JSONObject();
            first.put("kind",kind);first.put("id",id);first.put("name",title);
            first.put("ext",ext);first.put("stream_icon",image);
            newItems.put(first);
            for(int i=0;i<old.length()&&newItems.length()<40;i++){
                JSONObject item=old.optJSONObject(i);
                if(item==null)continue;
                if(kind.equals(item.optString("kind"))&&id.equals(item.optString("id")))continue;
                newItems.put(item);
            }
            prefs(c,account).edit().putString("recent",newItems.toString()).apply();
        }catch(Exception ignored){}
    }
    static void savePosition(Context c,String account,String kind,String id,long position){
        if(!"movie".equals(kind)&&!"series".equals(kind))return;
        if(id==null||id.isEmpty())return;
        prefs(c,account).edit().putLong("position:"+key(kind,id),Math.max(0,position)).apply();
    }
    static long lastPosition(Context c,String account,String kind,String id){
        return prefs(c,account).getLong("position:"+key(kind,id),0);
    }
}
