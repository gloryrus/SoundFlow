package com.farika.soundflow;
import java.util.concurrent.atomic.AtomicLong;
final class DownloadCancellation {private static final AtomicLong revision=new AtomicLong();static long ticket(){return revision.get();}static boolean current(long ticket){return revision.get()==ticket;}static void cancel(){revision.incrementAndGet();}}
