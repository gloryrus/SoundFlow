package com.farika.soundflow;
import android.content.*;import android.os.*;import org.json.*;import java.util.concurrent.*;
/** In-process timer only. No alarm, periodic job, service or wakelock can recreate it. */
final class ResidentChecks {
 static final long INTERVAL=15*60*1000L;private static final Handler main=new Handler(Looper.getMainLooper());private static final ExecutorService worker=Executors.newSingleThreadExecutor();private static boolean started;private static Runnable tick;
 static synchronized void start(Context c){Context app=c.getApplicationContext();start(app,()->check(app));}
 static synchronized void start(Context c,Runnable check){if(started)return;started=true;tick=new Runnable(){public void run(){worker.execute(()->{try{check.run();}finally{main.post(()->{synchronized(ResidentChecks.class){if(started)main.postDelayed(this,INTERVAL);}});}});}};main.postDelayed(tick,INTERVAL);}
 private static void check(Context c){if(!AutoDownloads.allowed(c)||Session.get(c).isEmpty())return;String owner=DownloadSync.owner(c);try(SyncDb db=new SyncDb(c)){for(JSONObject p:db.plans(owner,true)){String id=p.optString("id");if(Thread.currentThread().isInterrupted()||!AutoDownloads.allowed(c)||!owner.equals(DownloadSync.owner(c)))break;if(!CheckCadence.due(c,owner,id,System.currentTimeMillis()))continue;long gate=AutoDownloadGate.ticket(owner,id);try{DownloadSync.run(c,owner,id,p.optString("title"),p.optString("kind"),true,false,new DownloadSync.Monitor(){public boolean cancelled(){return Thread.currentThread().isInterrupted()||!AutoDownloadGate.current(owner,id,gate)||!AutoDownloads.allowed(c);}public void progress(String text,int percent){}public boolean beforeTransfers(int count){return false;}});}catch(Exception ignored){}finally{AutoDownloads.kick(c);AutoDownloads.retry(c);}}}}
 static synchronized boolean running(){return started;}
 static synchronized void resetForTest(){started=false;if(tick!=null)main.removeCallbacks(tick);tick=null;}
}
