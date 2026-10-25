package com.farika.soundflow;
import java.util.*;import java.io.*;
/** Complete pagination; no first-page/100-track cap and no partial-success return. */
public final class FavoriteLoader {
 public interface Source {ScApi.Page fetch(String next)throws Exception;}
 public interface Control {void check()throws Exception;void progress(int loaded);}
 public static ArrayList<Library.Track> load(Source source,Control control)throws Exception{ArrayList<Library.Track> out=new ArrayList<>();String next="";HashSet<String> pages=new HashSet<>(),ids=new HashSet<>();do{control.check();ScApi.Page page=source.fetch(next);for(Library.Track t:page.tracks)if(ids.add(t.id()))out.add(t);next=page.next;if(!next.isEmpty()&&!pages.add(next))throw new IOException("Повтор страницы: полный список не подтверждён");control.progress(out.size());}while(!next.isEmpty());control.check();return out;}
}
