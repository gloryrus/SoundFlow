package com.farika.soundflow;
final class OfflineQuality {
 static long expected(Library.Track t){return Math.max(t.duration,t.remote==null?0:t.remote.optLong("full_duration"));}
 static boolean shortCopy(long actual,long full){return actual>0&&full-actual>5000&&actual<full*.9;}
 static boolean needsRepair(Library.Track copy,Library.Track source){return copy!=null&&(copy.preview()||shortCopy(copy.duration,Math.max(expected(copy),expected(source))));}
}
