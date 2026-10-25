package com.farika.soundflow;
import android.content.*;import androidx.media3.common.C;import androidx.media3.common.audio.*;import java.nio.*;
/** On-device PCM EQ inside this player's Media3 audio sink. No global audio effect. */
final class AudioEqualizer {
 private final SharedPreferences prefs;private volatile Processor processor;private volatile Settings settings;
 private static final class Settings {final boolean enabled;final int[] gains;Settings(boolean e,int[] g){enabled=e;gains=g;}}
 AudioEqualizer(Context c){prefs=c.getSharedPreferences("equalizer",0);int[] gains=new int[5];for(int i=0;i<5;i++)gains[i]=Math.max(-1200,Math.min(1200,prefs.getInt(key(EqKernel.HZ[i]*1000),0)));settings=new Settings(prefs.getBoolean("enabled",false),gains);}
 static String key(int frequency){return "hz_"+frequency;}
 Processor createProcessor(){Processor p=new Processor();processor=p;return p;}
 boolean enabled(){return settings.enabled;}boolean available(){Processor p=processor;return p!=null&&p.configured;}
 String issue(){return "Начните воспроизведение. Эквалайзер работает в PCM-аудиотракте этого плеера.";}
 int[] frequencies(){int[] f=new int[5];for(int i=0;i<5;i++)f[i]=EqKernel.HZ[i]*1000;return f;}
 int[] range(){return new int[]{-1200,1200};}
 int gain(int frequency){for(int i=0;i<5;i++)if(frequency==EqKernel.HZ[i]*1000)return settings.gains[i];return 0;}
 boolean enable(boolean on){settings=new Settings(on,settings.gains.clone());prefs.edit().putBoolean("enabled",on).apply();return true;}
 boolean gain(int frequency,int value){for(int i=0;i<5;i++)if(frequency==EqKernel.HZ[i]*1000){int[] g=settings.gains.clone();g[i]=Math.max(-1200,Math.min(1200,value));settings=new Settings(settings.enabled,g);prefs.edit().putInt(key(frequency),g[i]).apply();return true;}return false;}
 void reset(){settings=new Settings(settings.enabled,new int[5]);SharedPreferences.Editor e=prefs.edit();for(int f:frequencies())e.putInt(key(f),0);e.apply();}
 void release(){processor=null;}
 final class Processor extends BaseAudioProcessor {
  volatile boolean configured;private EqKernel kernel;private Settings applied;private int rate,channels;
  protected AudioFormat onConfigure(AudioFormat f)throws UnhandledAudioFormatException{if(f.encoding!=C.ENCODING_PCM_16BIT||f.channelCount<1||f.channelCount>8||f.sampleRate<8000)throw new UnhandledAudioFormatException(f);rate=f.sampleRate;channels=f.channelCount;kernel=new EqKernel(rate,channels,settings.gains);applied=settings;configured=true;return f;}
  public void queueInput(ByteBuffer input){if(!input.hasRemaining())return;ByteBuffer out=replaceOutputBuffer(input.remaining()).order(ByteOrder.LITTLE_ENDIAN);Settings current=settings;if(!current.enabled){out.put(input);}else{if(applied!=current){kernel.configure(rate,current.gains);applied=current;}input.order(ByteOrder.LITTLE_ENDIAN);while(input.remaining()>=channels*2)for(int c=0;c<channels;c++)out.putShort(kernel.sample(input.getShort(),c));while(input.hasRemaining())out.put(input.get());}out.flip();}
  protected void onFlush(){if(kernel!=null)kernel.reset();}
  protected void onReset(){configured=false;kernel=null;applied=null;}
 }
}
