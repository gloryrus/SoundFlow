package com.farika.soundflow;
import android.content.*;
import org.json.*;
import java.util.*;
/** Local, account-scoped implicit feedback. No listening data is uploaded by this class. */
public final class TasteStore {
 private final SharedPreferences p;
 public TasteStore(Context c){String id=c.getSharedPreferences("profile",0).getString("id","");p=c.getSharedPreferences("taste_"+id,0);}
 public JSONObject stats(String id){try{return new JSONObject(p.getString(id,"{}"));}catch(Exception e){return new JSONObject();}}
 public void record(String id,long listened,long duration,boolean skipped){if(id==null||id.isEmpty()||listened<1500)return;synchronized(TasteStore.class){try{JSONObject s=stats(id);s.put("ms",Math.min(31536000000L,s.optLong("ms")+listened)).put("last",System.currentTimeMillis());if(duration>0&&listened>=Math.min(90000,duration*.6))s.put("plays",Math.min(1000,s.optInt("plays")+1));if(skipped&&duration>=45000&&listened<15000)s.put("skips",Math.min(1000,s.optInt("skips")+1));p.edit().putString(id,s.toString()).apply();prune();}catch(JSONException ignored){}}}
 public void hide(String id){synchronized(TasteStore.class){try{JSONObject s=stats(id);s.put("hidden",true);p.edit().putString(id,s.toString()).apply();}catch(Exception ignored){}}}
 public double score(String id){JSONObject s=stats(id);return Math.min(8,s.optInt("plays")*1.5)+Math.min(3,Math.log1p(s.optLong("ms")/60000.0))-Math.min(12,s.optInt("skips")*2.5);}
 public boolean known(String id){return stats(id).optInt("plays")>0;}
 public boolean seen(String id){JSONObject s=stats(id);return s.optInt("plays")>0||s.optLong("ms")>=15000;}
 public boolean hidden(String id){return stats(id).optBoolean("hidden");}
 public void clear(){synchronized(TasteStore.class){p.edit().clear().commit();}}
 private void prune(){if(p.getAll().size()<=1000)return;ArrayList<String> ids=new ArrayList<>(p.getAll().keySet());ids.sort((a,b)->Long.compare(stats(a).optLong("last"),stats(b).optLong("last")));SharedPreferences.Editor e=p.edit();for(int i=0;i<ids.size()-1000;i++)e.remove(ids.get(i));e.apply();}
}
