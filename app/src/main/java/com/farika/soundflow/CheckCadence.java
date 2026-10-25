package com.farika.soundflow;
import android.content.*;import org.json.*;
/** Fifteen-minute metadata cadence. Audio retries reuse the saved snapshot, not the remote list. */
final class CheckCadence {
 static final long INTERVAL=15*60*1000L;
 static long next(Context c,String owner,String id,long now){long last=c.getSharedPreferences("sync_check_times",0).getLong(owner+"\n"+id,0);return last<=0||last>now?now:last+INTERVAL;}
 static boolean due(Context c,String owner,String id,long now){return next(c,owner,id,now)<=now;}
 static void attempted(Context c,String owner,String id){c.getSharedPreferences("sync_check_times",0).edit().putLong(owner+"\n"+id,System.currentTimeMillis()).commit();}
 static boolean refresh(Context c,String owner,JSONObject p,SyncRequests.Request r,long now){return r.refresh||!p.optBoolean("listed");}
 static boolean runnable(Context c,SyncDb db,String owner,JSONObject p,long now){String id=p.optString("id");return SyncRequests.pending(c,owner,id)||db.nextItemRetry(owner,id)<=now;}
 static long retryAt(Context c,SyncDb db,String owner,long now){long at=Long.MAX_VALUE;for(JSONObject p:db.workPlans(owner)){String id=p.optString("id");if(SyncRequests.pending(c,owner,id))at=Math.min(at,now+30000);at=Math.min(at,db.nextItemRetry(owner,id));}return at==Long.MAX_VALUE?0:Math.max(now+30000,at);}
}
