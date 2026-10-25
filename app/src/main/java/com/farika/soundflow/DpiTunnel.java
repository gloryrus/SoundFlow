package com.farika.soundflow;
import java.io.*;import java.net.*;import java.nio.charset.StandardCharsets;import java.util.*;import java.util.concurrent.*;
/** App-local CONNECT relay. TLS stays end-to-end; only ClientHello record boundaries change. */
final class DpiTunnel implements Closeable {
 interface Dialer {Socket open(String host,int port)throws IOException;}
 private final ServerSocket listener;private final Dialer dialer;private final Semaphore permits=new Semaphore(12);private final Set<Socket> sockets=Collections.synchronizedSet(new HashSet<Socket>());private volatile boolean closed;
 DpiTunnel()throws IOException{this((host,port)->{Socket s=new Socket(Proxy.NO_PROXY);try{s.connect(new InetSocketAddress(host,port),20000);return s;}catch(IOException e){s.close();throw e;}});}
 DpiTunnel(Dialer dialer)throws IOException{this.dialer=dialer;listener=new ServerSocket();listener.bind(new InetSocketAddress(InetAddress.getByName("127.0.0.1"),0),12);thread(()->accept());}
 boolean alive(){return !closed&&!listener.isClosed();}
 int port(){return listener.getLocalPort();}
 static boolean target(URI u){if(u==null||!"https".equalsIgnoreCase(u.getScheme())||u.getUserInfo()!=null||(u.getPort()!=-1&&u.getPort()!=443))return false;String h=u.getHost();if(h==null)return false;h=h.toLowerCase(Locale.ROOT);return h.equals("soundcloud.com")||h.endsWith(".soundcloud.com")||h.equals("sndcdn.com")||h.endsWith(".sndcdn.com")||(h.equals("playback.media-streaming.soundcloud.cloud")||h.equals("license.media-streaming.soundcloud.cloud"));}
 private static void thread(Runnable r){Thread t=new Thread(r,"SoundFlow-TLS-transport");t.setDaemon(true);t.start();}
 private void accept(){while(!closed){try{Socket client=listener.accept();if(!permits.tryAcquire()){client.close();continue;}sockets.add(client);thread(()->handle(client));}catch(IOException e){if(!closed)closeQuietly();}}}
 private void handle(Socket client){Socket upstream=null;try{
  client.setSoTimeout(20000);client.setTcpNoDelay(true);InputStream input=client.getInputStream();ByteArrayOutputStream header=new ByteArrayOutputStream();int last=0;while(header.size()<8192){int b=input.read();if(b<0)throw new EOFException();header.write(b);last=(last<<8)|b;if(last==0x0d0a0d0a)break;}if(last!=0x0d0a0d0a)throw new IOException("CONNECT header too long");String line=new String(header.toByteArray(),StandardCharsets.US_ASCII).split("\r\n",2)[0];String[] parts=line.split(" ");if(parts.length!=3||!parts[0].equals("CONNECT")||!parts[1].endsWith(":443"))throw new IOException("Only HTTPS CONNECT supported");URI u=URI.create("https://"+parts[1]);if(!target(u))throw new IOException("Destination refused");
  upstream=dialer.open(u.getHost(),443);sockets.add(upstream);upstream.setSoTimeout(90000);upstream.setTcpNoDelay(true);client.getOutputStream().write("HTTP/1.1 200 Connection Established\r\n\r\n".getBytes(StandardCharsets.US_ASCII));client.getOutputStream().flush();
  Socket remote=upstream;thread(()->{try{pump(remote.getInputStream(),client.getOutputStream());}catch(IOException ignored){}finally{closeSocket(client);closeSocket(remote);}});
  TlsFirstFlight.send(input,remote.getOutputStream(),8);client.setSoTimeout(90000);pump(input,remote.getOutputStream());
 }catch(Exception ignored){/* No URL, token or TLS content logs. Failed connections never fall back direct. */}finally{closeSocket(client);closeSocket(upstream);permits.release();}}
 private static void pump(InputStream in,OutputStream out)throws IOException{byte[] b=new byte[32768];int n;while((n=in.read(b))!=-1){out.write(b,0,n);out.flush();}}
 private void closeSocket(Socket s){if(s!=null){sockets.remove(s);try{s.close();}catch(IOException ignored){}}}
 private void closeQuietly(){try{close();}catch(IOException ignored){}}
 public void close()throws IOException{closed=true;listener.close();Socket[] copy;synchronized(sockets){copy=sockets.toArray(new Socket[0]);}for(Socket s:copy)closeSocket(s);}
}
