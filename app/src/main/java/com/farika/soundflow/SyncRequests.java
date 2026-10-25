package com.farika.soundflow;
import android.content.*;import org.json.*;import java.util.*;
/** Durable explicit refresh/retry intent. Revisions protect newer requests from old completions. */
final class SyncRequests {
 static final class Request {String token="";boolean refresh,force,manual;}
 private static String key(String owner,String id){return owner+"\n"+id;}
 static synchronized Request get(Context c,String owner,String id){Request r=new Request();try{JSONObject j=new JSONObject(c.getSharedPreferences("sync_requests",0).getString(key(owner,id),""));r.token=j.optString("token");r.refresh=j.optBoolean("refresh");r.force=j.optBoolean("force");r.manual=j.optBoolean("manual");}catch(Exception ignored){}return r;}
 static synchronized void put(Context c,String owner,String id,boolean refresh,boolean force,boolean manual){Request old=get(c,owner,id);try{JSONObject j=new JSONObject().put("token",UUID.randomUUID().toString()).put("refresh",refresh||old.refresh).put("force",force||old.force).put("manual",manual||old.manual);if(!c.getSharedPreferences("sync_requests",0).edit().putString(key(owner,id),j.toString()).commit())throw new IllegalStateException("Не удалось сохранить запрос очереди");}catch(JSONException e){throw new IllegalStateException(e);}}
 static synchronized void consume(Context c,String owner,String id,Request request){if(!request.token.isEmpty()&&request.token.equals(get(c,owner,id).token))c.getSharedPreferences("sync_requests",0).edit().remove(key(owner,id)).commit();}
 static boolean pending(Context c,String owner,String id){return !get(c,owner,id).token.isEmpty();}
 static boolean any(Context c,String owner,boolean manualOnly){for(String key:c.getSharedPreferences("sync_requests",0).getAll().keySet())if(key.startsWith(owner+"\n")){Request r=get(c,owner,key.substring(owner.length()+1));if(!r.token.isEmpty()&&(!manualOnly||r.manual))return true;}return false;}
}
