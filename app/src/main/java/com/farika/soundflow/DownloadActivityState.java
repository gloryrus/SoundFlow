package com.farika.soundflow;
import java.util.concurrent.*;import java.util.concurrent.atomic.*;
/** Actual work only; scheduled auto-checks or waiting queues do not animate as downloads. */
final class DownloadActivityState {
 private static final ConcurrentHashMap<String,AtomicInteger> active=new ConcurrentHashMap<>();
 static void begin(String owner){active.compute(owner,(k,n)->{if(n==null)n=new AtomicInteger();n.incrementAndGet();return n;});}
 static void end(String owner){active.computeIfPresent(owner,(k,n)->n.decrementAndGet()<=0?null:n);}
 static boolean active(String owner){AtomicInteger n=active.get(owner);return n!=null&&n.get()>0;}
}
