package com.farika.soundflow;
import android.content.Context;
import org.json.*;
import java.util.*;
/** Only stable library identifiers, never expiring audio URLs, cookies or tokens. */
final class PlaybackCheckpoint {
 static final class Snapshot {
  final ArrayList<String> queue=new ArrayList<>(); int index; long position; boolean shuffle,repeat,wanted,suspended,playlistContext,repeatPlaylist,wave; long savedAt;
  String current(){return queue.isEmpty()?"":queue.get(index);}
 }
 static String owner(Context c){return c.getSharedPreferences("profile",0).getString("id","");}
 static void clear(Context c){c.getSharedPreferences("playback_checkpoint",0).edit().clear().commit();}
 static boolean save(Context c,List<String> queue,int index,long position,boolean shuffle,boolean repeat){
  return save(c,queue,index,position,shuffle,repeat,false,false);
 }
 static boolean save(Context c,List<String> queue,int index,long position,boolean shuffle,boolean repeat,boolean wanted,boolean suspended){
  return save(c,queue,index,position,shuffle,repeat,wanted,suspended,false,false);
 }
 static boolean save(Context c,List<String> queue,int index,long position,boolean shuffle,boolean repeat,boolean wanted,boolean suspended,boolean playlistContext,boolean repeatPlaylist){
  return save(c,queue,index,position,shuffle,repeat,wanted,suspended,playlistContext,repeatPlaylist,false);
 }
 static boolean save(Context c,List<String> queue,int index,long position,boolean shuffle,boolean repeat,boolean wanted,boolean suspended,boolean playlistContext,boolean repeatPlaylist,boolean wave){
  if(queue.isEmpty()||index<0||index>=queue.size())return false;
  try{JSONArray ids=new JSONArray();for(String id:queue)ids.put(id);
   JSONObject j=new JSONObject().put("wave",wave).put("playlistContext",playlistContext).put("repeatPlaylist",playlistContext&&repeatPlaylist).put("schema",1).put("owner",owner(c)).put("queue",ids).put("index",index).put("position",Math.max(0,position)).put("shuffle",shuffle).put("repeat",repeat).put("wanted",wanted).put("suspended",suspended).put("savedAt",System.currentTimeMillis());
   return c.getSharedPreferences("playback_checkpoint",0).edit().putString("state",j.toString()).commit();
  }catch(Exception e){return false;}
 }
 static void forget(Context c,String uri){try{android.content.SharedPreferences prefs=c.getSharedPreferences("playback_checkpoint",0);String raw=prefs.getString("state","");if(raw.isEmpty())return;JSONObject j=new JSONObject(raw);JSONArray ids=j.getJSONArray("queue"),kept=new JSONArray();int oldIndex=j.optInt("index"),newIndex=-1;String selected=ids.optString(oldIndex);boolean changed=false;for(int i=0;i<ids.length();i++){String value=ids.optString(i);if(uri.equals(value)){changed=true;continue;}if(i==oldIndex)newIndex=kept.length();kept.put(value);}if(!changed)return;if(kept.length()==0){clear(c);return;}if(newIndex<0){newIndex=Math.min(oldIndex,kept.length()-1);j.put("position",0).put("wanted",false).put("suspended",false);}j.put("queue",kept).put("index",newIndex);prefs.edit().putString("state",j.toString()).commit();}catch(Exception ignored){}}
 static Snapshot read(Context c){
  try{JSONObject j=new JSONObject(c.getSharedPreferences("playback_checkpoint",0).getString("state",""));if(j.optInt("schema")!=1||!owner(c).equals(j.optString("owner")))return null;
   JSONArray ids=j.getJSONArray("queue");int selected=j.getInt("index");if(selected<0||selected>=ids.length())return null;Snapshot s=new Snapshot();Library l=new Library(c);boolean found=false;
   for(int n=0;n<ids.length();n++){String id=ids.getString(n);if(l.find(id)==null)continue;if(n==selected){s.index=s.queue.size();found=true;}s.queue.add(id);}
   if(s.queue.isEmpty())return null;s.position=found?Math.min(Integer.MAX_VALUE,Math.max(0,j.optLong("position"))):0;s.wave=j.optBoolean("wave");s.playlistContext=j.optBoolean("playlistContext");s.repeatPlaylist=s.playlistContext&&j.optBoolean("repeatPlaylist");s.shuffle=j.optBoolean("shuffle");s.repeat=j.optBoolean("repeat");s.wanted=j.optBoolean("wanted");s.suspended=j.optBoolean("suspended");s.savedAt=j.optLong("savedAt");return s;
  }catch(Exception e){return null;}
 }
}
