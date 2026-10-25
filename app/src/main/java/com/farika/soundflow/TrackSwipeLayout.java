package com.farika.soundflow;

import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import java.util.function.IntConsumer;

/** Intercepts a horizontal swipe, not a tap, vertical drag, multitouch, or a seek-bar gesture. */
public final class TrackSwipeLayout extends LinearLayout {
 private final TrackSwipe gesture=new TrackSwipe();private final float density;private final IntConsumer action;
 private boolean blocked,dragged;private int pointer=-1;private float downX,downY;
 public TrackSwipeLayout(Context context,IntConsumer action){super(context);density=context.getResources().getDisplayMetrics().density;this.action=action;}
 private void begin(MotionEvent e){dragged=false;downX=e.getX();downY=e.getY();gesture.cancel();blocked=e.getPointerCount()!=1||seekAt(this,e.getX(),e.getY());pointer=e.getPointerId(0);if(!blocked)gesture.begin(e.getX()/density,e.getY()/density,e.getEventTime());}
 private void moved(MotionEvent e){if(Math.abs(e.getX()-downX)>=18*density||Math.abs(e.getY()-downY)>=18*density)dragged=true;}
 private void cancel(){gesture.cancel();blocked=true;pointer=-1;}
 private boolean valid(MotionEvent e){return !blocked&&e.getPointerCount()==1&&e.getPointerId(0)==pointer;}
 @Override public boolean onInterceptTouchEvent(MotionEvent e){
  switch(e.getActionMasked()){
   case MotionEvent.ACTION_DOWN:begin(e);return false;
   case MotionEvent.ACTION_POINTER_DOWN:case MotionEvent.ACTION_POINTER_UP:case MotionEvent.ACTION_CANCEL:cancel();return false;
   case MotionEvent.ACTION_MOVE:moved(e);return valid(e)&&gesture.move(e.getX()/density,e.getY()/density);
   default:return false;
  }
 }
 @Override public boolean onTouchEvent(MotionEvent e){
  switch(e.getActionMasked()){
   case MotionEvent.ACTION_DOWN:begin(e);return !blocked;
   case MotionEvent.ACTION_POINTER_DOWN:case MotionEvent.ACTION_POINTER_UP:case MotionEvent.ACTION_CANCEL:cancel();return true;
   case MotionEvent.ACTION_MOVE:moved(e);if(valid(e))gesture.move(e.getX()/density,e.getY()/density);return !blocked;
   case MotionEvent.ACTION_UP:
    if(!valid(e)){cancel();return false;}
    moved(e);int direction=gesture.finish(e.getX()/density,e.getY()/density,e.getEventTime());pointer=-1;
    if(direction!=TrackSwipe.NONE)action.accept(direction);else if(!dragged&&isClickable()&&Math.abs(e.getX()-downX)<18*density&&Math.abs(e.getY()-downY)<18*density)performClick();
    return true;
   default:return !blocked;
  }
 }
 @Override public boolean performClick(){return super.performClick();}
 private static boolean seekAt(View view,float x,float y){
  if(view.getVisibility()!=View.VISIBLE||x<0||y<0||x>=view.getWidth()||y>=view.getHeight())return false;
  if(view instanceof SeekBar)return true;
  if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=group.getChildCount()-1;i>=0;i--){View child=group.getChildAt(i);if(seekAt(child,x+group.getScrollX()-child.getX(),y+group.getScrollY()-child.getY()))return true;}}
  return false;
 }
}
