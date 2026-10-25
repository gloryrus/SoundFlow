package com.farika.soundflow;
import android.content.*;import android.net.*;import android.os.*;import java.net.*;import java.net.Proxy;import java.io.*;import java.util.*;
/** One process-wide route, rebuilt after network changes and failed loopback connections. */
final class AppNetwork {
 private static DpiTunnel engine;private static boolean installed;
 static boolean enabled(Context c){return c.getSharedPreferences("network_route",0).getBoolean("local_dpi",false);}
 private static synchronized int port(){try{if(engine==null||!engine.alive())engine=new DpiTunnel();return engine.port();}catch(IOException e){throw new IllegalStateException("Локальный TLS-транспорт недоступен",e);}}
 static synchronized void reset(){if(engine!=null)try{engine.close();}catch(IOException ignored){}engine=null;}
 static synchronized void install(Context c){if(installed)return;installed=true;Context app=c.getApplicationContext();ProxySelector previous=ProxySelector.getDefault();ProxySelector.setDefault(new ProxySelector(){
  public List<Proxy> select(URI u){if(u==null)throw new IllegalArgumentException("URI required");if(enabled(app)&&DpiTunnel.target(u))return Collections.singletonList(new Proxy(Proxy.Type.HTTP,new InetSocketAddress("127.0.0.1",port())));return previous==null?Collections.singletonList(Proxy.NO_PROXY):previous.select(u);}
  public void connectFailed(URI u,SocketAddress address,IOException error){if(address instanceof InetSocketAddress&&((InetSocketAddress)address).getAddress()!=null&&((InetSocketAddress)address).getAddress().isLoopbackAddress())reset();}
 });
 try{ConnectivityManager manager=app.getSystemService(ConnectivityManager.class);manager.registerDefaultNetworkCallback(new ConnectivityManager.NetworkCallback(){private Network active;
  public void onAvailable(Network network){boolean changed=active!=null&&!active.equals(network);active=network;if(changed)reset();new Handler(Looper.getMainLooper()).post(()->{if(PlaybackService.instance!=null)PlaybackService.instance.networkAvailable();AutoDownloads.kick(app);});}
  public void onLost(Network network){if(network.equals(active)){active=null;reset();}}
 });}catch(RuntimeException ignored){}
 }
}
