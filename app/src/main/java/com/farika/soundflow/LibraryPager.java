package com.farika.soundflow;
import java.util.*;import java.io.*;import org.json.JSONObject;
/** Fetch the complete library collection. API page size is not a collection size limit. */
final class LibraryPager {
 interface Source{ScApi.Page fetch(String next)throws Exception;}
 interface Control{void check()throws Exception;void progress(int tracks,int playlists);}
 static ScApi.Page all(Source source,Control control)throws Exception{
  ScApi.Page result=new ScApi.Page();HashSet<String> pages=new HashSet<>(),tracks=new HashSet<>(),playlists=new HashSet<>();String next="";
  do{control.check();ScApi.Page part=source.fetch(next);control.check();for(Library.Track t:part.tracks)if(tracks.add(t.uri))result.tracks.add(t);for(JSONObject p:part.playlists)if(playlists.add(p.optString("id")))result.playlists.add(p);next=part.next;if(!next.isEmpty()&&!pages.add(next))throw new IOException("Повтор страницы: полный список не подтверждён");control.progress(result.tracks.size(),result.playlists.size());}while(!next.isEmpty());
  control.check();return result;
 }
}
