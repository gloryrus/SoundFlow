package com.farika.soundflow;

import android.content.Context;
import android.net.Uri;
import androidx.media3.common.C;
import androidx.media3.datasource.*;
import androidx.media3.datasource.cache.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import org.json.JSONObject;
import static org.junit.Assert.*;

/** Synthetic transport/cache tests, not codec playback or a live SoundCloud entitlement test. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=28,manifest=Config.NONE,application=android.app.Application.class)
public class ClearStreamCacheTest {
 static final String BASE="https://cf-media.sndcdn.com/",ID="11111111-1111-1111-1111-111111111111";
 Context context;SimpleCache cache;File folder;Map<String,byte[]> data;int opens;
 final OfflineStore.Progress progress=new OfflineStore.Progress(){public boolean cancelled(){return false;}public void update(int p){}};
 @Before public void setup()throws Exception{
  context=RuntimeEnvironment.getApplication();data=new HashMap<>();opens=0;
  context.getSharedPreferences("profile",0).edit().putString("id","123").commit();
  folder=new File(context.getFilesDir(),"licensed_offline/"+ID);erase(folder);assertTrue(folder.mkdirs());
  cache=new SimpleCache(new File(folder,"cache"),new NoOpCacheEvictor());cache.checkInitialization();
  Provider provider=new Provider("VolnaSyntheticKeyStore",1.0,"test only"){};
  provider.put("KeyStore.AndroidKeyStore",WarmWriterTest.MemoryStore.class.getName());Security.insertProviderAt(provider,1);
 }
 @After public void cleanup(){if(cache!=null)cache.release();Security.removeProvider("VolnaSyntheticKeyStore");context.getSharedPreferences("offline_records",0).edit().clear().commit();erase(folder);}
 static void erase(File f){File[] children=f.listFiles();if(children!=null)for(File child:children)erase(child);f.delete();}
 DataSource.Factory network(){return ()->new DataSource(){ByteArrayDataSource current;
  public void addTransferListener(TransferListener listener){}
  public long open(DataSpec spec)throws IOException{opens++;byte[] bytes=data.get(spec.uri.toString());if(bytes==null)throw new FileNotFoundException("fixture missing");current=new ByteArrayDataSource(bytes);return current.open(spec);}
  public int read(byte[] b,int o,int n)throws IOException{return current.read(b,o,n);}
  public Uri getUri(){return current==null?null:current.getUri();}
  public void close(){if(current!=null)current.close();}
 };}
 void put(String path,String text){data.put(BASE+path,text.getBytes(StandardCharsets.UTF_8));}
 String playlist(boolean end){return "#EXTM3U\n#EXT-X-VERSION:3\n#EXT-X-TARGETDURATION:6\n#EXT-X-MEDIA-SEQUENCE:0\n#EXTINF:6.0,\na.ts\n#EXTINF:6.0,\nb.ts\n"+(end?"#EXT-X-ENDLIST\n":"");}
 ScApi.Stream stream(String path,boolean hls,boolean preview,long duration){return new ScApi.Stream(BASE+path,hls,"",preview,duration);}
 ClearStreamCache.Result save(ScApi.Stream stream)throws Exception{return ClearStreamCache.save(stream,network(),cache,folder,progress,()->{});}
 void fixtures(){put("list.m3u8",playlist(true));data.put(BASE+"a.ts",new byte[4096]);data.put(BASE+"b.ts",new byte[2048]);}
 void expectFailure(String message,ScApi.Stream stream)throws Exception{try{save(stream);fail("expected failure");}catch(IOException e){assertTrue(e.getMessage(),e.getMessage().contains(message));}}
 @Test(timeout=30000) public void clearHlsCachesWithoutOriginalOrLicence()throws Exception{fixtures();ClearStreamCache.Result r=save(stream("list.m3u8",true,false,12000));assertTrue(r.hls);assertEquals(12000,r.duration);assertTrue(cache.isCached(BASE+"a.ts",0,4096));assertTrue(cache.isCached(BASE+"b.ts",0,2048));}
 @Test(timeout=30000) public void progressiveCachesAllBytes()throws Exception{byte[] bytes=new byte[150000];new Random(4).nextBytes(bytes);data.put(BASE+"audio.mp3",bytes);ClearStreamCache.Result r=save(stream("audio.mp3",false,false,12000));assertFalse(r.hls);DataSource source=OfflineStore.cacheOnly(cache).createDataSource();int previous=opens;ByteArrayOutputStream out=new ByteArrayOutputStream();try{source.open(new DataSpec(Uri.parse(r.url)));byte[] b=new byte[2048];int n;while((n=source.read(b,0,b.length))!=C.RESULT_END_OF_INPUT)out.write(b,0,n);}finally{source.close();}assertArrayEquals(bytes,out.toByteArray());assertEquals(previous,opens);}
 @Test(timeout=30000) public void reopenedRecordNeedsNoWidevineOrNetwork()throws Exception{fixtures();ClearStreamCache.Result r=save(stream("list.m3u8",true,false,12000));cache.release();cache=null;JSONObject row=new JSONObject().put("owner","123").put("ready",true).put("kind","stream").put("url",r.url).put("hls",true);context.getSharedPreferences("offline_records",0).edit().putString(ID,OfflineVault.seal(row.toString())).commit();int previous=opens;try(OfflineStore.Open open=new OfflineStore(context).open("offline:"+ID)){assertFalse(open.drm);assertTrue(open.hls);DataSource source=open.data.createDataSource();try{assertEquals(4096,source.open(new DataSpec(Uri.parse(BASE+"a.ts"))));}finally{source.close();}source=open.data.createDataSource();try{source.open(new DataSpec(Uri.parse(BASE+"missing.ts")));fail("missing cache must fail");}catch(IOException expected){}finally{source.close();}}assertEquals(previous,opens);}
 @Test(timeout=30000) public void masterResolvesOneAudioPlaylist()throws Exception{fixtures();put("master.m3u8","#EXTM3U\n#EXT-X-STREAM-INF:BANDWIDTH=128000\nlist.m3u8\n");assertEquals(BASE+"list.m3u8",save(stream("master.m3u8",true,false,12000)).url);}
 @Test public void previewNeverRequestsNetwork()throws Exception{expectFailure("превью",stream("list.m3u8",true,true,30000));assertEquals(0,opens);}
 @Test public void drmNeverUsesClearDownloader()throws Exception{try{save(new ScApi.Stream(BASE+"protected.m3u8",true,"SYNTHETIC_TOKEN",false,12000));fail();}catch(IOException e){assertEquals("CLEAR_STREAM_ENCRYPTED",e.getMessage());}assertEquals(0,opens);}
 @Test public void livePlaylistRejected()throws Exception{put("list.m3u8",playlist(false));expectFailure("HLS",stream("list.m3u8",true,false,12000));}
 @Test public void shortPlaylistRejected()throws Exception{fixtures();expectFailure("Неполный",stream("list.m3u8",true,false,180000));}
 @Test public void aesPlaylistDoesNotFetchKeys()throws Exception{put("list.m3u8",playlist(true).replace("#EXTINF:6.0,", "#EXT-X-KEY:METHOD=AES-128,URI=\"key.bin\"\n#EXTINF:6.0,"));expectFailure("CLEAR_STREAM_ENCRYPTED",stream("list.m3u8",true,false,12000));assertEquals(1,opens);}
 @Test public void foreignSegmentRejected()throws Exception{put("list.m3u8",playlist(true).replace("a.ts","https://example.org/a.ts"));expectFailure("адрес HLS",stream("list.m3u8",true,false,12000));assertEquals(1,opens);}
 @Test(timeout=30000) public void missingSegmentDoesNotSucceed()throws Exception{fixtures();data.remove(BASE+"b.ts");try{save(stream("list.m3u8",true,false,12000));fail();}catch(IOException expected){}}
 @Test public void cancelledJobDoesNotOpenNetwork()throws Exception{try{ClearStreamCache.save(stream("audio.mp3",false,false,12000),network(),cache,folder,progress,()->{throw new InterruptedIOException("cancelled");});fail();}catch(InterruptedIOException expected){}assertEquals(0,opens);}
}
