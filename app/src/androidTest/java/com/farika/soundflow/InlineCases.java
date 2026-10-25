package com.farika.soundflow;
import android.app.*;import android.content.*;import android.webkit.*;import android.view.*;import android.os.*;import org.json.*;import java.util.*;import java.io.*;
/** Actual MainActivity button handler + background WebView. All its requests get synthetic responses. */
final class InlineCases {
 static boolean armed,hydrate;static volatile int writes,me=123,code=204;static volatile String method="",query="",auth="";static int checks;static Bundle out=new Bundle();
 static final String TOKEN="OAuth SYNTHETIC_INLINE_TOKEN_12345",FRESH="SYNTHETIC_INLINE_CLIENT_12345";
 static void check(boolean b,String name){if(!b)throw new AssertionError(name);checks++;out.putString(name,"PASS");}
 static Object field(Object o,Class<?> type,String n)throws Exception{java.lang.reflect.Field f=type.getDeclaredField(n);f.setAccessible(true);return f.get(o);}
 static void call(Object o,String method,Class<?>[] types,Object...args){try{java.lang.reflect.Method m=MainActivity.class.getDeclaredMethod(method,types);m.setAccessible(true);m.invoke(o,args);}catch(Exception e){throw new RuntimeException(e);}}
 public static class FixtureActivity extends MainActivity {
  @Override protected AccountWriter createAccountWriter(AccountWriter.Callback callback){return new AccountWriter(this,callback){@Override protected WebViewClient websiteClient(){WebViewClient real=super.websiteClient();return new WebViewClient(){
   public boolean shouldOverrideUrlLoading(WebView w,WebResourceRequest r){return real.shouldOverrideUrlLoading(w,r);}
   public void onPageStarted(WebView w,String u,android.graphics.Bitmap b){real.onPageStarted(w,u,b);}
   public void onPageFinished(WebView w,String u){real.onPageFinished(w,u);}
   public WebResourceResponse shouldInterceptRequest(WebView w,WebResourceRequest r){real.shouldInterceptRequest(w,r);Map<String,String> headers=new HashMap<>();headers.put("Access-Control-Allow-Origin","https://soundcloud.com");headers.put("Access-Control-Allow-Credentials","true");headers.put("Access-Control-Allow-Methods","GET, POST, PUT, DELETE, OPTIONS");headers.put("Access-Control-Allow-Headers","Authorization, Content-Type");headers.put("Cache-Control","no-store");String host=r.getUrl().getHost(),path=r.getUrl().getPath(),body="{}",mime="application/json";int status=200;
    if("soundcloud.com".equals(host)){mime="text/html";body="<!doctype html><meta charset='utf-8'><p>Test transport document</p><script>"+(hydrate?"window.__sc_hydration=[{hydratable:'apiClient',data:{id:'"+FRESH+"'}}];":"fetch('https://api-v2.soundcloud.com/me?client_id="+FRESH+"',{headers:{Authorization:'"+TOKEN+"'},credentials:'include'}).catch(()=>{});")+"</script>";}
    else if("api-v2.soundcloud.com".equals(host)){if("OPTIONS".equals(r.getMethod()))status=204;else if(!"GET".equals(r.getMethod())){writes++;method=r.getMethod();query=r.getUrl().getQuery();auth=AuthCapture.header(r.getRequestHeaders());status=code;body=code==201?"{\"id\":77}":code==403?"{\"url\":\"https://geo.captcha-delivery.com/captcha/?synthetic=1\"}":"";}
     else if("/me".equals(path))body="{\"id\":"+me+"}";else if("/playlists/77".equals(path))body="{\"user_id\":123,\"track_count\":2,\"tracks\":[{\"id\":111},{\"id\":222}]}";else status=404;
    }else status=403;
    return new WebResourceResponse(mime,"UTF-8",status,status<300?"OK":"Denied",headers,new ByteArrayInputStream(body.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
   }
  };}};}
 }
 static void run(Instrumentation i){Context c=i.getTargetContext();Activity[] a={null};Instrumentation.ActivityMonitor monitor=i.addMonitor(AccountWebActivity.class.getName(),null,false);try{
  for(String op:new String[]{"LIKE","UNLIKE","CREATE","ADD","HYDRATION","DENIED","OTHER_OWNER"}){
   hydrate=op.equals("HYDRATION");writes=0;code=op.equals("DENIED")?403:op.equals("CREATE")?201:204;me=op.equals("OTHER_OWNER")?999:123;method="";query="";auth="";
   c.getSharedPreferences("profile",0).edit().putString("id","123").commit();Session.put(c,TOKEN,"SYNTHETIC_STALE_CLIENT_12345");
   armed=true;a[0]=i.startActivitySync(new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
   Library.Track t=new Library.Track("sc:456","Inline test track","Synthetic",120000,op.equals("UNLIKE"));t.remote=new JSONObject().put("id","456");
   i.runOnMainSync(()->{try{Library l=(Library)field(a[0],MainActivity.class,"lib");l.upsert(t);l.save();((ArrayList<Library.Track>)field(a[0],MainActivity.class,"visible")).add(t);
    if(op.equals("LIKE")||op.equals("UNLIKE")||op.equals("HYDRATION")){call(a[0],"toggleLike",new Class<?>[]{Library.Track.class},t);call(a[0],"toggleLike",new Class<?>[]{Library.Track.class},t);}else call(a[0],"accountOperation",new Class<?>[]{String.class,String.class,String.class,String.class},op.equals("DENIED")||op.equals("OTHER_OWNER")?"LIKE":op,"456",op.equals("ADD")?"77":"","Synthetic list");
   }catch(Exception e){throw new RuntimeException(e);}});
   long end=System.currentTimeMillis()+90000;boolean done=false;while(System.currentTimeMillis()<end){boolean[] busy={true};i.runOnMainSync(()->{try{AccountWriter w=(AccountWriter)field(a[0],MainActivity.class,"accountWriter");busy[0]=w!=null&&w.busy();}catch(Exception e){throw new RuntimeException(e);}});if(!busy[0]){done=true;break;}Thread.sleep(200);}
   i.runOnMainSync(()->{try{AccountWriter writer=(AccountWriter)field(a[0],MainActivity.class,"accountWriter");WebView web=(WebView)field(writer,AccountWriter.class,"web");if(web!=null){check(!web.getSettings().getUserAgentString().contains("Android"),op.toLowerCase()+"_desktop_site_mode");check(web.getWidth()>1&&web.getHeight()>1,op.toLowerCase()+"_real_viewport");}}catch(Exception e){throw new RuntimeException(e);}});
   check(done,op.toLowerCase()+"_completed_in_place");check(!a[0].isFinishing(),op.toLowerCase()+"_main_not_closed");check(monitor.getHits()==0,op.toLowerCase()+"_no_browser_activity");
   if(op.equals("OTHER_OWNER")){check(writes==0,"wrong_owner_no_write");check(!new Library(c).find("sc:456").liked,"wrong_owner_no_false_like");}
   else if(op.equals("DENIED")){check(writes==1,"denial_not_retried");check(!new Library(c).find("sc:456").liked,"denial_no_false_like");check(Session.get(c).equals(TOKEN),"denial_preserves_auth");}
   else{check(writes==1,op.toLowerCase()+"_one_write_even_double_tap");check(query.contains(FRESH),op.toLowerCase()+"_fresh_client");check(auth.equals(TOKEN),op.toLowerCase()+"_authorized");check(method.equals(op.equals("UNLIKE")?"DELETE":op.equals("CREATE")?"POST":"PUT"),op.toLowerCase()+"_method");if(op.equals("LIKE")||op.equals("UNLIKE")||op.equals("HYDRATION"))check(new Library(c).find("sc:456").liked!=op.equals("UNLIKE"),op.toLowerCase()+"_heart_committed_after_response");}
   i.runOnMainSync(()->a[0].finish());i.waitForIdleSync();a[0]=null;
  }
  out.putString("scope","Real MainActivity handlers and background Android WebView; only synthetic intercepted HTTPS replies, no live account or WAF acceptance claimed");
 }catch(Throwable e){out.putString("failure",e.getClass().getSimpleName()+" "+e.getMessage());e.printStackTrace();}finally{armed=false;i.removeMonitor(monitor);if(a[0]!=null)i.runOnMainSync(()->a[0].finish());Session.clear(c);c.getSharedPreferences("profile",0).edit().clear().commit();}out.putInt("checks",checks);i.finish(out.containsKey("failure")?Activity.RESULT_CANCELED:Activity.RESULT_OK,out);}
}
