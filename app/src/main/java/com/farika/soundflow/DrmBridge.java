package com.farika.soundflow;

import android.net.Uri;
import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.MediaItem;
import androidx.media3.datasource.DataSpec;
import androidx.media3.exoplayer.drm.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Android's own CDM owns keys. Only genuine challenges/responses are exchanged with the service.
 * Reference: KittyTune's Media3/FrameworkMediaDrm integration (GPL-3.0); see THIRD-PARTY-NOTICES.
 * No device certificates, clear keys, server relay, decryption or replacement audio. */
public final class DrmBridge {
 private DrmBridge(){}
 public static boolean supported(){return FrameworkMediaDrm.isCryptoSchemeSupported(C.WIDEVINE_UUID);}
 public static DefaultDrmSessionManager streaming(String token)throws IOException{
  if(!supported())throw new IOException("На устройстве нет доступного системного Widevine. Защищённый трек не запустится.");
  if(!StreamPolicy.tokenValid(token))throw new IOException("SoundCloud не выдал действительную авторизацию лицензии");
  return manager(new LicenseCallback(token,false));
 }
 private static DefaultDrmSessionManager manager(MediaDrmCallback callback){return new DefaultDrmSessionManager.Builder().setUuidAndExoMediaDrmProvider(C.WIDEVINE_UUID,FrameworkMediaDrm.DEFAULT_PROVIDER).setMultiSession(true).build(callback);}
 public static OfflineLicenseHelper offlineHelper(String token)throws IOException{
  if(!supported())throw new IOException("На устройстве нет системного Widevine для офлайн-лицензии");
  if(!StreamPolicy.tokenValid(token))throw new IOException("SoundCloud не выдал авторизацию офлайн-лицензии");
  return new OfflineLicenseHelper(manager(new LicenseCallback(token,true)),new DrmSessionEventListener.EventDispatcher());
 }
 public static DefaultDrmSessionManager offline(byte[] keySetId)throws IOException{
  if(!supported())throw new IOException("Системный Widevine недоступен");
  if(keySetId==null||keySetId.length==0)throw new IOException("Офлайн-лицензия отсутствует. Загрузите трек заново при подключении к сети.");
  DefaultDrmSessionManager drm=manager(new NoNetworkCallback());drm.setMode(DefaultDrmSessionManager.MODE_PLAYBACK,keySetId);return drm;
 }
 public static MediaItem item(String url,boolean hls,boolean drm){MediaItem.Builder b=new MediaItem.Builder().setUri(url);if(hls)b.setMimeType(androidx.media3.common.MimeTypes.APPLICATION_M3U8);if(drm)b.setDrmConfiguration(new MediaItem.DrmConfiguration.Builder(C.WIDEVINE_UUID).setMultiSession(true).build());return b.build();}
 private static MediaDrmCallbackException failure(String code){Uri safe=Uri.parse("https://license.media-streaming.soundcloud.cloud/");return new MediaDrmCallbackException(new DataSpec(safe),safe,Collections.emptyMap(),0,new IOException(code));}
 private static final class NoNetworkCallback implements MediaDrmCallback {
  public byte[] executeKeyRequest(UUID id,ExoMediaDrm.KeyRequest request)throws MediaDrmCallbackException{throw failure("OFFLINE_LICENSE_EXPIRED_OR_MISSING");}
  public byte[] executeProvisionRequest(UUID id,ExoMediaDrm.ProvisionRequest request)throws MediaDrmCallbackException{throw failure("OFFLINE_DEVICE_PROVISIONING_REQUIRED");}
 }
 private static final class LicenseCallback implements MediaDrmCallback {
  private final String token;private final boolean offline;
  LicenseCallback(String token,boolean offline){this.token=token;this.offline=offline;}
  public byte[] executeKeyRequest(UUID id,ExoMediaDrm.KeyRequest request)throws MediaDrmCallbackException{
   if(!C.WIDEVINE_UUID.equals(id))throw failure("UNSUPPORTED_DRM");
   try{return post("https://license.media-streaming.soundcloud.cloud/"+(offline?"persistable":"playback")+"/widevine?license_token="+URLEncoder.encode(token,"UTF-8"),request.getData(),false);}
   catch(Exception e){throw failure(code(e));}
  }
  public byte[] executeProvisionRequest(UUID id,ExoMediaDrm.ProvisionRequest request)throws MediaDrmCallbackException{
   try{String raw=request.getDefaultUrl();URI u=new URI(raw);String h=u.getHost();if(!"https".equals(u.getScheme())||u.getRawUserInfo()!=null||(u.getPort()!=-1&&u.getPort()!=443)||!("www.googleapis.com".equals(h)||"android.clients.google.com".equals(h)))throw new IOException("UNTRUSTED_PROVISION_SERVER");
    return post(raw+(raw.contains("?")?"&":"?")+"signedRequest="+URLEncoder.encode(new String(request.getData(),StandardCharsets.UTF_8),"UTF-8"),new byte[0],true);
   }catch(Exception e){throw failure(code(e));}
  }
 }
 private static final class Status extends IOException {final int status;Status(int s){super("DRM_HTTP_"+s);status=s;}}
 private static byte[] post(String url,byte[] body,boolean provision)throws Exception{
  if(Thread.currentThread().isInterrupted())throw new InterruptedIOException();
  HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();c.setInstanceFollowRedirects(false);c.setConnectTimeout(20000);c.setReadTimeout(25000);c.setRequestMethod("POST");c.setDoOutput(true);c.setRequestProperty("Content-Type","application/octet-stream");c.setRequestProperty("Accept","application/octet-stream");c.setFixedLengthStreamingMode(body.length);
  try{try(OutputStream out=c.getOutputStream()){out.write(body);}int status=c.getResponseCode();if(status<200||status>=300)throw new Status(status);
   try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1){if(out.size()+n>2*1024*1024)throw new IOException("DRM_RESPONSE_TOO_LARGE");out.write(b,0,n);}if(out.size()==0)throw new IOException("EMPTY_DRM_RESPONSE");return out.toByteArray();}
  }finally{c.disconnect();}
 }
 private static String code(Exception e){if(e instanceof Status)return "DRM_HTTP_"+((Status)e).status;if(e instanceof SocketTimeoutException)return "DRM_TIMEOUT";if(e instanceof javax.net.ssl.SSLException)return "DRM_TLS_ERROR";if(e instanceof UnknownHostException)return "DRM_DNS_ERROR";return "DRM_NETWORK_OR_PROTOCOL_ERROR";}
 public static String publicError(Throwable e){if(!supported())return "На устройстве нет доступного системного Widevine. Защищённые треки воспроизводить нельзя.";for(Throwable t=e;t!=null;t=t.getCause()){String m=t.getMessage();if(m!=null&&m.matches("DRM_HTTP_[0-9]{3}"))return "Сервер лицензий SoundCloud отказал ("+m+"). Доступ не изменён.";if(m!=null&&m.startsWith("OFFLINE_"))return "Офлайн-лицензия истекла или недоступна на этом устройстве. Подключитесь к сети и загрузите трек заново.";}return "Не удалось получить или восстановить лицензию SoundCloud. Проверьте сеть, аккаунт и поддержку Widevine.";}
}
