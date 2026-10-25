package com.farika.soundflow;
import android.content.*;import android.content.res.*;import android.graphics.drawable.*;import android.view.*;import android.widget.*;
final class SoundFlowUi {
 static void notice(Context context,CharSequence message){if(!(context instanceof android.app.Activity))return;android.app.Activity activity=(android.app.Activity)context;if(activity.isFinishing()||activity.isDestroyed())return;ViewGroup root=activity.findViewById(android.R.id.content);TextView status=root.findViewWithTag("inline-status");if(status==null){LinearLayout column=findColumn(root);if(column==null)return;status=text(context,"",14,MUTED,false);status.setTag("inline-status");status.setMaxLines(3);status.setPadding(dp(context,16),dp(context,6),dp(context,16),dp(context,6));column.addView(status,Math.min(1,column.getChildCount()),new LinearLayout.LayoutParams(-1,-2));}status.setText(message);}
 private static LinearLayout findColumn(View v){if(v instanceof LinearLayout&&((LinearLayout)v).getOrientation()==LinearLayout.VERTICAL)return (LinearLayout)v;if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++){LinearLayout found=findColumn(g.getChildAt(i));if(found!=null)return found;}}return null;}
 static final int BG=0xff0b0e14,SURFACE=0xff171d27,RAISED=0xff222b38,TEXT=0xfff4f7fc,MUTED=0xffa5b0c2,ACCENT=0xffc2f578,ERROR=0xffffb0a3;
 static int dp(Context c,int n){return Math.round(n*c.getResources().getDisplayMetrics().density);}
 static GradientDrawable shape(Context c,int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(c,radius));return d;}
 static LinearLayout column(Context c){LinearLayout l=new LinearLayout(c);l.setOrientation(1);return l;}
 static TextView text(Context c,String value,int size,int color,boolean bold){TextView v=new TextView(c);v.setText(value);v.setTextSize(size);v.setTextColor(color);if(bold)v.setTypeface(android.graphics.Typeface.create("sans-serif-medium",0));return v;}
 static TextView button(Context c,String title,boolean primary,Runnable action){TextView v=text(c,title,15,primary?BG:TEXT,true);v.setGravity(Gravity.CENTER);v.setPadding(dp(c,16),dp(c,12),dp(c,16),dp(c,12));v.setMinHeight(dp(c,48));v.setBackground(new RippleDrawable(ColorStateList.valueOf(0x227fffff),shape(c,primary?ACCENT:RAISED,14),null));v.setClickable(true);v.setFocusable(true);v.setOnClickListener(w->action.run());Motion.press(v);return v;}
 static LinearLayout card(Context c){LinearLayout l=column(c);l.setPadding(dp(c,16),dp(c,16),dp(c,16),dp(c,16));l.setBackground(shape(c,SURFACE,18));return l;}
 static void gap(LinearLayout l,int n){l.addView(new View(l.getContext()),new LinearLayout.LayoutParams(1,dp(l.getContext(),n)));}
}
