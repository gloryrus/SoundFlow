package com.farika.soundflow;
import android.app.*;
import android.content.*;
import android.os.*;
import android.net.Uri;
import java.io.*;
import java.util.*;
import org.json.*;
/** Test-only APK; uses only synthetic audio, no user credentials, no DRM keys. */
public class NativeSmoke extends Instrumentation {
 private final Bundle result=new Bundle();private int checks=0;
 private void check(boolean b,String name){if(!b)throw new AssertionError(name);checks++;result.putString(name,"PASS");}
 private void copy(InputStream in,File dest)throws Exception{try(InputStream a=in;OutputStream out=new FileOutputStream(dest)){byte[] b=new byte[8192];int n;while((n=a.read(b))!=-1)out.write(b,0,n);}}
 @Override public void onCreate(Bundle b){super.onCreate(b);start();}
 @Override public void onStart(){Context c=getTargetContext();File tone=new File(c.getFilesDir(),"qa_native_test.wav");String offId=UUID.randomUUID().toString();try{
  check(c.getPackageManager().getPackageInfo(c.getPackageName(),0).versionName.equals("0.4.0-mixes"),"version");
  String sealed=OfflineVault.seal("SYNTHETIC_TEST_METADATA");check(OfflineVault.open(sealed).equals("SYNTHETIC_TEST_METADATA"),"vault_roundtrip");boolean rejected=false;try{OfflineVault.open(sealed.substring(0,sealed.length()-5)+"AAAAA");}catch(Exception e){rejected=true;}check(rejected,"vault_tamper_rejected");
  check(!ScApi.trusted("http://soundcloud.com/test")&&!ScApi.trusted("https://soundcloud.com.evil.test/")&&!ScApi.trusted("https://soundcloud.com@evil.test/"),"network_origin_checks");
  check(new OfflineStore(c).find("offline:../../bad")==null,"offline_invalid_path_rejected");
  boolean hasWidevine=DrmBridge.supported();result.putString("device_widevine",hasWidevine?"SUPPORTED_NOT_LICENSE_TESTED":"NOT_AVAILABLE");
  if(!hasWidevine){boolean failed=false;try{DrmBridge.streaming("SYNTHETIC_TEST_NOT_A_TOKEN");}catch(IOException e){failed=true;}check(failed,"unsupported_drm_fails_closed");}
  if(Session.get(c).isEmpty()){boolean denied=false;try{new OfflineStore(c).save(new Library.Track("sc:2407515615","Synthetic","Test",30000,false),new OfflineStore.Progress(){public void update(int p){}public boolean cancelled(){return false;}});}catch(IOException e){denied=true;}check(denied,"offline_without_session_rejected");}
  copy(getContext().getAssets().open("test.wav"),tone);String uri=Uri.fromFile(tone).toString();Library library=new Library(c);library.upsert(new Library.Track(uri,"Проверка нативного плеера","Тестовый сигнал",30000,false));library.save();
  // A synthetic authorized-original record tests the private storage path without any network.
  File dir=new File(new File(c.getFilesDir(),"licensed_offline"),offId);dir.mkdirs();copy(new FileInputStream(tone),new File(dir,"original"));JSONObject record=new JSONObject().put("ready",true).put("owner",c.getSharedPreferences("profile",0).getString("id","")).put("kind","original").put("title","Офлайн-проверка").put("artist","Тестовый сигнал").put("duration",30000);
  c.getSharedPreferences("offline_records",0).edit().putString(offId,OfflineVault.seal(record.toString())).commit();
  OfflineStore store=new OfflineStore(c);check(store.find("offline:"+offId)!=null,"offline_record_visible");try(OfflineStore.Open opened=store.open("offline:"+offId)){check(!opened.drm&&opened.url.startsWith("file:"),"offline_original_open");}
  Intent activity=new Intent().setComponent(new ComponentName(c.getPackageName(),"com.farika.soundflow.MainActivity")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);Activity a=startActivitySync(activity);waitForIdleSync();
  ArrayList<String> queue=new ArrayList<>();queue.add("offline:"+offId);c.startForegroundService(new Intent(c,PlaybackService.class).setAction("PLAY").putExtra("uri","offline:"+offId).putStringArrayListExtra("queue",queue));
  final boolean[] playing={false};for(int i=0;i<80;i++){Thread.sleep(250);runOnMainSync(()->{PlaybackService s=PlaybackService.instance;playing[0]=s!=null&&s.isPlaying()&&s.position()>300;});if(playing[0])break;}check(playing[0],"native_offline_playback");
  runOnMainSync(()->{PlaybackService.instance.pause();PlaybackService.instance.seek(5000);});Thread.sleep(900);final int[] pos={0};runOnMainSync(()->{pos[0]=PlaybackService.instance.position();});check(pos[0]>=4900&&pos[0]<6000,"native_seek_and_pause");
  runOnMainSync(()->{a.moveTaskToBack(true);PlaybackService.instance.resume();});Thread.sleep(1300);runOnMainSync(()->{pos[0]=PlaybackService.instance.position();});check(pos[0]>6000,"native_background_playback");runOnMainSync(()->PlaybackService.instance.stopForLocalChange());Thread.sleep(500);store.remove("offline:"+offId);check(store.find("offline:"+offId)==null,"offline_delete");
  // Public resolver only. No license request, credential capture or content decryption.
  try{Library.Track remote=new Library.Track("sc:2407515615","Hootie Frutti","sonyan",137446,false);remote.remote=new JSONObject().put("id","2407515615");ScApi.Stream stream=new ScApi(c).playable(remote);result.putString("public_resolver",stream.drm()&&stream.hls&&!stream.preview?"WIDEVINE_FULL_METADATA_ONLY":"OTHER_SERVER_RESPONSE");}catch(Exception e){result.putString("public_resolver","NOT_VERIFIED_"+e.getClass().getSimpleName());}
  tone.delete();Library cleaned=new Library(c);for(Iterator<Library.Track> it=cleaned.tracks.iterator();it.hasNext();)if(it.next().uri.contains("qa_native_test"))it.remove();cleaned.save();result.putInt("checks",checks);sendStatus(1,result);c.startActivity(activity);Thread.sleep(1200);finish(Activity.RESULT_OK,result);
 }catch(Throwable e){result.putString("failure",e.getClass().getSimpleName()+" "+(e instanceof AssertionError?e.getMessage():""));finish(Activity.RESULT_CANCELED,result);}finally{try{runOnMainSync(()->{if(PlaybackService.instance!=null)PlaybackService.instance.stopForLocalChange();});new OfflineStore(c).remove("offline:"+offId);tone.delete();Library l=new Library(c);for(Iterator<Library.Track> it=l.tracks.iterator();it.hasNext();)if(it.next().uri.contains("qa_native_test"))it.remove();l.save();}catch(Exception ignored){}}}
}
