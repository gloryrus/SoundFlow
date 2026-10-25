package com.farika.soundflow;
import android.content.*;import android.database.*;import android.database.sqlite.*;import org.json.*;import java.util.*;
/** Account-scoped generated recommendation metadata on disk, never a user's playlist or audio file. */
final class WaveCatalog {
 private final Context c;private final String owner;
 private static final class Store extends SQLiteOpenHelper {Store(Context c){super(c,"wave_catalog.db",null,1);}public void onCreate(SQLiteDatabase db){db.execSQL("CREATE TABLE wave (owner TEXT NOT NULL,uri TEXT NOT NULL,json TEXT NOT NULL,updated INTEGER NOT NULL,PRIMARY KEY(owner,uri))");}public void onUpgrade(SQLiteDatabase db,int a,int b){}}
 WaveCatalog(Context context){c=context.getApplicationContext();owner=PlaybackCheckpoint.owner(c);}
 void put(List<Library.Track> tracks){try(Store store=new Store(c)){SQLiteDatabase db=store.getWritableDatabase();db.beginTransaction();try{for(Library.Track t:tracks){if(!t.cloud())continue;ContentValues v=new ContentValues();v.put("owner",owner);v.put("uri",t.uri);v.put("json",Library.json(t).toString());v.put("updated",System.currentTimeMillis());db.insertWithOnConflict("wave",null,v,SQLiteDatabase.CONFLICT_REPLACE);}db.setTransactionSuccessful();}catch(JSONException e){throw new IllegalArgumentException(e);}finally{db.endTransaction();}}}
 Library.Track get(String uri){if(uri==null||!uri.startsWith("sc:"))return null;try(Store store=new Store(c);Cursor row=store.getReadableDatabase().query("wave",new String[]{"json"},"owner=? AND uri=?",new String[]{owner,uri},null,null,null)){if(row.moveToFirst())return Library.fromJson(new JSONObject(row.getString(0)));}catch(JSONException ignored){}return null;}
 ArrayList<Library.Track> recent(){ArrayList<Library.Track> out=new ArrayList<>();try(Store store=new Store(c);Cursor rows=store.getReadableDatabase().query("wave",new String[]{"json"},"owner=?",new String[]{owner},null,null,"updated DESC","300")){while(rows.moveToNext())try{out.add(Library.fromJson(new JSONObject(rows.getString(0))));}catch(JSONException ignored){}}return out;}
 void liked(String id,boolean liked){Library.Track t=get("sc:"+id);if(t!=null){t.liked=liked;put(Collections.singletonList(t));}}
}
