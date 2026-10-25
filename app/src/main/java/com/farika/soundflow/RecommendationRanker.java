package com.farika.soundflow;
import java.util.*;
/** Deterministic reranker: uniqueness, familiarity quota and soft artist diversity. Not a neural model. */
public final class RecommendationRanker {
 public static final class Item {public final String id,artist;public final double score;public final boolean familiar;public Item(String i,String a,double s,boolean f){id=i;artist=a==null?"":a.toLowerCase(Locale.ROOT);score=s;familiar=f;}}
 public static List<String> rank(List<Item> input,int limit,String mode){LinkedHashMap<String,Item> unique=new LinkedHashMap<>();for(Item t:input)if((!"new_only".equals(mode)||!t.familiar)&&t.id!=null&&!t.id.isEmpty()&&(!unique.containsKey(t.id)||unique.get(t.id).score<t.score))unique.put(t.id,t);ArrayList<Item> pool=new ArrayList<>(unique.values());ArrayList<String> out=new ArrayList<>();ArrayList<String> recentArtists=new ArrayList<>();HashMap<String,Integer> artists=new HashMap<>();double knownTarget="familiar".equals(mode)?.75:"discover".equals(mode)?.1:.35;int known=0;
  while(out.size()<limit&&!pool.isEmpty()){boolean wantKnown=known<(out.size()+1)*knownTarget;Item best=null;double bestScore=-Double.MAX_VALUE;for(Item t:pool){int n=artists.getOrDefault(t.artist,0);double value=t.score+(t.familiar==wantKnown?7:0)-n*2.5-(recentArtists.contains(t.artist)?9:0);if(value>bestScore){bestScore=value;best=t;}}pool.remove(best);out.add(best.id);if(best.familiar)known++;artists.put(best.artist,artists.getOrDefault(best.artist,0)+1);recentArtists.add(best.artist);if(recentArtists.size()>3)recentArtists.remove(0);}
  return out;
 }
}
