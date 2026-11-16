package br.com.moreira.tv;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import org.json.JSONObject;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/** Acesso local armazenado com AES-GCM e uma chave gerada pelo Android Keystore. */
final class CredentialsVault {
    private static final String PREFS="moreira_secure_access";
    private static final String ALIAS="moreira_tv_premium_aes";
    private CredentialsVault(){}
    private static SharedPreferences prefs(Context c){
        return c.getSharedPreferences(PREFS,Context.MODE_PRIVATE);
    }
    private static SecretKey key()throws Exception {
        KeyStore ks=KeyStore.getInstance("AndroidKeyStore");ks.load(null);
        if(ks.containsAlias(ALIAS)){
            return ((KeyStore.SecretKeyEntry)ks.getEntry(ALIAS,null)).getSecretKey();
        }
        KeyGenerator kg=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");
        kg.init(new KeyGenParameterSpec.Builder(ALIAS,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256).build());
        return kg.generateKey();
    }
    static boolean save(Context c,String host,String user,String pass){
        try{
            JSONObject data=new JSONObject();
            data.put("host",host);data.put("user",user);data.put("pass",pass);
            Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE,key());
            byte[] bytes=cipher.doFinal(data.toString().getBytes("UTF-8"));
            return prefs(c).edit()
                .putString("payload",Base64.encodeToString(bytes,Base64.NO_WRAP))
                .putString("iv",Base64.encodeToString(cipher.getIV(),Base64.NO_WRAP))
                .commit();
        }catch(Exception e){return false;}
    }
    static String[] load(Context c){
        String payload=prefs(c).getString("payload",null),iv=prefs(c).getString("iv",null);
        if(payload==null||iv==null)return null;
        try{
            Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE,key(),new GCMParameterSpec(128,Base64.decode(iv,Base64.NO_WRAP)));
            JSONObject data=new JSONObject(new String(cipher.doFinal(Base64.decode(payload,Base64.NO_WRAP)),"UTF-8"));
            return new String[]{data.getString("host"),data.getString("user"),data.getString("pass")};
        }catch(Exception e){clear(c);return null;}
    }
    static void clear(Context c){prefs(c).edit().clear().apply();}
}
