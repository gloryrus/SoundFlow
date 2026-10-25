package com.farika.soundflow;
import android.app.Activity;
import android.os.Looper;
import android.view.*;
import android.widget.*;
import java.util.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

/** Android View/MotionEvent dispatch via Robolectric; not a physical device or emulator test. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=28,manifest=Config.NONE,application=android.app.Application.class)
public class SwipeDispatchTest {
 Activity activity;TrackSwipeLayout layout;final List<Integer> actions=new ArrayList<>();int clicks;float density;
 @Before public void setup(){activity=Robolectric.buildActivity(Activity.class).setup().get();layout=new TrackSwipeLayout(activity,actions::add);layout.setOrientation(LinearLayout.VERTICAL);density=activity.getResources().getDisplayMetrics().density;activity.setContentView(layout);}
 private void arrange(){int w=(int)(400*density),h=(int)(500*density);layout.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(h,View.MeasureSpec.EXACTLY));layout.layout(0,0,w,h);}
 private void event(int type,float x,float y,long when){MotionEvent e=MotionEvent.obtain(100,when,type,x*density,y*density,0);layout.dispatchTouchEvent(e);e.recycle();Shadows.shadowOf(Looper.getMainLooper()).idle();}
 private void swipe(float from,float to){event(MotionEvent.ACTION_DOWN,from,80,100);event(MotionEvent.ACTION_MOVE,(from+to)/2,82,200);event(MotionEvent.ACTION_UP,to,83,300);}
 @Test public void leftAndRightExactlyOnce(){arrange();event(0,300,80,100);event(2,180,82,200);assertTrue(actions.isEmpty());event(1,80,83,300);assertEquals(Arrays.asList(TrackSwipe.NEXT),actions);swipe(80,300);assertEquals(Arrays.asList(TrackSwipe.NEXT,TrackSwipe.PREVIOUS),actions);}
 @Test public void childTapStillClicks(){Button button=new Button(activity);button.setOnClickListener(v->clicks++);layout.addView(button,new LinearLayout.LayoutParams(-1,(int)(120*density)));arrange();event(0,200,60,100);event(1,201,61,150);assertEquals(1,clicks);assertTrue(actions.isEmpty());}
 @Test public void swipeCancelsChildClick(){Button button=new Button(activity);button.setOnClickListener(v->clicks++);layout.addView(button,new LinearLayout.LayoutParams(-1,(int)(120*density)));arrange();swipe(300,80);assertEquals(Arrays.asList(TrackSwipe.NEXT),actions);assertEquals(0,clicks);}
 @Test public void shortDragDoesNotOpenMiniPlayer(){layout.setOnClickListener(v->clicks++);arrange();event(0,250,80,100);event(2,220,80,200);event(1,219,80,300);assertEquals(0,clicks);assertTrue(actions.isEmpty());}
 @Test public void dragBackDoesNotOpenMiniPlayer(){layout.setOnClickListener(v->clicks++);arrange();event(0,250,80,100);event(2,180,80,200);event(1,249,80,300);assertEquals(0,clicks);assertTrue(actions.isEmpty());}
 @Test public void miniTapOpensPlayer(){layout.setOnClickListener(v->clicks++);arrange();event(0,200,80,100);event(1,201,81,160);assertEquals(1,clicks);assertTrue(actions.isEmpty());}
 @Test public void verticalAndCancelledGesturesDoNotSkip(){arrange();event(0,200,80,100);event(2,210,200,200);event(1,210,250,300);assertTrue(actions.isEmpty());event(0,300,80,100);event(2,180,82,200);event(3,80,83,300);assertTrue(actions.isEmpty());}
 @Test public void seekBarKeepsHorizontalDrag(){SeekBar seek=new SeekBar(activity);seek.setMax(100);layout.addView(seek,new LinearLayout.LayoutParams(-1,(int)(120*density)));arrange();event(0,80,60,100);event(2,180,60,200);event(1,300,60,300);assertTrue(actions.isEmpty());assertTrue(seek.getProgress()>0);}
 @Test public void secondPointerCancelsSkip(){arrange();event(0,300,80,100);event(2,180,82,200);MotionEvent.PointerProperties[] pp={new MotionEvent.PointerProperties(),new MotionEvent.PointerProperties()};MotionEvent.PointerCoords[] pc={new MotionEvent.PointerCoords(),new MotionEvent.PointerCoords()};for(int j=0;j<2;j++){pp[j].id=j;pp[j].toolType=MotionEvent.TOOL_TYPE_FINGER;pc[j].x=(180+j*30)*density;pc[j].y=82*density;pc[j].pressure=1;pc[j].size=1;}MotionEvent e=MotionEvent.obtain(100,240,MotionEvent.ACTION_POINTER_DOWN|(1<<MotionEvent.ACTION_POINTER_INDEX_SHIFT),2,pp,pc,0,0,1,1,0,0,android.view.InputDevice.SOURCE_TOUCHSCREEN,0);layout.dispatchTouchEvent(e);e.recycle();event(1,80,83,300);assertTrue(actions.isEmpty());}
}
