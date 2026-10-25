package com.farika.soundflow;
import android.content.*;import org.json.*;import java.util.*;
/** Durable account/settings-scoped feed snapshot. No player URLs or credentials. */
final class HomeFeed {
 private final android.content.SharedPreferences p;HomeFeed(Context c){p=c.getSharedPreferences("home_feed_v1",0);}
 static final class State {ArrayList<Library.Track> tracks=new ArrayList<>();String note="";int position,offset;}
 String key(String user,String mode,boolean familiar){return user+":"+mode+":"+familiar;}
 State get(String key){try{JSONObject j=new JSONObject(p.getString(key,""));State s=new State();s.note=j.optString("note");s.position=j.optInt("position");s.offset=j.optInt("offset");JSONArray a=j.getJSONArray("tracks");for(int i=0;i<a.length();i++)s.tracks.add(Library.fromJson(a.getJSONObject(i)));return s;}catch(Exception e){return null;}}
 void put(String key,List<Library.Track> tracks,String note,int pos,int offset){try{JSONArray a=new JSONArray();for(Library.Track t:tracks)a.put(Library.json(t));JSONObject j=new JSONObject().put("tracks",a).put("note",note).put("position",pos).put("offset",offset);p.edit().putString(key,j.toString()).apply();}catch(Exception ignored){}}
 void clear(){p.edit().clear().apply();}
}
