package com.farika.soundflow;
import android.content.*;
public final class CancelDownloadsReceiver extends BroadcastReceiver {
 @Override public void onReceive(Context c,Intent i){DownloadCancellation.cancel();String user=DownloadSync.owner(c);try(SyncDb db=new SyncDb(c)){for(org.json.JSONObject p:db.workPlans(user)){db.status(user,p.optString("id"),"Загрузка приостановлена. Очередь и готовые треки сохранены.");}}DownloadNotice.clear(c);AutoDownloads.retry(c);}
}
