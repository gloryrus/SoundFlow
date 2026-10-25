package com.farika.soundflow;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import org.json.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.*;

/** Experimental direct adapter: website-session reads and mutations. Optional loopback TLS fragmentation; no remote proxy, borrowed secret or custom backend. */
public class ScApi {
 public static final String BASE="https://api-v2.soundcloud.com",OFFICIAL="https://api.soundcloud.com";
 private final Context context;private final String overrideToken,overrideClient;
 public ScApi(Context c){this(c,null);}
 public ScApi(Context c,String token){this(c,token,null);}
 public ScApi(Context c,String token,String client){context=c.getApplicationContext();overrideToken=token;overrideClient=client;}
 public static class ApiError extends IOException { public String stage="REQUEST";public ApiError at(String value){stage=value;return this;}public String reason="",responseKind="UNKNOWN";public long retryAfterMs=0;public final int status;ApiError(int s){super("SoundCloud: HTTP "+s+(s==401?". Войдите заново.":s==403?". Доступ к этому ресурсу ограничен.":s==429?". Слишком много запросов, попробуйте позже.":""));status=s;} }
 public static class Page { public final ArrayList<Library.Track> tracks=new ArrayList<>();public final ArrayList<JSONObject> playlists=new ArrayList<>();public String next="",note=""; }
 public static boolean trusted(String url){try{URI u=new URI(url);String h=u.getHost();return "https".equals(u.getScheme())&&u.getRawUserInfo()==null&&h!=null&&(h.equals("soundcloud.com")||h.endsWith(".soundcloud.com")||h.equals("sndcdn.com")||h.endsWith(".sndcdn.com")||h.equals("playback.media-streaming.soundcloud.cloud"))&&(u.getPort()==-1||u.getPort()==443);}catch(Exception e){return false;}}
 private static boolean apiHost(URL u){return "api-v2.soundcloud.com".equals(u.getHost())||"api.soundcloud.com".equals(u.getHost());}
 private HttpURLConnection connect(String url,String method,byte[] body)throws Exception{return connect(url,method,body,0,"");}
 private HttpURLConnection connect(String url,String method,byte[] body,long offset,String validator)throws Exception{
  for(int redirect=0;redirect<6;redirect++){
   if(!trusted(url))throw new IOException("Остановлен переход на посторонний сервер");
   URL u=new URL(url);HttpURLConnection c=(HttpURLConnection)u.openConnection();c.setInstanceFollowRedirects(false);c.setConnectTimeout(20000);c.setReadTimeout(30000);c.setRequestMethod(method);c.setRequestProperty("User-Agent","api.soundcloud.com".equals(u.getHost())?"SoundFlow/1.0.0 Android":"Mozilla/5.0");c.setRequestProperty("Accept","application/json, audio/*;q=0.9, */*;q=0.5");
   if(apiHost(u)){String t=overrideToken==null?Session.get(context):overrideToken;if(!t.isEmpty()){String auth=AuthCapture.authorization(t);if(auth.isEmpty())auth=AuthCapture.authorization("OAuth "+t);if(!auth.isEmpty())c.setRequestProperty("Authorization",auth);}}
   if("api-v2.soundcloud.com".equals(u.getHost())){c.setRequestProperty("Origin","https://soundcloud.com");c.setRequestProperty("Referer","https://soundcloud.com/");}
   if(offset>0){c.setRequestProperty("Range","bytes="+offset+"-");c.setRequestProperty("If-Range",validator);}c.setRequestProperty("Accept-Encoding","identity");
   if(body!=null){c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json");try(OutputStream out=c.getOutputStream()){out.write(body);}}
   int code=c.getResponseCode();
   if(code>=300&&code<400){String location=c.getHeaderField("Location");c.disconnect();if(location==null)throw new IOException("Пустой адрес перенаправления");url=new URL(u,location).toString();if(!method.equals("GET"))throw new IOException("Неожиданный переход при изменении данных");continue;}
   if(code==416&&offset>0)return c;
   if(code<200||code>=300){ApiError error=new ApiError(code);String contentType=c.getContentType();error.responseKind=contentType==null?"UNKNOWN":contentType.toLowerCase(java.util.Locale.ROOT).contains("html")?"HTML":contentType.toLowerCase(java.util.Locale.ROOT).contains("json")?"JSON":"OTHER";String authError=c.getHeaderField("WWW-Authenticate");try{c.setReadTimeout(5000);InputStream failed=c.getErrorStream();if(failed!=null)try(InputStream in=failed;ByteArrayOutputStream bytes=new ByteArrayOutputStream()){byte[] buffer=new byte[1024];int n;while(bytes.size()<8192&&(n=in.read(buffer,0,Math.min(buffer.length,8192-bytes.size())))>0)bytes.write(buffer,0,n);error.reason=ApiRefusal.extract(bytes.toString("UTF-8"),authError);}}catch(Exception ignored){}if(error.reason.isEmpty())error.reason=ApiRefusal.extract("",authError);String wait=c.getHeaderField("Retry-After");try{error.retryAfterMs=Math.min(86400L,Math.max(0,Long.parseLong(wait)))*1000L;}catch(Exception ignored){long date=c.getHeaderFieldDate("Retry-After",0);error.retryAfterMs=Math.min(86400000L,Math.max(0,date-System.currentTimeMillis()));}c.disconnect();throw error;}return c;
  }throw new IOException("Слишком много перенаправлений");
 }
 private byte[] read(String url,int max)throws Exception{HttpURLConnection c=connect(url,"GET",null);try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] buf=new byte[16384];int n,total=0;while((n=in.read(buf))!=-1){total+=n;if(total>max)throw new IOException("Ответ слишком большой");out.write(buf,0,n);}return out.toByteArray();}finally{c.disconnect();}}
 private synchronized String clientId()throws Exception{
  if(overrideClient!=null&&overrideClient.matches("[A-Za-z0-9_-]{16,256}"))return overrideClient;
  if(overrideToken==null){Session.migrateClient(context);String bound=Session.client(context);if(!bound.isEmpty()&&!Session.get(context).isEmpty())return bound;}
  android.content.SharedPreferences p=context.getSharedPreferences("web_config",0);String id=p.getString("id","");long at=p.getLong("at",0);
  if(!id.isEmpty()&&System.currentTimeMillis()-at<3600000)return id;
  String html=new String(read("https://soundcloud.com",4*1024*1024),"UTF-8");
  Matcher m=Pattern.compile("\"hydratable\"\\s*:\\s*\"apiClient\"\\s*,\\s*\"data\"\\s*:\\s*\\{\\s*\"id\"\\s*:\\s*\"([^\"\\s]+)\"").matcher(html);
  if(!m.find())throw new IOException("Изменилась веб-конфигурация SoundCloud. Нужна новая версия клиента.");
  id=m.group(1);p.edit().putString("id",id).putLong("at",System.currentTimeMillis()).apply();return id;
 }
 public String effectiveClient()throws Exception{return clientId();}
 private String apiUrl(String path)throws Exception{
  String url=path.startsWith("https://")?path:BASE+path;
  URL u=new URL(url);if(!apiHost(u)||!trusted(url))throw new IOException("Недопустимый адрес API");
  if("api-v2.soundcloud.com".equals(u.getHost())&&!url.matches(".*[?&]client_id=.*"))url+=(url.contains("?")?"&":"?")+"client_id="+enc(clientId());return url;
 }
 private String get(String path)throws Exception{return new String(read(apiUrl(path),8*1024*1024),"UTF-8");}
 private static String enc(String s)throws Exception{return URLEncoder.encode(s,"UTF-8");}
 public JSONObject me()throws Exception{return new JSONObject(get("/me"));}
 public static Library.Track track(JSONObject j){
  JSONObject user=j.optJSONObject("user");String id=j.optString("id");Library.Track t=new Library.Track("sc:"+id,j.optString("title","Без названия"),user==null?"SoundCloud":user.optString("username","SoundCloud"),j.optLong("duration"),j.optBoolean("user_favorite"));t.remote=Library.compactRemote(j);return t;
 }
 private Page tracks(String path,boolean liked)throws Exception{
  JSONObject root=new JSONObject(get(path));Page p=new Page();p.next=root.isNull("next_href")?"":root.optString("next_href","");JSONArray a=root.optJSONArray("collection");if(a!=null)for(int i=0;i<a.length();i++){JSONObject item=a.optJSONObject(i);if(item==null)continue;JSONObject t=item.optJSONObject("track");if(t==null)t=item;if(!t.has("title")||!t.has("id"))continue;Library.Track tr=track(t);if(liked)tr.liked=true;p.tracks.add(tr);}return p;
 }
 public Page search(String q,String next)throws Exception{return tracks(next.isEmpty()?"/search/tracks?q="+enc(q)+"&limit=50&linked_partitioning=1":next,false);}
 public Page likes(String user,String next)throws Exception{return tracks(next.isEmpty()?"/users/"+enc(user)+"/track_likes?limit=100&linked_partitioning=1":next,true);}
 public Page related(String id,String next)throws Exception{return tracks(next.isEmpty()?"/tracks/"+enc(id)+"/related?limit=50&linked_partitioning=1":next,false);}
 public Page playlists(String user,boolean liked,String next)throws Exception{
  String path=next.isEmpty()?"/users/"+enc(user)+(liked?"/playlist_likes":"/playlists")+"?limit=100&linked_partitioning=1":next;
  JSONObject j=new JSONObject(get(path));Page p=new Page();p.next=j.isNull("next_href")?"":j.optString("next_href","");JSONArray a=j.optJSONArray("collection");if(a!=null)for(int i=0;i<a.length();i++){JSONObject item=a.optJSONObject(i);if(item==null)continue;JSONObject l=item.optJSONObject("playlist");if(l==null)l=item;if(l.has("title")&&l.has("id"))p.playlists.add(playlistSummary(l));}return p;
 }
 static JSONObject playlistSummary(JSONObject source){JSONObject out=new JSONObject();try{for(String k:new String[]{"id","title","track_count","artwork_url","kind","cached","requested","savedAt","_cacheKind"})if(source.has(k)&&!source.isNull(k))out.put(k,source.get(k));JSONObject user=source.optJSONObject("user");if(user!=null)out.put("user",new JSONObject().put("username",user.optString("username")));}catch(JSONException e){throw new IllegalArgumentException(e);}return out;}
 public ArrayList<Library.Track> playlistTracks(String id)throws Exception{
  JSONObject p=new JSONObject(get("/playlists/"+enc(id)+"?representation=full"));JSONArray a=p.optJSONArray("tracks");ArrayList<Library.Track> out=new ArrayList<>();if(a==null){if(p.optInt("track_count")>0)throw new IOException("Полный список плейлиста не получен");return out;}if(p.has("track_count")&&p.optInt("track_count")!=a.length())throw new IOException("Сервис вернул неполный список плейлиста. Загрузка не отмечена полной.");
  ArrayList<String> batch=new ArrayList<>();HashMap<String,Library.Track> map=new HashMap<>();
  for(int i=0;i<a.length();i++){JSONObject t=a.getJSONObject(i);if(t.has("title"))map.put(t.optString("id"),track(t));else batch.add(t.optString("id"));}
  for(int off=0;off<batch.size();off+=50){String ids=android.text.TextUtils.join(",",batch.subList(off,Math.min(off+50,batch.size())));JSONArray loaded=new JSONArray(get("/tracks?ids="+enc(ids)));for(int k=0;k<loaded.length();k++){JSONObject t=loaded.optJSONObject(k);if(t!=null&&t.has("title"))map.put(t.optString("id"),track(t));}}
  for(int i=0;i<a.length();i++){String tid=a.getJSONObject(i).optString("id");Library.Track t=map.get(tid);if(t==null){t=new Library.Track("sc:"+tid,"Недоступный трек","SoundCloud",0,false);t.remote=new JSONObject().put("id",tid).put("streamable",false).put("policy","BLOCK");}out.add(t);}return out;
 }
 public Library.Track freshTrack(Library.Track old)throws Exception{Library.Track t;try{t=track(new JSONObject(get("/tracks/"+enc(old.id())+"?representation=full")));}catch(ApiError e){throw e.at("TRACK");}t.liked=old.liked;return t;}
 public static class WriteUnknown extends IOException {WriteUnknown(){super("Результат изменения неизвестен. Проверьте библиотеку перед повтором, чтобы не создать дубликат.");}}
 private JSONObject write(String method,String path,JSONObject body)throws Exception{if((overrideToken==null?Session.get(context):overrideToken).isEmpty())throw new IOException("Нужен вход в SoundCloud");String url=apiUrl(path);HttpURLConnection c;try{c=connect(url,method,body==null?null:body.toString().getBytes("UTF-8"));}catch(ApiError e){throw e.at("WRITE");}catch(IOException e){throw new WriteUnknown();}try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] buf=new byte[8192];int n;while((n=in.read(buf))!=-1){if(out.size()+n>2*1024*1024)throw new WriteUnknown();out.write(buf,0,n);}return out.size()==0?new JSONObject():new JSONObject(out.toString("UTF-8"));}catch(Exception e){throw new WriteUnknown();}finally{c.disconnect();}}
 private void own(String user)throws Exception{if(!user.matches("[0-9]+")||!user.equals(context.getSharedPreferences("profile",0).getString("id","")))throw new IOException("Аккаунт изменился");JSONObject profile;try{profile=me();}catch(ApiError e){throw e.at("READ_ACCOUNT");}if(!user.equals(profile.optString("id")))throw new IOException("Сессия другого аккаунта. Обновите вход.");}
 private static void numeric(String id)throws IOException{if(!id.matches("[0-9]{1,20}"))throw new IOException("Некорректный идентификатор");}
 public void nativeLike(String owner,String id,boolean liked)throws Exception{numeric(id);own(owner);write(liked?"PUT":"DELETE","/users/"+owner+"/track_likes/"+id,null);}
 public JSONObject nativeCreate(String owner,String title,String track)throws Exception{own(owner);if(title.trim().isEmpty()||title.trim().length()>80)throw new IOException("Название должно содержать от 1 до 80 символов");JSONArray tracks=new JSONArray();if(!track.isEmpty()){numeric(track);tracks.put(Long.parseLong(track));}return write("POST","/playlists",new JSONObject().put("playlist",new JSONObject().put("title",title.trim()).put("sharing","private").put("tracks",tracks)));}
 public void nativeAdd(String owner,String playlist,String track)throws Exception{numeric(playlist);numeric(track);own(owner);JSONObject old=new JSONObject(get("/playlists/"+playlist+"?representation=full")),user=old.optJSONObject("user");if(!owner.equals(old.optString("user_id",user==null?"":user.optString("id"))))throw new IOException("Этот плейлист принадлежит другому аккаунту");JSONArray rows=old.optJSONArray("tracks");if(rows==null||rows.length()!=old.optInt("track_count",-1))throw new IOException("Сервис вернул неполный плейлист. Изменение отменено.");JSONArray ids=new JSONArray();for(int i=0;i<rows.length();i++){String id=rows.getJSONObject(i).optString("id");numeric(id);if(id.equals(track))return;ids.put(Long.parseLong(id));}ids.put(Long.parseLong(track));write("PUT","/playlists/"+playlist,new JSONObject().put("playlist",new JSONObject().put("tracks",ids)));}
 public static class Stream {
  public String note="";public final String url,licenseToken;public final boolean hls,preview;public final long duration;
  Stream(String u,boolean h,String token,boolean p,long d){url=u;hls=h;licenseToken=token;preview=p;duration=d;}
  public boolean drm(){return !licenseToken.isEmpty();}
 }
 public Stream playable(Library.Track track)throws Exception{return playable(track,false);}
 public Stream playable(Library.Track track,boolean offline)throws Exception{
  JSONObject fresh;try{fresh=new JSONObject(get("/tracks/"+enc(track.id())+"?representation=full"));}catch(ApiError e){throw e.at("TRACK");}
  if(!fresh.optBoolean("streamable",true)||"BLOCK".equalsIgnoreCase(fresh.optString("policy")))throw new IOException("SoundCloud не разрешает воспроизведение в текущем регионе или аккаунте");
  JSONObject media=fresh.optJSONObject("media");JSONArray trans=media==null?null:media.optJSONArray("transcodings");if(trans==null)throw new IOException("У трека нет доступного потока");
  ArrayList<JSONObject> candidates=new ArrayList<>();boolean otherDrm=false;
  for(int i=0;i<trans.length();i++){JSONObject t=trans.getJSONObject(i),f=t.optJSONObject("format");if(f==null)continue;String mime=f.optString("mime_type"),proto=f.optString("protocol");if(proto.contains("encrypted"))otherDrm=true;if(StreamPolicy.supported(proto,mime))candidates.add(t);}
  if(candidates.isEmpty())throw new IOException(otherDrm?"SoundCloud отдаёт защищённый поток, но совместимый Widevine-вариант не найден":"SoundCloud не предоставил поддерживаемый аудиопоток");
  final String policy=fresh.optString("policy");final long fullDuration=fresh.optLong("full_duration",fresh.optLong("duration"));
  Collections.sort(candidates,(a,b)->streamScore(b,policy,fullDuration,offline)-streamScore(a,policy,fullDuration,offline));Exception last=null;
  boolean hasFull=false;for(JSONObject candidate:candidates)if(!isPreview(candidate,policy,fullDuration))hasFull=true;
  for(JSONObject chosen:candidates){
   // Never silently fall back to a short preview when a full supported variant was advertised.
   if(hasFull&&isPreview(chosen,policy,fullDuration))continue;
   String raw=chosen.getString("url"),auth=fresh.optString("track_authorization","");if(!auth.isEmpty())raw+=(raw.contains("?")?"&":"?")+"track_authorization="+enc(auth);
   try{JSONObject result=new JSONObject(get(raw));String url=result.optString("url");if(!trusted(url))throw new IOException("SoundCloud вернул неподдерживаемый адрес аудио");String protocol=chosen.getJSONObject("format").optString("protocol"),token="";
    if(StreamPolicy.encrypted(protocol)){if(!DrmBridge.supported())throw new IOException("Полный поток требует Widevine, недоступный на этом устройстве");token=result.optString("licenseAuthToken","");if(!StreamPolicy.tokenValid(token))throw new IOException("SoundCloud не выдал авторизацию Widevine-лицензии");}
    boolean preview=isPreview(chosen,policy,fullDuration);Stream stream=new Stream(url,!"progressive".equals(protocol),token,preview,chosen.optLong("duration",fresh.optLong("duration")));if(preview)stream.note="Превью "+Library.time(stream.duration)+(fullDuration>stream.duration?" из "+Library.time(fullDuration):"")+" · полная версия ограничена условиями SoundCloud, аккаунтом и регионом.";return stream;
   }catch(ApiError e){if(e.status==401||e.status==429||e.status>=500)throw e;last=e;}catch(IOException e){last=e;}
  }
  if(last!=null)throw last;throw new IOException("SoundCloud не предоставил совместимый поток");
 }
 private static boolean isPreview(JSONObject t,String policy,long fullDuration){return StreamPolicy.preview(t.optBoolean("snipped"),policy,t.optLong("duration"),fullDuration);}
 private static int streamScore(JSONObject t,String policy,long fullDuration,boolean offline){JSONObject f=t.optJSONObject("format");return f==null?-99999:StreamPolicy.rank(f.optString("protocol"),f.optString("mime_type"),t.optString("quality"),isPreview(t,policy,fullDuration),offline);}
 /** Original files only if explicitly downloadable. Protected offline uses OfflineStore + a real CDM licence. */
 public File download(Library.Track t)throws Exception{return download(t,null);}
 public File download(Library.Track t,OfflineStore.Progress progress)throws Exception{
  JSONObject fresh=new JSONObject(get("/tracks/"+enc(t.id())));
  if(!fresh.optBoolean("downloadable",false))throw new IOException("Автор не разрешил скачивание оригинала. Поток не будет копироваться.");
  String raw=fresh.isNull("download_url")?"":fresh.optString("download_url","");if(raw.isEmpty())raw=BASE+"/tracks/"+enc(t.id())+"/download";
  final String endpoint=apiUrl(raw);String owner=context.getSharedPreferences("profile",0).getString("id","");
  String identity=t.id()+":"+fresh.optString("last_modified")+":"+fresh.optLong("original_content_size",-1);
  return OriginalTransfer.fetch(new File(context.getFilesDir(),"original_parts"),OriginalTransfer.key(owner,t.id()),identity,(offset,validator)->{
   HttpURLConnection c;try{c=connect(endpoint,"GET",null,offset,validator);}catch(ApiError e){throw e.at("ORIGINAL");}
   String type=c.getContentType();if(c.getResponseCode()!=416&&type!=null&&type.contains("json")){
    String target;try(InputStream in=c.getInputStream();ByteArrayOutputStream bytes=new ByteArrayOutputStream()){byte[] buffer=new byte[4096];int n;while((n=in.read(buffer))!=-1){if(bytes.size()+n>65536)throw new IOException("Некорректный ответ загрузки");bytes.write(buffer,0,n);}JSONObject j=new JSONObject(bytes.toString("UTF-8"));target=j.optString("redirectUri",j.optString("redirect_uri",j.optString("url","")));}finally{c.disconnect();}
    if(!trusted(target))throw new IOException("Нет доступной ссылки на разрешённую загрузку");c=connect(target,"GET",null,offset,validator);
   }return c;
  },new OriginalTransfer.Progress(){public boolean cancelled(){return progress!=null&&progress.cancelled()||!owner.equals(context.getSharedPreferences("profile",0).getString("id",""));}public void update(int p){if(progress!=null)progress.update(p);}});
 }

 public Bitmap artwork(String url)throws Exception{return artwork(url,512);}
 public Bitmap artwork(String url,int edge)throws Exception{if(!trusted(url))return null;edge=Math.max(32,Math.min(768,edge));String sized=url.replace("-large.",edge<=200?"-t200x200.":"-t500x500.");byte[] b=read(sized,2*1024*1024);return OfflineArt.decode(b,edge);}

}
