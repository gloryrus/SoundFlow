package com.farika.soundflow;
import android.content.*;import android.graphics.*;import android.os.SystemClock;import android.view.*;import android.widget.TextView;
final class DownloadIndicator extends TextView {
 private boolean active;private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);private final RectF ring=new RectF();private final Runnable frame=this::invalidate;
 DownloadIndicator(Context c){super(c);setText("↓");setTextSize(24);setGravity(Gravity.CENTER);setTextColor(SoundFlowUi.TEXT);setContentDescription("Загрузки");setMinWidth(SoundFlowUi.dp(c,48));setMinHeight(SoundFlowUi.dp(c,48));setTooltipText(null);Motion.press(this);}
 void setActive(boolean value){if(active==value)return;active=value;setTextColor(active?SoundFlowUi.ACCENT:SoundFlowUi.TEXT);setContentDescription(active?"Загрузки: идёт скачивание":"Загрузки");removeCallbacks(frame);invalidate();}
 boolean active(){return active;}
 @Override protected void onDraw(Canvas canvas){boolean animate=active&&isShown()&&hasWindowFocus()&&Motion.enabled(getContext());float phase=(SystemClock.elapsedRealtime()%1400)/1400f;canvas.save();if(animate)canvas.translate(0,(float)Math.sin(phase*Math.PI*2)*SoundFlowUi.dp(getContext(),2));super.onDraw(canvas);canvas.restore();if(active){paint.setColor(SoundFlowUi.ACCENT);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(SoundFlowUi.dp(getContext(),2));paint.setStrokeCap(Paint.Cap.ROUND);float radius=Math.min(getWidth(),getHeight())*.34f;ring.set(getWidth()/2f-radius,getHeight()/2f-radius,getWidth()/2f+radius,getHeight()/2f+radius);canvas.drawArc(ring,animate?phase*360:-90,100,false,paint);}removeCallbacks(frame);if(animate)postDelayed(frame,50);}
 @Override protected void onDetachedFromWindow(){removeCallbacks(frame);super.onDetachedFromWindow();}
 @Override public void onWindowFocusChanged(boolean focus){super.onWindowFocusChanged(focus);removeCallbacks(frame);if(focus)invalidate();}
}
