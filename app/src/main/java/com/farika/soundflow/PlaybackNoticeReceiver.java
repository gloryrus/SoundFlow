package com.farika.soundflow;
import android.content.*;import android.app.NotificationManager;
public final class PlaybackNoticeReceiver extends BroadcastReceiver {
 public void onReceive(Context c,Intent i){if(i==null||!"CLOSE".equals(i.getAction())||!PlaybackCheckpoint.owner(c).equals(i.getStringExtra("owner")))return;PlaybackService s=PlaybackService.instance;if(s!=null)s.stopForLocalChange();else{PlaybackCheckpoint.clear(c);c.getSystemService(NotificationManager.class).cancel(PlaybackNotice.ID);}c.sendBroadcast(new Intent(PlaybackService.EVENT).setPackage(c.getPackageName()));}
}
