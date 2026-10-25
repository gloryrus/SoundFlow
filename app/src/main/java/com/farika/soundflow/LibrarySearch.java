package com.farika.soundflow;
import java.util.*;import java.text.Normalizer;import org.json.*;
/** Local text matching; catalog requests are only used for uncached playlist contents. */
final class LibrarySearch {
 static String key(String s){return Normalizer.normalize(s==null?"":s,Normalizer.Form.NFKC).toLowerCase(Locale.ROOT).replace('ё','е').trim();}
 static boolean matches(Library.Track t,String query){String hay=key(t.title+" "+t.artist);for(String word:key(query).split("\\s+"))if(!hay.contains(word))return false;return true;}
 static ArrayList<Library.Track> filter(List<Library.Track> source,String query){ArrayList<Library.Track> out=new ArrayList<>();for(Library.Track t:source)if(matches(t,query))out.add(t);return out;}
 interface Source {List<JSONObject> playlists()throws Exception;List<Library.Track> tracks(JSONObject playlist)throws Exception;void check()throws Exception;}
 static ArrayList<Library.Track> all(Source source,String query)throws Exception{LinkedHashMap<String,Library.Track> found=new LinkedHashMap<>();HashSet<String> lists=new HashSet<>();for(JSONObject p:source.playlists()){source.check();if(!lists.add(p.optString("id")))continue;for(Library.Track t:source.tracks(p)){source.check();if(matches(t,query))found.putIfAbsent(t.id().isEmpty()?t.uri:t.id(),t);}}source.check();return new ArrayList<>(found.values());}
}
