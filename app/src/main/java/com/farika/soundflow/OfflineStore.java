package com.farika.soundflow;

import android.content.*;
import android.net.Uri;
import android.util.Base64;
import android.util.Pair;
import androidx.media3.common.*;
import androidx.media3.datasource.*;
import androidx.media3.datasource.cache.*;
import androidx.media3.exoplayer.drm.*;
import androidx.media3.exoplayer.hls.offline.HlsDownloader;
import androidx.media3.exoplayer.hls.playlist.*;
import androidx.media3.extractor.mp4.PsshAtomUtil;
import java.io.*;
import java.net.URI;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.json.*;

/** Fail-closed offline store. Protected audio remains encrypted; Android's CDM retains its keys.
 * Clear playback streams are cached privately, independently of original-file downloads. */
public class OfflineStore {
 public interface Progress {void update(int percent);boolean cancelled();}
 private static final AtomicInteger EPOCH=new AtomicInteger();
 private final Context context;private final SharedPreferences prefs;private final File root;
 public OfflineStore(Context c){context=c.getApplicationContext();prefs=context.getSharedPreferences("offline_records",0);root=new File(context.getFilesDir(),"licensed_offline");}
 public static boolean isOffline(String uri){return uri!=null&&uri.startsWith("offline:");}
 private String id(String uri)throws IOException{String v=uri==null?"":uri.replaceFirst("^offline:","");if(!v.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"))throw new IOException("Некорректная офлайн-запись");return v;}
 private String owner(){return context.getSharedPreferences("profile",0).getString("id","");}
 private JSONObject record(String uri)throws Exception{String raw=prefs.getString(id(uri),"");if(raw.isEmpty())throw new IOException("Офлайн-запись не найдена");JSONObject r=new JSONObject(OfflineVault.open(raw));if(!r.optBoolean("ready")||!r.optString("owner").equals(owner()))throw new IOException("Эта офлайн-запись недоступна для текущего аккаунта");return r;}
 private static Library.Track track(String id,JSONObject r){Library.Track t=new Library.Track("offline:"+id,r.optString("title"),r.optString("artist"),r.optLong("duration"),r.optBoolean("liked"));t.remote=r.optJSONObject("remote");return t;}
 private ArrayList<Library.Track> records(){ArrayList<Library.Track> out=new ArrayList<>();for(String id:prefs.getAll().keySet())try{JSONObject r=record("offline:"+id);if(new File(root,id).isDirectory())out.add(track(id,r));}catch(Exception ignored){}return out;}
 public ArrayList<Library.Track> list(){java.util.LinkedHashMap<String,Library.Track> unique=new java.util.LinkedHashMap<>();for(Library.Track t:records()){String key=t.id().isEmpty()?t.uri:t.id();Library.Track old=unique.get(key);if(old==null||t.duration>old.duration)unique.put(key,t);}return new ArrayList<>(unique.values());}
 private String resolve(String uri){SharedPreferences aliases=context.getSharedPreferences("offline_replacements",0);java.util.HashSet<String> seen=new java.util.HashSet<>();for(int n=0;n<32&&seen.add(uri);n++){String next=aliases.getString(owner()+":"+uri,"");if(next.isEmpty())break;uri=next;}return uri;}
 private boolean redirect(String old,String replacement){SharedPreferences aliases=context.getSharedPreferences("offline_replacements",0);SharedPreferences.Editor editor=aliases.edit();for(java.util.Map.Entry<String,?> entry:aliases.getAll().entrySet())if(entry.getKey().startsWith(owner()+":")&&old.equals(entry.getValue()))editor.putString(entry.getKey(),replacement);return editor.putString(owner()+":"+old,replacement).commit();}
 public Library.Track bySource(String id){if(id==null||id.isEmpty())return null;Library.Track best=null;for(Library.Track t:list())if(id.equals(t.id())&&(best==null||t.duration>best.duration))best=t;return best;}
 public Library.Track find(String uri){try{uri=resolve(uri);return track(id(uri),record(uri));}catch(Exception e){return null;}}
 public synchronized void updateLiked(String sourceId,boolean liked){for(String id:prefs.getAll().keySet())try{JSONObject r=record("offline:"+id),remote=r.optJSONObject("remote");if(remote!=null&&sourceId.equals(remote.optString("id"))){r.put("liked",liked);prefs.edit().putString(id,OfflineVault.seal(r.toString())).commit();}}catch(Exception ignored){}}
 private void check(String owner,int epoch,Progress p)throws IOException{if(p.cancelled()||Thread.currentThread().isInterrupted()||EPOCH.get()!=epoch||!owner.equals(owner()))throw new InterruptedIOException("Загрузка отменена или аккаунт изменился");}
 private static JSONObject metadata(Library.Track t,String owner)throws JSONException{JSONObject r=new JSONObject().put("owner",owner).put("title",t.title).put("artist",t.artist).put("duration",t.duration).put("liked",t.liked);JSONObject m=new JSONObject();if(t.remote!=null)for(String k:new String[]{"id","permalink_url","artwork_url","downloadable","streamable","policy","full_duration","monetization_model","genre"})if(t.remote.has(k)&&!t.remote.isNull(k))m.put(k,t.remote.get(k));if(!t.artwork().isEmpty())m.put("artwork_url",t.artwork());return r.put("remote",m);}
 protected ScApi api(){return new ScApi(context);}
 protected DataSource.Factory networkSource(){return network(context);}
 public void observedDuration(String uri,long measured,long expected)throws Exception{if(measured<=0||!OfflineQuality.shortCopy(measured,expected))return;synchronized(OfflineStore.class){JSONObject r=record(uri);r.put("duration",measured);JSONObject remote=r.optJSONObject("remote");if(remote!=null)remote.put("full_duration",Math.max(expected,remote.optLong("full_duration")));if(!prefs.edit().putString(id(uri),OfflineVault.seal(r.toString())).commit())throw new IOException("Не удалось обновить длительность");}repairKnownCopies();}
 public void repairKnownCopies(){String user=owner();if(user.isEmpty())return;Library library=new Library(context);boolean changed=false;try(SyncDb db=new SyncDb(context)){for(Library.Track copy:list()){if(excluded(copy.id())||!OfflineQuality.needsRepair(copy,copy)||copy.id().isEmpty())continue;Library.Track source=new Library.Track("sc:"+copy.id(),copy.title,copy.artist,OfflineQuality.expected(copy),copy.liked);source.remote=copy.remote;library.upsert(source);changed=true;String plan="track:"+copy.id();db.ensure(user,plan,copy.title,"track");if(db.tracks(user,plan).isEmpty())db.snapshot(user,plan,java.util.Collections.singletonList(source));db.active(user,plan,true);db.status(user,plan,"Короткая офлайн-копия: ожидаем полный трек SoundCloud. Старая копия сохранена до замены.");}if(changed)library.save();}catch(Exception ignored){}}
 public boolean excluded(String id){return !id.isEmpty()&&context.getSharedPreferences("offline_exclusions",0).getBoolean(owner()+":"+id,false);}
 public void exclude(String id){synchronized(OfflineStore.class){if(!id.isEmpty())context.getSharedPreferences("offline_exclusions",0).edit().putBoolean(owner()+":"+id,true).commit();}}
 public void removeSource(String uri,String sourceId)throws Exception{synchronized(OfflineStore.class){exclude(sourceId);if(sourceId.isEmpty()){remove(uri);return;}for(Library.Track copy:records())if(sourceId.equals(copy.id()))remove(copy.uri);}}
 public void allow(String id){context.getSharedPreferences("offline_exclusions",0).edit().remove(owner()+":"+id).commit();}
 protected long measuredFile(File f)throws Exception{return AudioDuration.file(f);}
 protected long measuredCache(DataSource.Factory data,String url)throws Exception{return AudioDuration.cache(data,url);}
 public Library.Track save(Library.Track t,Progress progress)throws Exception{
  final String owner=owner();final int epoch=EPOCH.get();if(excluded(t.id()))throw new InterruptedIOException("Локальная копия исключена из автозагрузки");if(owner.isEmpty()||Session.get(context).isEmpty())throw new IOException("Для загрузки войдите в аккаунт SoundCloud");check(owner,epoch,progress);
  String id=UUID.randomUUID().toString();File folder=new File(root,id);if(!folder.mkdirs())throw new IOException("Не удалось создать защищённое хранилище");boolean committed=false;SimpleCache cache=null;OfflineLicenseHelper helper=null;File original=null;
  try{
   ScApi api=api();t=api.freshTrack(t);boolean originalAllowed=t.remote!=null&&t.remote.optBoolean("downloadable");ScApi.Stream stream=originalAllowed?null:api.playable(t,true);File fetchedOriginal=null;if(originalAllowed){try{fetchedOriginal=api.download(t,progress);original=fetchedOriginal;}catch(ScApi.ApiError denied){if(denied.status!=403&&denied.status!=404&&denied.status!=410)throw denied;originalAllowed=false;stream=api.playable(t,true);}}check(owner,epoch,progress);JSONObject r=metadata(t,owner);
   if(stream!=null&&stream.preview)throw new IOException("SoundCloud выдал только превью. Оно не сохраняется как полный трек.");
   if(stream!=null&&OfflineQuality.shortCopy(stream.duration,OfflineQuality.expected(t)))throw new IOException("SoundCloud выдал только превью. Оно будет повторно проверено позже.");
   if(originalAllowed){
    original=fetchedOriginal;check(owner,epoch,progress);File dest=new File(folder,"original");if(!original.renameTo(dest))throw new IOException("Не удалось переместить разрешённый оригинал");original=null;long measured=measuredFile(dest);if(measured<=0)throw new IOException("Не удалось проверить длительность аудиофайла");if(OfflineQuality.shortCopy(measured,Math.max(OfflineQuality.expected(t),stream==null?0:stream.duration)))throw new IOException("SoundCloud выдал только превью");r.put("kind","original").put("duration",measured);
   }else if(!stream.drm()){
    cache=new SimpleCache(new File(folder,"cache"),new NoOpCacheEvictor());cache.checkInitialization();
    final String account=owner;final int jobEpoch=epoch;
    ClearStreamCache.Result saved=ClearStreamCache.save(stream,networkSource(),cache,folder,new Progress(){
     public boolean cancelled(){return progress.cancelled();}
     public void update(int percent){progress.update(percent);}
    },()->check(account,jobEpoch,progress));
    long measured=saved.hls?saved.duration:measuredCache(cacheOnly(cache),saved.url);if(measured<=0)throw new IOException("Не удалось проверить длительность аудиофайла");if(OfflineQuality.shortCopy(measured,Math.max(OfflineQuality.expected(t),stream==null?0:stream.duration)))throw new IOException("SoundCloud выдал только превью");r.put("kind","stream").put("url",saved.url).put("hls",saved.hls).put("duration",measured).put("savedAt",System.currentTimeMillis());
   }else{
    if(!DrmBridge.supported())throw new IOException("На этом устройстве нет Widevine. Офлайн-лицензия не получена.");
    progress.update(0);DataSource.Factory net=networkSource();String[] offlineUrl={stream.url};Format format=licenceFormat(stream,net,offlineUrl);
    helper=DrmBridge.offlineHelper(stream.licenseToken);byte[] keySet;
    try{keySet=helper.downloadLicense(format);validateDuration(helper,keySet);}catch(Exception denied){throw new IOException("Офлайн-лицензия не получена. "+DrmBridge.publicError(denied),denied);}
    check(owner,epoch,progress);File cacheDir=new File(folder,"cache");cache=new SimpleCache(cacheDir,new NoOpCacheEvictor());cache.checkInitialization();
    CacheDataSource.Factory writer=new CacheDataSource.Factory().setCache(cache).setUpstreamDataSourceFactory(net).setFlags(CacheDataSource.FLAG_BLOCK_ON_CACHE);
    MediaItem item=DrmBridge.item(offlineUrl[0],true,false);HlsDownloader downloader=new HlsDownloader(item,writer,Runnable::run);
    downloader.download((length,bytes,percent)->{try{check(owner,epoch,progress);if(folder.getUsableSpace()<24*1024*1024)throw new IOException("Недостаточно памяти");progress.update(percent<0?-1:Math.min(99,(int)percent));}catch(IOException e){downloader.cancel();throw new DownloadCancelled(e);}});
    check(owner,epoch,progress);
    // A second, cache-only traversal must succeed. Missing data never falls back to the network.
    CacheDataSource.Factory reader=cacheOnly(cache);new HlsDownloader(item,reader,Runnable::run).download((length,bytes,percent)->{});
    validateDuration(helper,keySet);check(owner,epoch,progress);
    r.put("kind","drm").put("url",offlineUrl[0]).put("keySetId",Base64.encodeToString(keySet,Base64.NO_WRAP)).put("duration",stream.duration).put("savedAt",System.currentTimeMillis());
   }
   check(owner,epoch,progress);OfflineArt.save(context,api,t.artwork());check(owner,epoch,progress);r.put("ready",true);String sealed=OfflineVault.seal(r.toString());check(owner,epoch,progress);
   synchronized(OfflineStore.class){check(owner,epoch,progress);if(excluded(t.id()))throw new InterruptedIOException("Локальная копия исключена из автозагрузки");if(!prefs.edit().putString(id,sealed).commit())throw new IOException("Не удалось сохранить офлайн-запись");committed=true;}
   for(Library.Track old:records())if(!old.uri.equals("offline:"+id)&&old.id().equals(t.id()))try{if(redirect(old.uri,"offline:"+id))remove(old.uri);}catch(Exception ignored){}
   return track(id,r);
  }catch(DownloadCancelled e){throw new IOException("Загрузка отменена или недостаточно памяти. Трек не сохранён.");}
  finally{if(helper!=null)helper.release();if(cache!=null)cache.release();if(original!=null)original.delete();if(!committed)deleteTree(folder);}
 }
 private static final class DownloadCancelled extends RuntimeException{DownloadCancelled(Throwable e){super("DOWNLOAD_CANCELLED",e);}}
 private static void validateDuration(OfflineLicenseHelper h,byte[] keySet)throws Exception{if(keySet==null||keySet.length==0)throw new IOException("OFFLINE_LICENSE_MISSING");Pair<Long,Long> p=h.getLicenseDurationRemainingSec(keySet);if(p==null||p.first<=60||p.second<=60)throw new IOException("OFFLINE_LICENSE_EXPIRED_OR_UNCONFIRMED");}
 static DataSource.Factory network(Context c){return new DefaultDataSource.Factory(c,new DefaultHttpDataSource.Factory().setUserAgent("SoundFlow/0.3 Android").setConnectTimeoutMs(20000).setReadTimeoutMs(30000).setAllowCrossProtocolRedirects(false));}
 static CacheDataSource.Factory cacheOnly(SimpleCache cache){return new CacheDataSource.Factory().setCache(cache).setUpstreamDataSourceFactory(null).setCacheWriteDataSinkFactory(null).setFlags(CacheDataSource.FLAG_BLOCK_ON_CACHE);}
 private static byte[] read(DataSource.Factory factory,String url,long position,long length)throws Exception{
  if(!ScApi.trusted(url))throw new IOException("Неподдерживаемый адрес HLS");DataSource ds=factory.createDataSource();try{ds.open(new DataSpec.Builder().setUri(url).setPosition(position).setLength(length).build());try(ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[8192];int n;while((n=ds.read(b,0,b.length))!=-1){if(out.size()+n>2*1024*1024)throw new IOException("HLS metadata too large");out.write(b,0,n);}return out.toByteArray();}}finally{ds.close();}
 }
 private static Format licenceFormat(ScApi.Stream stream,DataSource.Factory net,String[] resolved)throws Exception{
  String manifestUrl=stream.url;HlsPlaylist p=null;HashSet<String> seen=new HashSet<>();for(int depth=0;depth<4;depth++){if(!seen.add(manifestUrl))throw new IOException("Цикл HLS");byte[] manifest=read(net,manifestUrl,0,C.LENGTH_UNSET);p=new HlsPlaylistParser().parse(Uri.parse(manifestUrl),new ByteArrayInputStream(manifest));if(p instanceof HlsMediaPlaylist)break;androidx.media3.exoplayer.hls.playlist.HlsMultivariantPlaylist master=(androidx.media3.exoplayer.hls.playlist.HlsMultivariantPlaylist)p;String child="";for(androidx.media3.exoplayer.hls.playlist.HlsMultivariantPlaylist.Rendition audio:master.audios)if(audio.url!=null){child=audio.url.toString();break;}if(child.isEmpty()&&!master.variants.isEmpty())child=master.variants.get(0).url.toString();if(child.isEmpty())throw new IOException("Пустой HLS master");manifestUrl=new URI(manifestUrl).resolve(child).toString();}
  if(!(p instanceof HlsMediaPlaylist))throw new IOException("Для офлайна нужен отдельный аудиопоток, не общий HLS-плейлист");HlsMediaPlaylist media=(HlsMediaPlaylist)p;resolved[0]=manifestUrl;
  if(!media.hasEndTag||media.segments.isEmpty())throw new IOException("Нельзя сохранить неполный или живой HLS-поток");
  for(HlsMediaPlaylist.Segment segment:media.segments){DrmInitData d=segment.drmInitData;if(d!=null)for(int i=0;i<d.schemeDataCount;i++){DrmInitData.SchemeData s=d.get(i);if(s.matches(C.WIDEVINE_UUID)&&s.hasData())return new Format.Builder().setContainerMimeType("audio/mp4").setDrmInitData(d).build();}}
  HlsMediaPlaylist.Segment init=media.segments.get(0).initializationSegment;if(init==null)throw new IOException("В потоке нет инициализации Widevine");String uri=new URI(manifestUrl).resolve(init.url).toString();byte[] data=read(net,uri,init.byteRangeOffset,init.byteRangeLength);
  // Parse only public MP4 PSSH initialization metadata. No content keys or decryption.
  for(int i=0;i+32<=data.length;i++){if(data[i+4]!='p'||data[i+5]!='s'||data[i+6]!='s'||data[i+7]!='h')continue;long size=((long)(data[i]&255)<<24)|((data[i+1]&255)<<16)|((data[i+2]&255)<<8)|(data[i+3]&255);if(size<32||size>data.length-i)continue;byte[] atom=Arrays.copyOfRange(data,i,i+(int)size);if(C.WIDEVINE_UUID.equals(PsshAtomUtil.parseUuid(atom))){DrmInitData d=new DrmInitData("cenc",new DrmInitData.SchemeData(C.WIDEVINE_UUID,"video/mp4",atom));return new Format.Builder().setContainerMimeType("audio/mp4").setDrmInitData(d).build();}}
  throw new IOException("Поток не содержит доступных данных инициализации Widevine");
 }
 public static final class Open implements AutoCloseable {
  public final String url;public final boolean hls,drm;public final DataSource.Factory data;public final byte[] keySetId;private final SimpleCache cache;
  Open(String u,boolean h,DataSource.Factory d,byte[] k,SimpleCache c){url=u;hls=h;drm=k!=null;data=d;keySetId=k;cache=c;}
  public void close(){if(cache!=null)cache.release();}
 }
 public Open open(String uri)throws Exception{uri=resolve(uri);JSONObject r=record(uri);File dir=new File(root,id(uri));if("original".equals(r.optString("kind"))){File f=new File(dir,"original");if(!f.isFile()||f.length()==0)throw new IOException("Локальный файл отсутствует");return new Open(Uri.fromFile(f).toString(),false,new DefaultDataSource.Factory(context),null,null);}
  if("stream".equals(r.optString("kind"))){
   SimpleCache cache=new SimpleCache(new File(dir,"cache"),new NoOpCacheEvictor());
   try{cache.checkInitialization();return new Open(r.getString("url"),r.getBoolean("hls"),cacheOnly(cache),null,cache);}catch(Exception e){cache.release();throw e;}
  }
  if(!DrmBridge.supported())throw new IOException("На устройстве нет Widevine для защищённой офлайн-записи");byte[] key=Base64.decode(r.getString("keySetId"),Base64.NO_WRAP);if(key.length==0)throw new IOException("Офлайн-лицензия отсутствует");SimpleCache cache=new SimpleCache(new File(dir,"cache"),new NoOpCacheEvictor());try{cache.checkInitialization();return new Open(r.getString("url"),true,cacheOnly(cache),key,cache);}catch(Exception e){cache.release();throw e;}}
 public void remove(String uri)throws Exception{synchronized(OfflineStore.class){String id=id(uri);File folder=new File(root,id);if(SimpleCache.isCacheFolderLocked(new File(folder,"cache")))throw new IOException("Остановите воспроизведение перед удалением");if(!prefs.edit().remove(id).commit())throw new IOException("Не удалось удалить запись");deleteTree(folder);}}
 public void clearAll(){synchronized(OfflineStore.class){EPOCH.incrementAndGet();OriginalTransfer.clear(new File(context.getFilesDir(),"original_parts"));prefs.edit().clear().commit();context.getSharedPreferences("offline_replacements",0).edit().clear().commit();context.getSharedPreferences("offline_exclusions",0).edit().clear().commit();File[] files=root.listFiles();if(files!=null)for(File f:files)if(!SimpleCache.isCacheFolderLocked(new File(f,"cache")))deleteTree(f);}}
 public void pruneIncomplete(){synchronized(OfflineStore.class){if(root.exists()){File[] files=root.listFiles();if(files!=null)for(File f:files)if(!prefs.contains(f.getName())&&!SimpleCache.isCacheFolderLocked(new File(f,"cache")))deleteTree(f);}}}
 private static void deleteTree(File f){if(f.isDirectory()){File[] a=f.listFiles();if(a!=null)for(File c:a)deleteTree(c);}f.delete();}
}
