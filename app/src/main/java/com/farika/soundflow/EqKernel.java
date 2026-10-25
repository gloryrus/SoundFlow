package com.farika.soundflow;
/** Five peaking biquads, transposed direct form II. PCM only; no network or DRM operations. */
public final class EqKernel {
 public static final int[] HZ={60,230,910,3600,14000};private final double[][] coeff=new double[5][5];private final double[][] z1,z2;private double headroom=1;private final int channels;
 public EqKernel(int sampleRate,int channels,int[] millibels){if(sampleRate<8000||channels<1||channels>8||millibels.length!=5)throw new IllegalArgumentException();this.channels=channels;z1=new double[channels][5];z2=new double[channels][5];configure(sampleRate,millibels);}
 public void configure(int rate,int[] gains){int positive=0;for(int b=0;b<5;b++){int gain=Math.max(-1200,Math.min(1200,gains[b]));if(HZ[b]>=rate/2.0){coeff[b][0]=1;for(int k=1;k<5;k++)coeff[b][k]=0;continue;}positive=Math.max(positive,gain);double a=Math.pow(10,gain/4000.0),w=2*Math.PI*Math.min(HZ[b],rate*.45)/rate,alpha=Math.sin(w)/(2*.8),cos=Math.cos(w),a0=1+alpha/a;coeff[b][0]=(1+alpha*a)/a0;coeff[b][1]=-2*cos/a0;coeff[b][2]=(1-alpha*a)/a0;coeff[b][3]=-2*cos/a0;coeff[b][4]=(1-alpha/a)/a0;}headroom=positive>0?Math.pow(10,-(positive/100.0+3)/20):1;reset();}
 public void reset(){for(int c=0;c<channels;c++)for(int b=0;b<5;b++){z1[c][b]=0;z2[c][b]=0;}}
 public short sample(short pcm,int channel){double x=pcm*headroom;for(int b=0;b<5;b++){double[] k=coeff[b];double y=k[0]*x+z1[channel][b];z1[channel][b]=k[1]*x-k[3]*y+z2[channel][b];z2[channel][b]=k[2]*x-k[4]*y;x=y;}if(!Double.isFinite(x)){reset();return 0;}return (short)Math.max(-32768,Math.min(32767,Math.round(x)));}
}
