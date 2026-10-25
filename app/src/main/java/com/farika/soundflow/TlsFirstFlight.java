package com.farika.soundflow;
import java.io.*;
/** Reframes the first plaintext ClientHello only. No SNI substitution or TLS termination. */
final class TlsFirstFlight {
 static void send(InputStream in,OutputStream out,int delayMs)throws IOException{
  byte[] h=read(in,5);int n=u16(h,3);if((h[0]&255)!=22||h[1]!=3||n<1||n>18432)throw new IOException("Expected TLS ClientHello");byte[] b=read(in,n);
  if((b[0]&255)!=1||n<8){out.write(h);out.write(b);out.flush();return;}
  int cut=sniCut(b);if(cut<1||cut>=b.length)cut=Math.max(1,b.length/2);
  record(out,h,b,0,cut);try{if(delayMs>0)Thread.sleep(Math.min(30,delayMs));}catch(InterruptedException e){Thread.currentThread().interrupt();throw new InterruptedIOException("TLS send cancelled");}record(out,h,b,cut,b.length-cut);
 }
 private static void record(OutputStream out,byte[] h,byte[] b,int off,int n)throws IOException{out.write(new byte[]{h[0],h[1],h[2],(byte)(n>>>8),(byte)n});out.write(b,off,n);out.flush();}
 static int sniCut(byte[] b){try{int p=4+2+32;p+=1+(b[p]&255);int cipher=u16(b,p);p+=2+cipher;p+=1+(b[p]&255);int end=p+2+u16(b,p);p+=2;if(end>b.length)return -1;while(p+4<=end){int type=u16(b,p),len=u16(b,p+2);p+=4;if(p+len>end)return -1;if(type==0&&len>=5&&(b[p+2]&255)==0){int name=u16(b,p+3);if(name>1&&name<=len-5)return p+5+name/2;}p+=len;}}catch(IndexOutOfBoundsException ignored){}return -1;}
 static int u16(byte[] b,int p){return ((b[p]&255)<<8)|(b[p+1]&255);}
 static byte[] read(InputStream in,int n)throws IOException{byte[] b=new byte[n];int p=0,r;while(p<n){r=in.read(b,p,n-p);if(r<0)throw new EOFException("Incomplete TLS record");if(r>0)p+=r;}return b;}
}
