package com.farika.soundflow;
import java.util.concurrent.ConcurrentHashMap;import java.util.concurrent.atomic.AtomicLong;
/** Disabling and even immediately re-enabling a plan invalidates the old running job. */
final class AutoDownloadGate {
 private static final ConcurrentHashMap<String,AtomicLong> revisions=new ConcurrentHashMap<>();
 private static AtomicLong entry(String owner,String id){return revisions.computeIfAbsent(owner+"\n"+id,k->new AtomicLong());}
 static long ticket(String owner,String id){return entry(owner,id).get();}
 static void changed(String owner,String id){entry(owner,id).incrementAndGet();}
 static boolean current(String owner,String id,long ticket){return entry(owner,id).get()==ticket;}
}
