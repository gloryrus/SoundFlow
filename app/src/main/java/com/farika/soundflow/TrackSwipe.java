package com.farika.soundflow;

/** Deterministic single-pointer gesture policy, coordinates in dp. No playback or Android dependency. */
public final class TrackSwipe {
 public static final int NONE=0,NEXT=1,PREVIOUS=-1;
 private float x,y;private long time;private boolean active,horizontal,vertical;
 public void begin(float x,float y,long time){this.x=x;this.y=y;this.time=time;active=Float.isFinite(x)&&Float.isFinite(y);horizontal=false;vertical=false;}
 public boolean move(float x,float y){
  if(!active||vertical||!Float.isFinite(x)||!Float.isFinite(y))return false;
  float dx=Math.abs(x-this.x),dy=Math.abs(y-this.y);
  if(!horizontal){if(dy>18&&dy>dx*1.3f){vertical=true;return false;}if(dx>18&&dx>dy*1.5f)horizontal=true;}
  return horizontal;
 }
 public int finish(float x,float y,long now){
  boolean h=move(x,y);long elapsed=now-time;float dx=x-this.x,dy=y-this.y;
  int result=active&&h&&elapsed>=0&&elapsed<=2000&&Float.isFinite(dx)&&Float.isFinite(dy)&&Math.abs(dx)>=56&&Math.abs(dx)>Math.abs(dy)*1.5f?(dx<0?NEXT:PREVIOUS):NONE;
  cancel();return result;
 }
 public void cancel(){active=false;horizontal=false;vertical=false;}
}
