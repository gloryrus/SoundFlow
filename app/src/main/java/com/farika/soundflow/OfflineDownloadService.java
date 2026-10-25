package com.farika.soundflow;
import android.app.*;import android.content.*;import android.os.*;import java.io.*;
/** Compatibility entry only. Work is a JobScheduler task, never an invisible long-running FGS. */
public class OfflineDownloadService extends Service {
 public static final String EVENT="com.farika.soundflow.DOWNLOAD";
 static boolean pending(String owner,String id){return false;}
 @Override public int onStartCommand(Intent intent,int flags,int startId){if(intent==null){stopSelf();return START_NOT_STICKY;}if("CANCEL".equals(intent.getAction())){new CancelDownloadsReceiver().onReceive(this,intent);stopSelf();return START_NOT_STICKY;}String owner=DownloadSync.owner(this),id=intent.getStringExtra("playlistId");if(id==null){Library.Track track=new Library(this).find(intent.getStringExtra("uri"));if(track!=null&&!track.id().isEmpty())id="track:"+track.id();}if(id!=null&&!owner.isEmpty()){String name=intent.getStringExtra("playlistName"),kind=intent.getStringExtra("kind");try(SyncDb db=new SyncDb(this)){db.ensure(owner,id,name==null?"Офлайн SoundCloud":name,kind==null?"track":kind);db.active(owner,id,true);}SyncRequests.put(this,owner,id,intent.getBooleanExtra("refresh",true),intent.getBooleanExtra("forceRetry",false),!intent.getBooleanExtra("automatic",false));AutoDownloads.kick(this);AutoDownloads.retry(this);}stopSelf();return START_NOT_STICKY;}
 static String safeError(Exception e){String m=e.getMessage();if(e instanceof ScApi.ApiError)return "SoundCloud: HTTP "+((ScApi.ApiError)e).status+". Очередь сохранена. Проверьте вход/доступ; данные не удалены.";if(m!=null&&(m.startsWith("Офлайн-лицензия не получена")||m.startsWith("На этом устройстве нет Widevine")||m.startsWith("SoundCloud выдал только превью")||m.startsWith("Для загрузки войдите")))return m;return "Загрузка не завершена. Очередь сохранена; проверьте сеть и память. Причины — в разделе «Загрузки».";}
 @Override public IBinder onBind(Intent intent){return null;}
}
