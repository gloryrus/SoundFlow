package com.farika.soundflow;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/** Session values stay on-device; never written to logs or network query strings. */
public final class Session {
 private static SecretKey key() throws Exception {
  KeyStore k=KeyStore.getInstance("AndroidKeyStore");k.load(null);
  if(k.containsAlias("volna_session"))return (SecretKey)k.getKey("volna_session",null);
  KeyGenerator g=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");
  g.init(new KeyGenParameterSpec.Builder("volna_session",KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());return g.generateKey();
 }
 public static void put(Context c,String token)throws Exception{put(c,token,"");}
 public static void put(Context c,String token,String client)throws Exception{
  Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,key());
  String value=Base64.encodeToString(cipher.getIV(),Base64.NO_WRAP)+":"+Base64.encodeToString(cipher.doFinal(token.getBytes("UTF-8")),Base64.NO_WRAP);
  if(!c.getSharedPreferences("session",0).edit().putString("encrypted",value).putString("client",client.matches("[A-Za-z0-9_-]{16,256}")?client:"").commit())throw new Exception("Не удалось сохранить сессию");
 }
 public static String client(Context c){return c.getSharedPreferences("session",0).getString("client","");}
 public static void migrateClient(Context c){android.content.SharedPreferences p=c.getSharedPreferences("session",0);if(!p.contains("client")&&!get(c).isEmpty()){String id=c.getSharedPreferences("web_config",0).getString("id","");p.edit().putString("client",id.matches("[A-Za-z0-9_-]{16,256}")?id:"").apply();}}
 public static String get(Context c){try{
  String s=c.getSharedPreferences("session",0).getString("encrypted","");if(s.isEmpty())return "";String[] p=s.split(":",2);
  Cipher ci=Cipher.getInstance("AES/GCM/NoPadding");ci.init(Cipher.DECRYPT_MODE,key(),new GCMParameterSpec(128,Base64.decode(p[0],Base64.NO_WRAP)));return new String(ci.doFinal(Base64.decode(p[1],Base64.NO_WRAP)),"UTF-8");
 }catch(Exception e){return "";}}
 public static void clear(Context c){c.getSharedPreferences("session",0).edit().clear().commit();}
}
