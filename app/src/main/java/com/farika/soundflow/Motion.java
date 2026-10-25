package com.farika.soundflow;
import android.animation.*;import android.content.*;import android.view.*;import android.view.animation.*;
/** Short, optional native motion. No touch interception, repeated list animations or audio work. */
final class Motion {
 static boolean enabled(Context c){return c.getSharedPreferences("appearance",0).getBoolean("motion",true)&&ValueAnimator.areAnimatorsEnabled();}
 static void press(View v){if(!enabled(v.getContext())){v.setStateListAnimator(null);v.setScaleX(1);v.setScaleY(1);return;}StateListAnimator state=new StateListAnimator();state.addState(new int[]{android.R.attr.state_pressed},scale(v,.96f,90));state.addState(new int[]{},scale(v,1,160));v.setStateListAnimator(state);}
 private static Animator scale(View v,float value,long ms){AnimatorSet a=new AnimatorSet();a.playTogether(ObjectAnimator.ofFloat(v,View.SCALE_X,value),ObjectAnimator.ofFloat(v,View.SCALE_Y,value));a.setDuration(ms);a.setInterpolator(new DecelerateInterpolator());return a;}
 static void refresh(View v){v.setTooltipText(null);if(v.getStateListAnimator()!=null||(v.isClickable()&&v instanceof android.widget.TextView&&!(v instanceof android.widget.EditText)))press(v);if(v instanceof android.view.ViewGroup){android.view.ViewGroup g=(android.view.ViewGroup)v;for(int i=0;i<g.getChildCount();i++)refresh(g.getChildAt(i));}}
 static void close(android.app.Dialog dialog){if(dialog==null)return;View v=dialog.getWindow().getDecorView();if(!enabled(v.getContext())){dialog.dismiss();return;}v.animate().alpha(0).translationY(SoundFlowUi.dp(v.getContext(),12)).setDuration(150).withEndAction(dialog::dismiss).start();}
 static void enter(View v){v.animate().cancel();if(!enabled(v.getContext())){v.setAlpha(1);v.setTranslationY(0);return;}v.setAlpha(0);v.setTranslationY(SoundFlowUi.dp(v.getContext(),10));v.animate().alpha(1).translationY(0).setDuration(200).setInterpolator(new DecelerateInterpolator()).start();}
 static void pulse(View v){if(v==null||!enabled(v.getContext()))return;v.animate().cancel();v.setScaleX(.84f);v.setScaleY(.84f);v.animate().scaleX(1).scaleY(1).setDuration(230).setInterpolator(new OvershootInterpolator(.8f)).start();}
 static void trackChange(View v,int direction){if(v==null)return;v.animate().cancel();if(!enabled(v.getContext())){v.setAlpha(1);v.setTranslationX(0);return;}v.setAlpha(.35f);v.setTranslationX(SoundFlowUi.dp(v.getContext(),22)*direction);v.animate().alpha(1).translationX(0).setDuration(260).setInterpolator(new DecelerateInterpolator()).start();}
 static String version(Context c){try{return c.getPackageManager().getPackageInfo(c.getPackageName(),0).versionName;}catch(Exception e){return "неизвестная версия";}}
}
