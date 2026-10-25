package com.farika.soundflow;
import android.content.Context;import android.database.DataSetObserver;import android.os.*;import android.view.*;import android.widget.*;
/** One explicit hold timer. Native AbsListView long-click/UP must never open a second menu. */
final class HoldListView extends ListView {
 static final int HOLD_MS=350;private final Handler handler=new Handler(Looper.getMainLooper());private float x,y;private boolean down,handled;private int position=INVALID_POSITION;private Object item;private final int slop;private OnItemLongClickListener menu;
 private final DataSetObserver changed=new DataSetObserver(){public void onChanged(){cancelHold();}public void onInvalidated(){cancelHold();}};
 HoldListView(Context c){super(c);slop=Math.max(ViewConfiguration.get(c).getScaledTouchSlop(),Math.round(8*c.getResources().getDisplayMetrics().density));}
 @Override public void setAdapter(ListAdapter adapter){ListAdapter old=getAdapter();if(old!=null)old.unregisterDataSetObserver(changed);super.setAdapter(adapter);if(adapter!=null)adapter.registerDataSetObserver(changed);if(handler!=null)cancelHold();}
 @Override public void setOnItemLongClickListener(OnItemLongClickListener listener){menu=listener;super.setOnItemLongClickListener(null);setLongClickable(false);}
 private void cancelHold(){down=false;handler.removeCallbacks(hold);}
 private final Runnable hold=()->{if(!down||handled||menu==null||position<0||getAdapter()==null||position>=getAdapter().getCount()||item!=getAdapter().getItem(position))return;View row=getChildAt(position-getFirstVisiblePosition());if(row==null)return;handled=true;down=false;if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(true);MotionEvent cancel=MotionEvent.obtain(SystemClock.uptimeMillis(),SystemClock.uptimeMillis(),MotionEvent.ACTION_CANCEL,x,y,0);super.dispatchTouchEvent(cancel);cancel.recycle();setPressed(false);row.setPressed(false);performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);menu.onItemLongClick(this,row,position,getAdapter().getItemId(position));};
 @Override public boolean dispatchTouchEvent(MotionEvent e){int action=e.getActionMasked();if(action==MotionEvent.ACTION_DOWN){cancelHold();handled=false;x=e.getX();y=e.getY();position=pointToPosition((int)x,(int)y);item=position>=0&&getAdapter()!=null?getAdapter().getItem(position):null;boolean result=super.dispatchTouchEvent(e);down=e.getPointerCount()==1&&position>=0;if(down)handler.postDelayed(hold,HOLD_MS);return result||down;}
 if(action==MotionEvent.ACTION_CANCEL||action==MotionEvent.ACTION_UP){cancelHold();if(handled){handled=false;if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(false);return true;}}
 if(action==MotionEvent.ACTION_MOVE&&(Math.abs(e.getX()-x)>slop||Math.abs(e.getY()-y)>slop)||e.getPointerCount()!=1)cancelHold();if(handled)return true;return super.dispatchTouchEvent(e);}
 @Override protected void onDetachedFromWindow(){cancelHold();handled=false;super.onDetachedFromWindow();}
}
