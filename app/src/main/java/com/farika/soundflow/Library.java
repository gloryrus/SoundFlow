package com.farika.soundflow;

import android.content.*;
import org.json.*;
import java.util.*;

public final class Library {
 public static class Track {
  public String uri,title,artist; public long duration; public boolean liked; public JSONObject remote;public String reason="";public long savedPosition;
  public boolean preview(){return remote!=null&&"SNIP".equalsIgnoreCase(remote.optString("policy"));}
  public boolean cloud(){return uri.startsWith("sc:");}
  public String id(){return remote==null?"":remote.optString("id");}
  public String artwork(){if(remote==null)return "";String art=remote.isNull("artwork_url")?"":remote.optString("artwork_url","");JSONObject user=remote.optJSONObject("user");return art.isEmpty()&&user!=null?user.optString("avatar_url",""):art;}
  public String link(){return remote==null?"":remote.optString("permalink_url","");}
  Track(String u,String t,String a,long d,boolean l){uri=u;title=t;artist=a;duration=d;liked=l;}
 }
 public final ArrayList<Track> tracks=new ArrayList<>();
 public final LinkedHashMap<String,ArrayList<String>> playlists=new LinkedHashMap<>();
 private final SharedPreferences prefs;private final Context context;
 public Library(Context c){context=c.getApplicationContext();prefs=c.getSharedPreferences("library",0);load();}
 private void load(){
  try{JSONArray a=new JSONArray(prefs.getString("tracks","[]"));
   for(int i=0;i<a.length();i++){JSONObject t=a.getJSONObject(i);Track track=new Track(t.getString("uri"),t.getString("title"),t.optString("artist","Неизвестный исполнитель"),t.optLong("duration"),t.optBoolean("liked"));track.remote=compactRemote(t.optJSONObject("remote"));track.reason=t.optString("reason","");tracks.add(track);}
   JSONObject p=new JSONObject(prefs.getString("playlists","{}")); Iterator<String> it=p.keys();
   while(it.hasNext()){String n=it.next();JSONArray ar=p.getJSONArray(n);ArrayList<String> ids=new ArrayList<>();for(int i=0;i<ar.length();i++)ids.add(ar.getString(i));playlists.put(n,ids);}
  }catch(JSONException ignored){}
 }
 static JSONObject safeRemote(JSONObject source)throws JSONException{if(source==null)return null;JSONObject out=new JSONObject();for(String key:new String[]{"id","permalink_url","artwork_url","downloadable","streamable","policy","full_duration","monetization_model","genre"})if(source.has(key)&&!source.isNull(key))out.put(key,source.get(key));if((!out.has("artwork_url")||out.optString("artwork_url").isEmpty())&&source.optJSONObject("user")!=null){String avatar=source.optJSONObject("user").optString("avatar_url","");if(!avatar.isEmpty())out.put("artwork_url",avatar);}return out;}
 static JSONObject json(Track t)throws JSONException{return new JSONObject().put("uri",t.uri).put("title",t.title).put("artist",t.artist).put("duration",t.duration).put("liked",t.liked).put("remote",safeRemote(t.remote)).put("reason",t.reason);}
 static JSONObject compactRemote(JSONObject source){try{return safeRemote(source);}catch(JSONException e){throw new IllegalArgumentException("Invalid track metadata",e);}}
 static Track fromJson(JSONObject j){Track t=new Track(j.optString("uri"),j.optString("title"),j.optString("artist"),j.optLong("duration"),j.optBoolean("liked"));t.remote=compactRemote(j.optJSONObject("remote"));t.reason=j.optString("reason","");return t;}
 public void save(){
  try{JSONArray a=new JSONArray();for(Track t:tracks)if(!OfflineStore.isOffline(t.uri)&&!LocalAudioStore.isLocal(t.uri))a.put(json(t));
   JSONObject p=new JSONObject();for(Map.Entry<String,ArrayList<String>> e:playlists.entrySet())p.put(e.getKey(),new JSONArray(e.getValue()));
   prefs.edit().putString("tracks",a.toString()).putString("playlists",p.toString()).apply();
  }catch(JSONException ignored){}
 }
 public void upsert(Track t){if(LocalAudioStore.isLocal(t.uri))return;Track old=find(t.uri);if(old!=null)tracks.remove(old);tracks.add(t);}
 public Track find(String uri){if(LocalAudioStore.isLocal(uri))return new LocalAudioStore(context).find(uri);for(Track t:tracks)if(t.uri.equals(uri))return t;return OfflineStore.isOffline(uri)?new OfflineStore(context).find(uri):new WaveCatalog(context).get(uri);}
 public static String time(long ms){long s=Math.max(0,ms/1000);return String.format(Locale.ROOT,"%d:%02d",s/60,s%60);}
}
