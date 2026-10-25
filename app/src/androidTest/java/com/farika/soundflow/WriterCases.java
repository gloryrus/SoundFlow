package com.farika.soundflow;
import android.app.*;import android.content.*;import android.os.*;import android.webkit.*;import org.json.*;import java.io.*;import java.util.*;
/** Actual Android WebView, JS fetch, CORS and evaluateJavascript result polling. Every HTTPS request is intercepted. */
final class WriterCases {
 static final String TOKEN="OAuth SYNTHETIC_BROWSER_TOKEN_12345",OLD="SYNTHETIC_STALE_CLIENT_12345",FRESH="SYNTHETIC_CURRENT_CLIENT_98765";
 static volatile int writeCode=204,me=123,writes,requests;static volatile String method="",path="",query="",authorization="",cookie="";static boolean armed,issueChallenge,hydrationOnly;
 static Bundle out=new Bundle();static int checks;
 static void check(boolean value,String label){if(!value)throw new AssertionError(label);checks++;out.putString(label,"PASS");}
 static Object field(Object o,Class<?> type,String name)throws Exception{java.lang.reflect.Field f=type.getDeclaredField(name);f.setAccessible(true);return f.get(o);}
 public static class FixtureActivity extends AccountWebActivity {
  @Override protected WebViewClient websiteClient(){WebViewClient real=super.websiteClient();return new WebViewClient(){
   public boolean shouldOverrideUrlLoading(WebView w,WebResourceRequest r){return real.shouldOverrideUrlLoading(w,r);}
   public void onPageFinished(WebView w,String u){real.onPageFinished(w,u);}
   public WebResourceResponse shouldInterceptRequest(WebView w,WebResourceRequest r){real.shouldInterceptRequest(w,r);requests++;
    String host=r.getUrl().getHost(),p=r.getUrl().getPath();Map<String,String> headers=new HashMap<>();headers.put("Access-Control-Allow-Origin","https://soundcloud.com");headers.put("Access-Control-Allow-Credentials","true");headers.put("Access-Control-Allow-Methods","GET, POST, PUT, DELETE, OPTIONS");headers.put("Access-Control-Allow-Headers","Authorization, Content-Type");headers.put("Cache-Control","no-store");
    int code=200;String body="",mime="application/json";
    if("soundcloud.com".equals(host)){mime="text/html";body="<!doctype html><meta charset='utf-8'><title>Synthetic writer fixture</title><p>First-party test page, no network</p><script>fetch('https://api-v2.soundcloud.com/me?client_id="+FRESH+"',{credentials:'include',headers:{Authorization:'"+TOKEN+"'}}).catch(()=>{});</script>";}
    else if("api-v2.soundcloud.com".equals(host)){
     if("OPTIONS".equals(r.getMethod())){code=204;}
     else if(!"GET".equals(r.getMethod())){writes++;method=r.getMethod();path=p;query=r.getUrl().getQuery();authorization=AuthCapture.header(r.getRequestHeaders());cookie=r.getRequestHeaders().get("Cookie");code=writeCode;body=issueChallenge?"{\"url\":\"https://geo.captcha-delivery.com/captcha/?fixture=1\"}":code==201?"{\"id\":77}":code>=400?"{\"error\":\"access_denied\"}":"";}
     else if("/me".equals(p))body="{\"id\":"+me+"}";
     else if("/playlists/77".equals(p))body="{\"user_id\":123,\"track_count\":2,\"tracks\":[{\"id\":111},{\"id\":222}]}";
     else {code=404;body="{}";}
    }else if("geo.captcha-delivery.com".equals(host)){mime="text/html";body="<!doctype html><script>parent.postMessage({cookie:'datadome=SYNTHETIC_PROVIDER_COOKIE; Path=/; Secure'},'https://soundcloud.com');</script>";}else{code=403;body="{}";}
    if("soundcloud.com".equals(host)&&hydrationOnly)body="<!doctype html><meta charset='utf-8'><title>Hydration-only fixture</title><script>window.__sc_hydration=[{hydratable:'apiClient',data:{id:'"+FRESH+"'}}];</script>";
    return new WebResourceResponse(mime,"UTF-8",code,code==204?"No Content":code<300?"OK":"Denied",headers,new ByteArrayInputStream(body.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
   }
  };}
 }
 static void run(Instrumentation i){Context c=i.getTargetContext();Activity[] current={null};try{
  for(String op:new String[]{"LIKE","UNLIKE","CREATE","ADD","DENIED","OTHER_OWNER","HYDRATION","CHALLENGE"}){
   writes=0;requests=0;hydrationOnly=op.equals("HYDRATION");issueChallenge=op.equals("CHALLENGE");me=op.equals("OTHER_OWNER")?999:123;writeCode=op.equals("DENIED")||issueChallenge?403:op.equals("CREATE")?201:204;method="";query="";authorization="";
   c.getSharedPreferences("profile",0).edit().putString("id","123").commit();Session.put(c,TOKEN,OLD);
   String actual=op.equals("DENIED")||op.equals("OTHER_OWNER")||hydrationOnly||issueChallenge?"LIKE":op;armed=true;
   current[0]=i.startActivitySync(new Intent(c,AccountWebActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).putExtra("op",actual).putExtra("track","456").putExtra("playlist",op.equals("ADD")?"77":"").putExtra("title","Synthetic private list").putExtra("auto",true));
   long deadline=System.currentTimeMillis()+60000;boolean done=false;
   while(System.currentTimeMillis()<deadline){final boolean[] state={false};i.runOnMainSync(()->{try{String last=(String)field(current[0],AccountWebActivity.class,"last");state[0]=current[0].isFinishing()||(!last.equals("NOT_STARTED")&&!last.equals("PENDING")&&!last.equals("CHALLENGE_PENDING"));}catch(Exception e){throw new RuntimeException(e);}});if(state[0]){done=true;break;}Thread.sleep(200);}
   if(!done){out.putString("diagnostic", "requests="+requests+" last="+field(current[0],AccountWebActivity.class,"last")+" pageReady="+field(current[0],AccountWebActivity.class,"pageReady")+" class="+current[0].getClass().getName());i.runOnMainSync(()->{try{WebView w=(WebView)field(current[0],AccountWebActivity.class,"web");out.putString("page_url",w==null?"null":w.getUrl());if(w!=null)w.evaluateJavascript("JSON.stringify({ready:document.readyState,title:document.title,keys:Object.keys(window).filter(x=>x.startsWith('_volna_'))})",v->out.putString("page_state",v));}catch(Exception ignored){}});Thread.sleep(1500);}check(done,op.toLowerCase()+"_browser_completed_or_denied");
   int result=(Integer)field(current[0],Activity.class,"mResultCode");
   if(issueChallenge){check(writes==1&&result!=Activity.RESULT_OK,"provider_challenge_never_marks_mutation_success");check("CHALLENGE_PASSED".equals(field(current[0],AccountWebActivity.class,"last")),"browser_accepts_only_provider_iframe_completion");check(CookieManager.getInstance().getCookie("https://soundcloud.com/").contains("datadome=SYNTHETIC_PROVIDER_COOKIE"),"provider_cookie_applied_in_same_browser_session");Thread.sleep(700);check(writes==1,"passed_challenge_does_not_automatically_replay_write");}
   else if(op.equals("OTHER_OWNER")){check(writes==0&&result!=Activity.RESULT_OK,"browser_account_mismatch_no_write");}
   else if(op.equals("DENIED")){check(writes==1&&result!=Activity.RESULT_OK,"browser_403_no_success_or_retry");check(TOKEN.equals(Session.get(c)),"browser_refusal_keeps_session");}
   else{
    check(writes==1&&result==Activity.RESULT_OK,"browser_"+op.toLowerCase()+"_one_confirmed_write");
    check(query.contains("client_id="+FRESH)&&!query.contains(OLD),"browser_"+op.toLowerCase()+"_current_page_client");
    check(TOKEN.equals(authorization),"browser_"+op.toLowerCase()+"_authorized_fetch");
    check(Session.client(c).equals(FRESH),"browser_"+op.toLowerCase()+"_persists_matched_client");
    check(method.equals(op.equals("LIKE")||hydrationOnly?"PUT":op.equals("UNLIKE")?"DELETE":op.equals("CREATE")?"POST":"PUT"),"browser_"+op.toLowerCase()+"_method");
   }
   i.runOnMainSync(()->current[0].finish());i.waitForIdleSync();current[0]=null;
  }
  out.putString("scope","Real WebView engine and first-party synthetic CORS responses only; all requests intercepted; no real user account changes or WAF acceptance claimed.");
 }catch(Throwable e){out.putString("failure",e.getClass().getSimpleName()+" "+(e instanceof AssertionError?e.getMessage():"see test trace"));e.printStackTrace();}finally{armed=false;if(current[0]!=null)i.runOnMainSync(()->current[0].finish());Session.clear(c);c.getSharedPreferences("profile",0).edit().clear().commit();}
 out.putInt("checks",checks);i.finish(out.containsKey("failure")?Activity.RESULT_CANCELED:Activity.RESULT_OK,out);
 }
}
