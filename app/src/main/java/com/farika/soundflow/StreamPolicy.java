package com.farika.soundflow;

/** Deterministic format selection; no URL rewriting, policy bypass or substitute audio. */
public final class StreamPolicy {
 private StreamPolicy(){}
 public static boolean encrypted(String protocol){return "ctr-encrypted-hls".equals(protocol);}
 public static boolean supported(String protocol,String mime){
  String m=mime==null?"":mime.toLowerCase(java.util.Locale.ROOT).split(";",2)[0].trim();
  if(!(encrypted(protocol)||"hls".equals(protocol)||"progressive".equals(protocol)))return false;
  if(m.equals("audio/mpeg")||m.equals("audio/mp4")||m.equals("audio/aac")||m.equals("audio/aacp"))return true;
  if(m.equals("audio/ogg")||m.equals("audio/opus"))return !encrypted(protocol);
  return !"progressive".equals(protocol)&&(m.equals("audio/mpegurl")||m.equals("application/vnd.apple.mpegurl")||m.equals("application/x-mpegurl")||m.equals("audio/x-mpegurl"));
 }
 public static int rank(String protocol,String mime,String quality,boolean preview){
  return rank(protocol,mime,quality,preview,false);
 }
 /** Clear full playback variants precede DRM for both streaming and private offline caching. */
 public static int rank(String protocol,String mime,String quality,boolean preview,boolean offline){
  int r=encrypted(protocol)?-100:"progressive".equals(protocol)?200:100;
  String m=mime==null?"":mime.toLowerCase(java.util.Locale.ROOT);
  if(m.contains("mpegurl"))r-=60;
  if("sq".equals(quality))r+=20;else if("hq".equals(quality))r+=30;
  if(preview)r-=10000;return r;
 }
 /** A missing snipped flag must not turn an obviously short variant into a full track. */
 public static boolean preview(boolean snipped,String policy,long duration,long fullDuration){return snipped||"SNIP".equalsIgnoreCase(policy)||(duration>0&&fullDuration-duration>5000&&duration<fullDuration*0.9);}
 public static boolean tokenValid(String t){if(t==null||t.length()<8||t.length()>16384)return false;for(int i=0;i<t.length();i++){char c=t.charAt(i);if(c<=32||c>=127)return false;}return true;}
}
