package com.farika.soundflow;
import android.app.*;import android.content.*;import android.os.SystemClock;
/** No notification for metadata. The notice lives only for the transfer phase. */
final class DownloadNotice implements AutoCloseable {
 static final int ID=8;private final Context context;private final String title;private boolean started,closed;private long last;private final long cancellation=DownloadCancellation.ticket();
 DownloadNotice(Context c,String title){context=c.getApplicationContext();this.title=title;}
 synchronized void transfer(String text,int percent){if(closed||!DownloadCancellation.current(cancellation))return;started=true;show(text,percent,true);}
 synchronized void progress(String text,int percent){if(!closed&&started&&DownloadCancellation.current(cancellation))show(text,percent,false);}
 private void show(String text,int percent,boolean force){long now=SystemClock.elapsedRealtime();if(!force&&now-last<700)return;last=now;NotificationManager manager=context.getSystemService(NotificationManager.class);manager.createNotificationChannel(new NotificationChannel("downloads","Офлайн-загрузки",NotificationManager.IMPORTANCE_LOW));PendingIntent cancel=PendingIntent.getBroadcast(context,81,new Intent(context,CancelDownloadsReceiver.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);PendingIntent open=PendingIntent.getActivity(context,82,new Intent(context,MainActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);Notification n=new Notification.Builder(context,"downloads").setSmallIcon(R.drawable.ic_notification).setContentTitle(title).setContentText(text).setStyle(new Notification.BigTextStyle().bigText(text)).setContentIntent(open).setOnlyAlertOnce(true).setOngoing(true).setProgress(100,Math.max(0,Math.min(100,percent)),percent<0).addAction(new Notification.Action.Builder(android.R.drawable.ic_menu_close_clear_cancel,"Приостановить загрузки",cancel).build()).build();try{manager.notify(ID,n);}catch(RuntimeException ignored){}}
 synchronized boolean started(){return started;}
 public synchronized void close(){if(closed)return;closed=true;if(started)clear(context);started=false;}
 static void clear(Context c){c.getSystemService(NotificationManager.class).cancel(ID);}
}
