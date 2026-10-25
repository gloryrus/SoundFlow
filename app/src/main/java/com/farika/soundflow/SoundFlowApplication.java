package com.farika.soundflow;
import android.app.Application;import java.util.UUID;
public final class SoundFlowApplication extends Application {
 static final String PROCESS=UUID.randomUUID().toString();
 @Override public void onCreate(){super.onCreate();DownloadNotice.clear(this);AppNetwork.install(this);
  // Keep the stable queue checkpoint for Android service recovery. UI launch alone never starts playback.
  try(SyncDb db=new SyncDb(this)){db.recoverInterrupted(DownloadSync.owner(this));}AutoDownloads.schedule(this);AutoDownloads.retry(this);
  new Thread(()->{new OfflineStore(this).repairKnownCopies();AutoDownloads.schedule(this);AutoDownloads.retry(this);},"SoundFlow-offline-audit").start();
  // Login, downloaded audio, download plans and partial authorized originals stay durable.
 }
}
