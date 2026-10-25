package com.farika.soundflow;
final class RepeatPolicy {
 static boolean listContext(String page){return "library".equals(page)||"own".equals(page);}
 static int next(int mode,boolean playlist){return mode==0?1:mode==1&&playlist?2:0;}
 static boolean wraps(boolean playlist,boolean repeatAll){return playlist&&repeatAll;}
}
