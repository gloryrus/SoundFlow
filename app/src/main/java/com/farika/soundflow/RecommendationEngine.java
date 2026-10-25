package com.farika.soundflow;
import android.content.*;
import org.json.*;
import java.io.*;
import java.util.*;
import java.util.function.BooleanSupplier;
/** Multi-seed direct SoundCloud candidates + local taste reranking; no external recommender or tracking. */
public final class RecommendationEngine {
 private final Context c;private final ScApi api;private final TasteStore taste;
 public RecommendationEngine(Context context){this(context,new ScApi(context));}
 RecommendationEngine(Context context,ScApi source){c=context.getApplicationContext();api=source;taste=new TasteStore(c);}
 protected ScApi.Page related(String id,String next)throws Exception{return api.related(id,next);}
 private void check(BooleanSupplier cancelled)throws IOException{if(cancelled.getAsBoolean()||Thread.currentThread().isInterrupted())throw new InterruptedIOException();}
 private static boolean allowed(Library.Track t){return !t.id().isEmpty()&&!t.preview()&&(t.remote==null||t.remote.optBoolean("streamable",true)&&!"BLOCK".equals(t.remote.optString("policy")));}
 public ScApi.Page generate(String user,String mode,BooleanSupplier cancelled)throws Exception{
  return generate(user,mode,Collections.emptySet(),false,cancelled);
 }
 public ScApi.Page generate(String user,String mode,Set<String> excluded,boolean continuation,BooleanSupplier cancelled)throws Exception{
  ArrayList<Library.Track> likes=new ArrayList<>();boolean liveLikes=false;try{likes.addAll(FavoriteLoader.load(next->api.likes(user,next),new FavoriteLoader.Control(){public void check()throws Exception{RecommendationEngine.this.check(cancelled);}public void progress(int n){}}));liveLikes=true;}catch(Exception e){likes.addAll(new PlaylistCache(c).tracks("likes"));for(Library.Track t:new Library(c).tracks)if(t.liked&&!t.id().isEmpty())likes.add(t);}check(cancelled);
  LinkedHashMap<String,Library.Track> seedsMap=new LinkedHashMap<>();for(Library.Track t:likes)if(allowed(t)&&!taste.hidden(t.id()))seedsMap.put(t.id(),t);
  ArrayList<Library.Track> known=new ArrayList<>(new Library(c).tracks);known.addAll(new WaveCatalog(c).recent());for(Library.Track t:known)if(allowed(t)&&taste.known(t.id())&&!taste.hidden(t.id()))seedsMap.putIfAbsent(t.id(),t);
  ArrayList<Library.Track> seeds=new ArrayList<>(seedsMap.values());seeds.sort((a,b)->Double.compare(taste.score(b.id()),taste.score(a.id())));ScApi.Page out=new ScApi.Page();if(seeds.isEmpty()){out.note="Нужны лайки или прослушивания полных треков, чтобы собрать вашу волну";return out;}
  SharedPreferences settings=c.getSharedPreferences("recommendation_settings",0);int round=settings.getInt(user+":round",0);settings.edit().putInt(user+":round",round+1).apply();Collections.rotate(seeds,-Math.floorMod(round*3,seeds.size()));
  HashSet<String> familiar=new HashSet<>();for(Library.Track t:likes)familiar.add(t.id());LinkedHashMap<String,Library.Track> candidates=new LinkedHashMap<>();HashMap<String,Double> affinity=new HashMap<>();int fetched=0;Exception last=null;
  for(int i=0;i<Math.min(6,seeds.size());i++){check(cancelled);Library.Track seed=seeds.get(i);try{SharedPreferences cursors=c.getSharedPreferences("wave_cursors",0);String key=user+":"+seed.id();String cursor=continuation?cursors.getString(key,""):"";ScApi.Page related;try{related=related(seed.id(),cursor);}catch(ScApi.ApiError broken){if(cursor.isEmpty()||broken.status==401||broken.status==429)throw broken;related=related(seed.id(),"");}check(cancelled);if(cursors.getAll().size()>128)cursors.edit().clear().apply();cursors.edit().putString(key,related.next).apply();fetched++;for(int j=0;j<related.tracks.size();j++){Library.Track t=related.tracks.get(j);if(!allowed(t)||taste.hidden(t.id()))continue;t.liked=familiar.contains(t.id());if(!candidates.containsKey(t.id())){t.reason="По вашим вкусам · рядом с "+seed.artist;candidates.put(t.id(),t);}affinity.put(t.id(),affinity.getOrDefault(t.id(),0.0)+3.0/(1+j/10.0));}}catch(ScApi.ApiError e){if(e.status==401||e.status==429)throw e;last=e;}catch(Exception e){last=e;}}
  check(cancelled);for(Library.Track t:seedsMap.values())if(allowed(t)&&!taste.hidden(t.id())){if(!candidates.containsKey(t.id())){t.reason=t.liked?"Из вашего любимого":"Вы слушали с интересом";candidates.put(t.id(),t);}}
  for(String uri:excluded)if(uri.startsWith("sc:"))candidates.remove(uri.substring(3));
  ArrayList<RecommendationRanker.Item> input=new ArrayList<>();long now=System.currentTimeMillis();for(Library.Track t:candidates.values()){JSONObject stat=taste.stats(t.id());long ago=now-stat.optLong("last");double recent=stat.optLong("last")>0&&ago<4*3600000L?5:0;double score=affinity.getOrDefault(t.id(),0.0)+taste.score(t.id())-recent;input.add(new RecommendationRanker.Item(t.id(),t.artist,score,familiar.contains(t.id())||taste.seen(t.id())));}
  for(String id:RecommendationRanker.rank(input,continuation?30:60,c.getSharedPreferences("recommendation_settings",0).getBoolean(user+":familiar",true)?mode:"new_only"))out.tracks.add(candidates.get(id));out.note=fetched>0?"Персональная смесь · источников: "+fetched+" · учитываются прослушивания и пропуски":"По сохранённой библиотеке · рекомендации из сети недоступны";return out;
 }
}
