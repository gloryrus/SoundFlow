package com.farika.soundflow;
import android.content.*;import android.graphics.*;import android.os.*;import android.util.LruCache;import android.widget.ImageView;import java.lang.ref.WeakReference;import java.util.*;import java.util.concurrent.*;
/** Bounded, deduplicated artwork pipeline. Never retains an Activity or a recycled row strongly. */
final class ArtworkLoader implements AutoCloseable {
 static final int CACHE_BYTES=2*1024*1024,MAX_PENDING=12,WORKERS=2;
 interface Source {Bitmap read(String url,int edge)throws Exception;}
 private final Source source;private final Handler main=new Handler(Looper.getMainLooper());private final ExecutorService executor=Executors.newFixedThreadPool(WORKERS);
 private final LruCache<String,Bitmap> cache=new LruCache<String,Bitmap>(CACHE_BYTES){protected int sizeOf(String k,Bitmap b){return b.getAllocationByteCount();}};
 private final LinkedHashMap<String,Job> pending=new LinkedHashMap<>();private final HashMap<String,Job> flight=new HashMap<>();private int workers,epoch;private boolean paused,closed;
 private static class Job {String key,url;int edge,epoch;ArrayList<WeakReference<ImageView>> views=new ArrayList<>();}
 ArtworkLoader(Source s){source=s;}
 void bind(ImageView v,String url,int edge){if(url==null)url="";edge=Math.max(32,Math.min(768,edge));String key=url+"#"+edge;if(url.equals(v.getTag())&&v.getDrawable()!=null)return;v.setTag(url);v.setImageDrawable(null);if(url.isEmpty())return;Bitmap hit=cache.get(key);if(hit!=null){v.setImageBitmap(hit);return;}synchronized(this){if(closed||paused){v.setTag(null);return;}Bitmap again=cache.get(key);if(again!=null){v.setImageBitmap(again);return;}Job job=flight.get(key);if(job==null)job=pending.get(key);if(job==null){job=new Job();job.key=key;job.url=url;job.edge=edge;job.epoch=epoch;if(pending.size()>=MAX_PENDING){Job drop=pending.remove(pending.keySet().iterator().next());reset(drop);}pending.put(key,job);}boolean found=false;for(WeakReference<ImageView> w:job.views)if(w.get()==v)found=true;if(!found)job.views.add(new WeakReference<>(v));while(workers<WORKERS&&!pending.isEmpty()){workers++;executor.execute(this::drain);}}}
 private void reset(Job job){main.post(()->{for(WeakReference<ImageView> w:job.views){ImageView v=w.get();if(v!=null&&job.url.equals(v.getTag())&&v.getDrawable()==null)v.setTag(null);}});}
 private void drain(){for(;;){Job job;synchronized(this){if(closed||paused||pending.isEmpty()){workers--;return;}job=pending.remove(pending.keySet().iterator().next());flight.put(job.key,job);}Bitmap bitmap=null;try{bitmap=source.read(job.url,job.edge);}catch(Exception ignored){}final Bitmap ready=bitmap;final ArrayList<WeakReference<ImageView>> views;synchronized(this){flight.remove(job.key,job);views=new ArrayList<>(job.views);if(ready!=null&&!closed&&!paused&&job.epoch==epoch)cache.put(job.key,ready);}main.post(()->{synchronized(ArtworkLoader.this){if(closed||paused||job.epoch!=epoch)return;}for(WeakReference<ImageView> ref:views){ImageView v=ref.get();if(v!=null&&job.url.equals(v.getTag())){if(ready!=null)v.setImageBitmap(ready);else v.setTag(null);}}});}}
 synchronized void suspend(){paused=true;epoch++;for(Job job:pending.values())reset(job);pending.clear();flight.clear();}
 synchronized void pause(){paused=true;epoch++;for(Job job:pending.values())reset(job);pending.clear();flight.clear();cache.evictAll();}
 void trim(){cache.evictAll();}
 synchronized void resume(){if(!closed)paused=false;}
 synchronized int pendingCount(){return pending.size();}synchronized int flightCount(){return flight.size();}int cacheBytes(){return cache.size();}
 public synchronized void close(){closed=true;pause();executor.shutdownNow();main.removeCallbacksAndMessages(null);}
}
