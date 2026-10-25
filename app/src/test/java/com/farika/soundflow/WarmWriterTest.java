package com.farika.soundflow;
import android.app.Activity;
import android.content.Intent;
import android.os.Looper;
import android.webkit.*;
import java.security.*;
import java.security.cert.Certificate;
import java.util.*;
import java.io.*;
import java.time.Duration;
import javax.crypto.spec.SecretKeySpec;
import org.json.JSONObject;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

/** Real AccountWriter lifecycle with synthetic WebView/JS replies; no live account or measured network latency. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=28,manifest=Config.NONE,application=android.app.Application.class)
public class WarmWriterTest {
 static final String TOKEN="OAuth SYNTHETIC_WARM_TOKEN_12345",CLIENT="SYNTHETIC_WARM_CLIENT_12345";
 Activity activity;AccountWriter writer;final List<FakeWeb> browsers=new ArrayList<>();final List<Intent> done=new ArrayList<>();final List<String> failures=new ArrayList<>();boolean deny;
 public static class MemoryStore extends KeyStoreSpi {
  public Key engineGetKey(String a,char[] p){return new SecretKeySpec(new byte[32],"AES");}
  public Certificate[] engineGetCertificateChain(String a){return null;}public Certificate engineGetCertificate(String a){return null;}public Date engineGetCreationDate(String a){return new Date(0);}
  public void engineSetKeyEntry(String a,Key k,char[] p,Certificate[] c){}public void engineSetKeyEntry(String a,byte[] k,Certificate[] c){}public void engineSetCertificateEntry(String a,Certificate c){}public void engineDeleteEntry(String a){}
  public Enumeration<String> engineAliases(){return Collections.enumeration(Collections.singletonList("volna_session"));}public boolean engineContainsAlias(String a){return true;}public int engineSize(){return 1;}public boolean engineIsKeyEntry(String a){return true;}public boolean engineIsCertificateEntry(String a){return false;}public String engineGetCertificateAlias(Certificate c){return null;}public void engineStore(OutputStream o,char[] p){}public void engineLoad(InputStream i,char[] p){}
 }
 class FakeWeb extends WebView {
  int loads,scripts;String url="";boolean destroyed;String lastScript="";
  FakeWeb(){super(activity);}
  @Override public void loadUrl(String value){loads++;url=value;getWebViewClient().onPageStarted(this,value,null);getWebViewClient().onPageFinished(this,value);}
  @Override public String getUrl(){return url;}
  @Override public void evaluateJavascript(String js,ValueCallback<String> callback){
   if(js.startsWith("(window.__sc_hydration")){if(callback!=null)callback.onReceiveValue(JSONObject.quote(CLIENT));}
   else if(js.startsWith("(function()")){scripts++;lastScript=js;}
   else if(js.startsWith("JSON.stringify")){if(callback!=null)callback.onReceiveValue(JSONObject.quote(deny?"{\"ok\":false,\"status\":403,\"reason\":\"HTTP\"}":"{\"ok\":true,\"status\":204,\"playlist_id\":\"77\"}"));}
  }
  @Override public void destroy(){destroyed=true;super.destroy();}
 }
 @Before public void setup()throws Exception{
  Provider provider=new Provider("VolnaSyntheticKeyStore",1.0,"Test-only in-memory key store"){};provider.put("KeyStore.AndroidKeyStore",MemoryStore.class.getName());Security.insertProviderAt(provider,1);
  activity=Robolectric.buildActivity(Activity.class).setup().get();activity.getSharedPreferences("profile",0).edit().putString("id","123").commit();Session.put(activity,TOKEN,CLIENT);
  writer=new AccountWriter(activity,new AccountWriter.Callback(){public void complete(Intent result){done.add(result);}public void failed(int status,String reason,boolean verification){failures.add(reason);}}){@Override protected WebView createWebView(){FakeWeb w=new FakeWeb();browsers.add(w);return w;}};
 }
 @After public void cleanup(){writer.close();activity.finish();Security.removeProvider("VolnaSyntheticKeyStore");}
 private void tick(){Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(90));}
 @Test public void warmDoesNotWriteOrReportSuccess(){writer.warm();assertEquals(1,browsers.size());assertEquals(1,browsers.get(0).loads);assertEquals(0,browsers.get(0).scripts);assertFalse(writer.busy());assertTrue(done.isEmpty());assertTrue(failures.isEmpty());}
 @Test public void secondOperationReusesOnePageAndUsesNewArguments(){writer.warm();assertTrue(writer.submit("LIKE","456","",""));tick();assertEquals(1,done.size());assertEquals("LIKE",done.get(0).getStringExtra("op"));assertTrue(writer.submit("UNLIKE","789","",""));tick();assertEquals(1,browsers.size());assertEquals(1,browsers.get(0).loads);assertEquals(2,browsers.get(0).scripts);assertEquals("UNLIKE",done.get(1).getStringExtra("op"));assertEquals("789",done.get(1).getStringExtra("track"));assertTrue(browsers.get(0).lastScript.contains("const me=await call('/me')"));}
 @Test public void duplicateTapDoesNotDoubleWrite(){assertTrue(writer.submit("LIKE","456","",""));assertFalse(writer.submit("LIKE","456","",""));tick();assertEquals(1,browsers.get(0).scripts);assertEquals(1,done.size());}
 @Test public void failureIsNotReplayedAndNextExplicitOperationStartsFresh(){writer.warm();deny=true;writer.submit("LIKE","456","","");tick();assertEquals(Arrays.asList("HTTP"),failures);assertTrue(done.isEmpty());assertEquals(1,browsers.get(0).scripts);assertTrue(browsers.get(0).destroyed);deny=false;writer.submit("UNLIKE","456","","");tick();assertEquals(2,browsers.size());assertEquals(1,done.size());}
 @Test public void closePreventsFurtherWrites(){writer.warm();writer.close();assertTrue(browsers.get(0).destroyed);assertFalse(writer.submit("LIKE","456","",""));writer.warm();assertEquals(1,browsers.size());assertTrue(done.isEmpty());}
 @Test public void ownerChangeDropsOldBrowser(){writer.warm();activity.getSharedPreferences("profile",0).edit().putString("id","999").commit();writer.warm();assertTrue(browsers.get(0).destroyed);assertEquals(2,browsers.size());assertEquals(0,browsers.get(1).scripts);}
 @Test public void signedOutWarmDoesNothing(){Session.clear(activity);writer.warm();assertTrue(browsers.isEmpty());assertTrue(done.isEmpty());}
 @Test public void idleWarmPageIsDestroyedAfterFifteenSeconds(){writer.warm();Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(16));assertTrue(browsers.get(0).destroyed);assertFalse(writer.busy());assertEquals(0,browsers.get(0).scripts);}
 @Test public void nextOperationReopensBrowserAfterIdleRelease(){writer.submit("LIKE","456","","");tick();Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(16));assertTrue(browsers.get(0).destroyed);writer.submit("UNLIKE","456","","");tick();assertEquals(2,browsers.size());assertEquals(2,done.size());}

 @Test public void idleRendererLossIsHandledWithoutCrashingHost(){writer.warm();FakeWeb browser=browsers.get(0);assertTrue(browser.getWebViewClient().onRenderProcessGone(browser,null));assertTrue(browser.destroyed);assertTrue(failures.isEmpty());}

 @Test public void createAndAddReuseWarmBrowser(){writer.warm();writer.submit("CREATE","456","","My list");tick();writer.submit("ADD","789","77","");tick();assertEquals(1,browsers.size());assertEquals(1,browsers.get(0).loads);assertEquals(2,done.size());assertEquals("CREATE",done.get(0).getStringExtra("op"));assertEquals("ADD",done.get(1).getStringExtra("op"));assertTrue(browsers.get(0).lastScript.contains("'77'")||browsers.get(0).lastScript.contains("\"77\""));}
 @Test public void memoryPressureReleasesIdleButNotBusyWriter(){writer.warm();FakeWeb w=browsers.get(0);writer.releaseIdle();assertTrue(w.destroyed);writer.submit("LIKE","456","","");FakeWeb busy=browsers.get(1);writer.releaseIdle();assertFalse(busy.destroyed);tick();assertEquals(1,done.size());writer.releaseIdle();assertTrue(busy.destroyed);}

 @Test public void detachedNotificationWriterUsesSameConfirmedTransport(){List<Intent> results=new ArrayList<>();AccountWriter headless=new AccountWriter(activity.getApplicationContext(),new AccountWriter.Callback(){public void complete(Intent result){results.add(result);}public void failed(int status,String reason,boolean v){fail(reason);}}){protected WebView createWebView(){FakeWeb w=new FakeWeb();browsers.add(w);return w;}};try{assertTrue(headless.submit("LIKE","456","",""));tick();assertEquals(1,results.size());assertNull(browsers.get(0).getParent());assertEquals(1,browsers.get(0).scripts);}finally{headless.close();}}
 @Test public void foregroundAndNotificationWritesAreSerialized(){AccountWriter other=new AccountWriter(activity.getApplicationContext(),new AccountWriter.Callback(){public void complete(Intent result){}public void failed(int s,String r,boolean v){}}){protected WebView createWebView(){FakeWeb w=new FakeWeb();browsers.add(w);return w;}};try{assertTrue(writer.submit("LIKE","456","",""));assertFalse(other.submit("UNLIKE","456","",""));tick();assertTrue(other.submit("UNLIKE","456","",""));tick();assertEquals(2,browsers.size());}finally{other.close();}}

}
