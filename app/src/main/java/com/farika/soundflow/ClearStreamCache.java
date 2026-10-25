package com.farika.soundflow;

import android.net.Uri;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.datasource.*;
import androidx.media3.datasource.cache.*;
import androidx.media3.exoplayer.hls.offline.HlsDownloader;
import androidx.media3.exoplayer.hls.playlist.*;
import androidx.media3.exoplayer.offline.Downloader;
import androidx.media3.exoplayer.offline.ProgressiveDownloader;
import java.io.*;
import java.net.URI;
import java.util.HashSet;

/** Private cache of full clear streams actually advertised to this session.
 * No rewritten URLs, external backend, licence requests, key extraction or preview promotion.
 * Selected URLs use OfflineVault records; cache manifests/index stay in app-private storage.
 * Playback is cache-only.
 */
final class ClearStreamCache {
 interface Check {void run()throws IOException;}
 static final class Result {
  final String url;final boolean hls;final long duration;
  Result(String u,boolean h,long d){url=u;hls=h;duration=d;}
 }
 private ClearStreamCache(){}
 static Result save(ScApi.Stream stream,DataSource.Factory network,SimpleCache cache,File folder,OfflineStore.Progress progress,Check check)throws Exception{
  if(stream.preview)throw new IOException("SoundCloud выдал только превью");
  if(stream.drm())throw new IOException("CLEAR_STREAM_ENCRYPTED");
  check.run();String url=stream.url;long duration=stream.duration;
  if(!ScApi.trusted(url))throw new IOException("Неподдерживаемый адрес аудиопотока");
  if(stream.hls){
   HlsMediaPlaylist media=null;HashSet<String> seen=new HashSet<>();
   for(int depth=0;depth<4;depth++){
    check.run();if(!seen.add(url))throw new IOException("Цикл HLS");
    HlsPlaylist parsed=new HlsPlaylistParser().parse(Uri.parse(url),new ByteArrayInputStream(readManifest(network,url)));
    if(parsed instanceof HlsMediaPlaylist){media=(HlsMediaPlaylist)parsed;break;}
    HlsMultivariantPlaylist master=(HlsMultivariantPlaylist)parsed;
    if(!master.sessionKeyDrmInitData.isEmpty())throw new IOException("CLEAR_STREAM_ENCRYPTED");
    String child="";for(HlsMultivariantPlaylist.Rendition audio:master.audios)if(audio.url!=null){child=audio.url.toString();break;}
    if(child.isEmpty()&&!master.variants.isEmpty())child=master.variants.get(0).url.toString();
    if(child.isEmpty())throw new IOException("Пустой HLS master");url=trustedChild(url,child);
   }
   if(media==null||!media.hasEndTag||media.segments.isEmpty())throw new IOException("Неполный или живой HLS-поток");
   if(media.protectionSchemes!=null)throw new IOException("CLEAR_STREAM_ENCRYPTED");
   long actualDuration=media.durationUs/1000;
   if(actualDuration<=0||StreamPolicy.preview(false,"ALLOW",actualDuration,duration))throw new IOException("Неполный HLS-аудиопоток");
   for(HlsMediaPlaylist.Segment segment:media.segments){
    validateSegment(url,segment);
    if(segment.initializationSegment!=null)validateSegment(url,segment.initializationSegment);
   }
   duration=actualDuration;
  }
  CacheDataSource.Factory writer=new CacheDataSource.Factory().setCache(cache).setUpstreamDataSourceFactory(network).setFlags(CacheDataSource.FLAG_BLOCK_ON_CACHE);
  MediaItem item=new MediaItem.Builder().setUri(url).build();
  Downloader downloader=downloader(item,stream.hls,writer);
  try{
   downloader.download((length,bytes,percent)->{
    try{check.run();if(folder.getUsableSpace()<24*1024*1024L)throw new IOException("Недостаточно памяти");progress.update(percent<0?-1:Math.min(99,(int)percent));}
    catch(IOException error){downloader.cancel();throw new Cancelled(error);}
   });
  }catch(Cancelled error){throw (IOException)error.getCause();}
  check.run();if(cache.getCacheSpace()<=0)throw new EOFException("Неполный аудиофайл");
  // Verify every referenced resource without any network fallback before marking ready.
  downloader(item,stream.hls,OfflineStore.cacheOnly(cache)).download((length,bytes,percent)->{
   try{check.run();}catch(IOException error){throw new Cancelled(error);}
  });
  check.run();return new Result(url,stream.hls,duration);
 }
 private static Downloader downloader(MediaItem item,boolean hls,CacheDataSource.Factory data){
  return hls?new HlsDownloader(item,data,Runnable::run):new ProgressiveDownloader(item,data,Runnable::run);
 }
 private static void validateSegment(String base,HlsMediaPlaylist.Segment segment)throws IOException{
  if(segment.drmInitData!=null||segment.fullSegmentEncryptionKeyUri!=null)throw new IOException("CLEAR_STREAM_ENCRYPTED");
  trustedChild(base,segment.url);
 }
 private static String trustedChild(String base,String child)throws IOException{
  try{String url=new URI(base).resolve(child).toString();if(!ScApi.trusted(url))throw new IOException("Неподдерживаемый адрес HLS");return url;}
  catch(java.net.URISyntaxException error){throw new IOException("Некорректный адрес HLS",error);}
 }
 private static byte[] readManifest(DataSource.Factory factory,String url)throws IOException{
  if(!ScApi.trusted(url))throw new IOException("Неподдерживаемый адрес HLS");
  DataSource source=factory.createDataSource();
  try{source.open(new DataSpec(Uri.parse(url)));ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[8192];int n;
   while((n=source.read(buffer,0,buffer.length))!=C.RESULT_END_OF_INPUT){if(out.size()+n>2*1024*1024)throw new IOException("HLS metadata too large");out.write(buffer,0,n);}return out.toByteArray();
  }finally{source.close();}
 }
 private static final class Cancelled extends RuntimeException {Cancelled(IOException cause){super(cause);}}
}
