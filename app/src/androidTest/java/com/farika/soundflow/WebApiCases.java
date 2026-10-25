package com.farika.soundflow;
import android.app.*;import android.content.*;import android.os.*;import java.net.*;import java.io.*;import java.util.*;import org.json.*;
/** Synthetic first-party API contracts. All HTTPS is intercepted in this test process only. */
final class WebApiCases {
 static final String TOKEN="OAuth SYNTHETIC_WEB_SESSION_12345",CLIENT="SYNTHETIC_BOUND_WEB_CLIENT_12345";
 static final ArrayList<Fake> calls=new ArrayList<>();static JSONObject track;static int resolutionStatus=200,writeStatus=204;static boolean incomplete,redirectWrite,timeoutWrite;static int n;static Bundle result=new Bundle();
 static void check(boolean value,String label){if(!value)throw new AssertionError(label);n++;result.putString(label,"PASS");}
 static class Fake extends HttpURLConnection {
  ByteArrayOutputStream sent=new ByteArrayOutputStream();byte[] response=new byte[0];int status=200;
  Fake(URL u){super(u);calls.add(this);}public void connect(){}public void disconnect(){}public boolean usingProxy(){return false;}public OutputStream getOutputStream(){return sent;}
  public int getResponseCode()throws IOException{
   if(!url.getHost().equals("api-v2.soundcloud.com"))throw new IOException("EXTERNAL_NETWORK_BLOCKED");String path=url.getPath();
   if(!method.equals("GET")){if(timeoutWrite)throw new SocketTimeoutException("SYNTHETIC");status=redirectWrite?302:writeStatus;if(path.equals("/playlists")&&status==201)response="{\"id\":77}".getBytes("UTF-8");return status;}
   if(path.equals("/me"))response="{\"id\":123}".getBytes("UTF-8");
   else if(path.equals("/playlists/77"))response=("{\"user_id\":123,\"track_count\":"+(incomplete?3:2)+",\"tracks\":[{\"id\":111},{\"id\":222}]}").getBytes("UTF-8");
   else if(path.equals("/tracks/456"))response=track.toString().getBytes("UTF-8");
   else if(path.startsWith("/media/")){if(path.endsWith("bad"))status=resolutionStatus;response=(path.endsWith("invalid")?"{\"url\":\"https://evil.test/audio\"}":"{\"url\":\"https://cf-media.sndcdn.com/full.m3u8\"}").getBytes("UTF-8");}
   else throw new IOException("UNEXPECTED_TEST_ROUTE");return status;
  }
  public String getHeaderField(String key){return key.equalsIgnoreCase("Location")?"https://evil.test/redirect":null;}
  public InputStream getInputStream(){return new ByteArrayInputStream(response);}public InputStream getErrorStream(){return new ByteArrayInputStream("{\"error\":\"forbidden\"}".getBytes());}
 }
 static JSONObject variant(String name,String proto,boolean snipped,long duration)throws Exception{return new JSONObject().put("url","https://api-v2.soundcloud.com/media/"+name).put("duration",duration).put("snipped",snipped).put("quality","sq").put("format",new JSONObject().put("protocol",proto).put("mime_type",proto.equals("progressive")?"audio/mpeg":"audio/mp4"));}
 static void media(JSONObject... variants)throws Exception{JSONArray a=new JSONArray();for(JSONObject v:variants)a.put(v);track=new JSONObject().put("id",456).put("title","Synthetic full track").put("duration",180000).put("full_duration",180000).put("policy","ALLOW").put("streamable",true).put("track_authorization","SYNTHETIC_TRACK_AUTH_12345").put("media",new JSONObject().put("transcodings",a));calls.clear();}
 static Fake last(){return calls.get(calls.size()-1);}static int writes(){int count=0;for(Fake c:calls)if(!c.getRequestMethod().equals("GET"))count++;return count;}
 static boolean requested(String path){for(Fake c:calls)if(c.getURL().getPath().equals(path))return true;return false;}
 static void run(Instrumentation i){Context c=i.getTargetContext();try{
  URL.setURLStreamHandlerFactory(p->p.equals("https")?new URLStreamHandler(){protected URLConnection openConnection(URL u){return new Fake(u);}}:null);
  c.getSharedPreferences("profile",0).edit().putString("id","123").commit();Session.put(c,TOKEN,CLIENT);c.getSharedPreferences("web_config",0).edit().putString("id","DIFFERENT_PUBLIC_CONFIG_98765").putLong("at",System.currentTimeMillis()).commit();ScApi api=new ScApi(c,TOKEN,CLIENT);
  api.nativeLike("123","456",true);check(last().getRequestMethod().equals("PUT")&&last().getURL().getPath().equals("/users/123/track_likes/456"),"web_like_put_user_track_likes");check(last().getURL().getHost().equals("api-v2.soundcloud.com")&&last().getURL().getQuery().equals("client_id="+CLIENT),"write_same_host_and_bound_client");check(TOKEN.equals(last().getRequestProperty("Authorization"))&&"https://soundcloud.com".equals(last().getRequestProperty("Origin"))&&last().getRequestProperty("Cookie")==null,"session_auth_no_other_browser_account_cookie");
  api.nativeLike("123","456",false);check(last().getRequestMethod().equals("DELETE"),"unlike_delete_204_empty_body_success");
  writeStatus=201;api.nativeCreate("123","  QA list  ","456");JSONObject body=new JSONObject(last().sent.toString("UTF-8")).getJSONObject("playlist");check(body.getString("title").equals("QA list")&&body.getString("sharing").equals("private")&&body.getJSONArray("tracks").get(0) instanceof Number&&body.getJSONArray("tracks").getLong(0)==456,"private_create_numeric_track_ids");
  api.nativeCreate("123","Empty QA","");check(new JSONObject(last().sent.toString("UTF-8")).getJSONObject("playlist").getJSONArray("tracks").length()==0,"empty_private_playlist_supported");
  writeStatus=204;api.nativeAdd("123","77","456");JSONArray ids=new JSONObject(last().sent.toString("UTF-8")).getJSONObject("playlist").getJSONArray("tracks");check(last().getRequestMethod().equals("PUT")&&last().getURL().getPath().equals("/playlists/77")&&ids.toString().equals("[111,222,456]"),"playlist_numeric_ids_preserve_existing_order");int before=writes();api.nativeAdd("123","77","111");check(writes()==before,"duplicate_add_no_write");
  incomplete=true;boolean blocked=false;try{api.nativeAdd("123","77","456");}catch(IOException e){blocked=true;}check(blocked&&writes()==before,"incomplete_playlist_no_destructive_overwrite");incomplete=false;
  blocked=false;try{api.nativeLike("999","456",true);}catch(IOException e){blocked=true;}check(blocked&&writes()==before,"other_owner_refused_before_write");
  for(int code:new int[]{401,403,429}){writeStatus=code;before=writes();blocked=false;try{api.nativeLike("123","456",true);}catch(ScApi.ApiError e){blocked=e.status==code&&e.stage.equals("WRITE");}check(blocked&&writes()==before+1&&TOKEN.equals(Session.get(c)),"http_"+code+"_one_write_preserves_session");}writeStatus=204;
  timeoutWrite=true;before=writes();blocked=false;try{api.nativeCreate("123","Unknown","");}catch(ScApi.WriteUnknown e){blocked=true;}check(blocked&&writes()==before+1,"ambiguous_create_never_retried");timeoutWrite=false;
  redirectWrite=true;before=writes();blocked=false;try{api.nativeLike("123","456",true);}catch(ScApi.WriteUnknown e){blocked=true;}check(blocked&&writes()==before+1,"mutation_redirect_refused_no_credential_leak");redirectWrite=false;
  Library.Track stale=new Library.Track("sc:456","Stale","QA",30000,false);stale.remote=new JSONObject().put("id",456).put("policy","SNIP");
  media(variant("preview","progressive",true,30000),variant("full","hls",false,180000));ScApi.Stream stream=api.playable(stale);check(!stream.preview&&requested("/media/full")&&!requested("/media/preview"),"fresh_full_hls_beats_cached_or_progressive_preview");check(last().getURL().getQuery().contains("track_authorization=SYNTHETIC_TRACK_AUTH_12345")&&last().getURL().getQuery().contains("client_id="+CLIENT)&&TOKEN.equals(last().getRequestProperty("Authorization")),"track_resolution_uses_fresh_track_auth_and_session");check("https://soundcloud.com/".equals(last().getRequestProperty("Referer")),"read_and_write_origin_consistent");
  media(variant("drm","ctr-encrypted-hls",false,180000),variant("clear","hls",false,180000));stream=api.playable(stale);check(!stream.drm()&&requested("/media/clear")&&!requested("/media/drm"),"full_clear_before_widevine_for_playback");
  resolutionStatus=403;media(variant("bad","progressive",false,180000),variant("full","hls",false,180000));stream=api.playable(stale);check(!stream.preview&&requested("/media/bad")&&requested("/media/full"),"unavailable_full_variant_tries_another_advertised_full");
  media(variant("invalid","progressive",false,180000),variant("full","hls",false,180000));stream=api.playable(stale);check(!stream.preview&&requested("/media/full"),"untrusted_audio_url_rejected_then_valid_full_variant");
  media(variant("short","progressive",false,30000));stream=api.playable(stale);check(stream.preview&&stream.note.contains("Превью"),"short_duration_without_snipped_not_full_success");
  media(variant("preview","hls",true,30000));stream=api.playable(stale);check(stream.preview&&stream.duration==30000,"only_preview_truthfully_labelled");
  media(variant("policy","hls",false,30000));track.put("policy","SNIP");stream=api.playable(stale);check(stream.preview,"service_snip_policy_preserved");
  media(variant("bad","progressive",false,180000),variant("preview","hls",true,30000));blocked=false;try{api.playable(stale);}catch(ScApi.ApiError e){blocked=e.status==403;}check(blocked&&!requested("/media/preview"),"failed_full_not_silently_downgraded_to_preview");
  for(int code:new int[]{401,429,503}){resolutionStatus=code;media(variant("bad","progressive",false,180000),variant("full","hls",false,180000));blocked=false;try{api.playable(stale);}catch(ScApi.ApiError e){blocked=e.status==code;}check(blocked&&!requested("/media/full"),"stream_"+code+"_terminal_no_request_storm");}
  media(variant("full","hls",false,180000));track.put("policy","BLOCK");blocked=false;try{api.playable(stale);}catch(IOException e){blocked=true;}check(blocked&&!requested("/media/full"),"blocked_policy_never_rewritten_or_bypassed");
  media(variant("full","hls",false,180000));track.put("downloadable",false);blocked=false;try{new OfflineStore(c).save(stale,new OfflineStore.Progress(){public boolean cancelled(){return false;}public void update(int p){}});}catch(IOException e){blocked="NO_OFFLINE_SOURCE".equals(e.getMessage());}check(blocked&&new OfflineStore(c).bySource("456")==null,"full_clear_online_is_not_offline_permission_no_fake_record");
  result.putString("scope","Synthetic API contracts only. Real SoundCloud writes, account entitlements and offline licences NOT verified.");
 }catch(Throwable e){result.putString("failure",e.getClass().getSimpleName()+" "+(e instanceof AssertionError?e.getMessage():"see test trace"));e.printStackTrace();}finally{Session.clear(c);c.getSharedPreferences("profile",0).edit().clear().commit();}
 result.putInt("checks",n);i.finish(result.containsKey("failure")?Activity.RESULT_CANCELED:Activity.RESULT_OK,result);
 }
}
