package com.farika.soundflow;

import android.app.*;
import android.content.*;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.Rating;
import android.media.MediaMetadata;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.os.*;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import java.util.*;
import java.util.concurrent.*;

public class PlaybackService extends Service {
 public static final String EVENT="com.farika.soundflow.STATE";public static PlaybackService instance;
 public void resetTaste(){audibleMs=0;lastTick=SystemClock.elapsedRealtime();}
 private long audibleMs=0,lastTick=0;private boolean tasteEligible=false;private TasteStore listeningTaste;private String tasteId="";private long tasteDuration=0;private final Runnable listenTick=new Runnable(){public void run(){long now=SystemClock.elapsedRealtime();if(lastTick>0&&tasteEligible&&player!=null&&player.isPlaying())audibleMs+=Math.min(2000,now-lastTick);lastTick=now;if((intent.wanted||intent.suspended||loading)&&now-lastCheckpoint>=5000)checkpoint();if(intent.wanted||intent.suspended||loading)handler.postDelayed(this,1000);}};
 private void finishListening(boolean skipped){if(tasteEligible&&listeningTaste!=null&&!tasteId.isEmpty())listeningTaste.record(tasteId,audibleMs,tasteDuration,skipped);audibleMs=0;tasteEligible=false;}
 private AudioEqualizer equalizer;public AudioEqualizer equalizer(){return equalizer;}
 private String offlineUri="";private long offlineExpected;private boolean offlineChecked;private OfflineStore.Open offlineOpen;public String notice="";private long sourceDuration=0;private ExoPlayer player;private MediaSession session;private AudioManager audio;private AudioFocusRequest focus;
 private final FocusIntent intent=new FocusIntent();private boolean focusHeld=false;private long pendingPosition=0,lastCheckpoint=0;private String playbackOwner="";private boolean stopped=false,prepared=false,loading=false,localEnded=false;private ArrayList<String> queue=new ArrayList<>();private int index=0,generation=0;
 public boolean shuffle=false,repeat=false,repeatPlaylist=false,playlistContext=false;private int networkAttempts=0;private boolean waitingForNetwork=false;public String current="",error="";private Library.Track activeTrack;
 private final Handler handler=new Handler(Looper.getMainLooper());private final ExecutorService resolver=Executors.newSingleThreadExecutor();
 private boolean wave=false,waveBusy=false,waveWaiting=false;private volatile int waveEpoch=0;private long waveRetryAfter=0;private final LinkedHashSet<String> waveRecent=new LinkedHashSet<>();private final ExecutorService waveWorker=Executors.newSingleThreadExecutor();
 private final Runnable waveRetry=()->{if(wave&&!stopped&&intent.wanted)refillWave();};
 private final BroadcastReceiver noisy=new BroadcastReceiver(){public void onReceive(Context c,Intent i){pause();}};
 @Override public void onCreate(){super.onCreate();equalizer=new AudioEqualizer(this);handler.postDelayed(listenTick,1000);androidx.media3.common.util.Log.setLogLevel(androidx.media3.common.util.Log.LOG_LEVEL_OFF);instance=this;playbackOwner=PlaybackCheckpoint.owner(this);audio=(AudioManager)getSystemService(AUDIO_SERVICE);
  focus=new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()).setAcceptsDelayedFocusGain(true).setOnAudioFocusChangeListener(this::focusChanged,handler).build();
  getSystemService(NotificationManager.class).createNotificationChannel(new NotificationChannel("playback","Воспроизведение",NotificationManager.IMPORTANCE_LOW));
  session=new MediaSession(this,"SoundFlow");session.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS|MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);session.setCallback(new MediaSession.Callback(){public void onPlay(){resume();}public void onPause(){pause();}public void onSkipToNext(){next();}public void onSkipToPrevious(){previous();}public void onSeekTo(long p){seek((int)p);}public void onStop(){pause();}public void onCustomAction(String action,Bundle extras){if(PlaybackNotice.LIKE_ACTION.equals(action)&&activeTrack!=null)NotificationLikeService.request(PlaybackService.this,activeTrack,!activeTrack.liked);}public void onSetRating(Rating rating){if(rating!=null&&rating.isRated()&&rating.getRatingStyle()==Rating.RATING_HEART&&activeTrack!=null)NotificationLikeService.request(PlaybackService.this,activeTrack,rating.hasHeart());}});session.setRatingType(Rating.RATING_HEART);session.setActive(true);
  if(Build.VERSION.SDK_INT>=33)registerReceiver(noisy,new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY),Context.RECEIVER_NOT_EXPORTED);else registerReceiver(noisy,new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));
 }
 void focusChanged(int change){
  if(stopped)return;
  if(change==AudioManager.AUDIOFOCUS_GAIN){focusHeld=intent.wanted;boolean again=intent.gain();if(player!=null)player.setVolume(1);if(again&&prepared&&player!=null){player.play();}checkpoint();publish();}
  else if(change==AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK){if(player!=null)player.setVolume(.2f);}
  else if(change==AudioManager.AUDIOFOCUS_LOSS_TRANSIENT){focusHeld=false;intent.transientLoss();if(player!=null)player.pause();checkpoint();publish();/* Keep foreground service alive throughout a temporary interruption. */}
  else if(change==AudioManager.AUDIOFOCUS_LOSS){focusHeld=false;pause();}
 }
 public void checkpoint(){if(!stopped&&LocalAudioStore.isLocal(current))localAudioStore().position(current,localEnded?0:position(),duration());if(playbackOwner.equals(PlaybackCheckpoint.owner(this))&&!stopped&&!current.isEmpty()&&!queue.isEmpty()){PlaybackCheckpoint.save(this,queue,index,position(),shuffle,repeat,intent.wanted,intent.suspended,playlistContext,repeatPlaylist,wave);lastCheckpoint=SystemClock.elapsedRealtime();}}
 private boolean restore(boolean play){PlaybackCheckpoint.Snapshot s=PlaybackCheckpoint.read(this);if(s==null)return false;wave=s.wave;waveRecent.clear();waveEpoch++;waveBusy=false;waveWaiting=false;queue=s.queue;index=s.index;shuffle=s.shuffle;repeat=s.repeat;playlistContext=s.playlistContext;repeatPlaylist=s.repeatPlaylist;pendingPosition=s.position;load(play);return true;}
 @Override public int onStartCommand(Intent i,int flags,int id){
  if(i!=null&&"DISMISS".equals(i.getAction())){stopForLocalChange();return START_NOT_STICKY;}
  startForeground(7,notification());String a=i==null?"SYSTEM_RESTORE":i.getAction();
  if("PLAY".equals(a)){waveEpoch++;waveBusy=false;waveWaiting=false;waveRetryAfter=0;waveRecent.clear();handler.removeCallbacks(waveRetry);wave=i.getBooleanExtra("wave",false);networkAttempts=0;waitingForNetwork=false;playlistContext=i.getBooleanExtra("playlistContext",false);repeatPlaylist=false;checkpoint();ArrayList<String> q=i.getStringArrayListExtra("queue");if(q!=null)queue=q;index=Math.max(0,queue.indexOf(i.getStringExtra("uri")));pendingPosition=0;load();}
  else if("SYSTEM_RESTORE".equals(a)||"RESTORE".equals(a)||"RESTORE_PAUSED".equals(a)){PlaybackCheckpoint.Snapshot saved=PlaybackCheckpoint.read(this);boolean restart="SYSTEM_RESTORE".equals(a)&&saved!=null&&RestartPolicy.resume(saved.wanted,saved.suspended,getSharedPreferences("playback_settings",0).getBoolean("system_resume",true),saved.savedAt,System.currentTimeMillis());if(!restore("RESTORE".equals(a)||restart)){stopSelf();return START_NOT_STICKY;}}
  else{if(queue.isEmpty()&&!restore(false)){stopSelf();return START_NOT_STICKY;}if("STOP".equals(a))pause();else if("TOGGLE".equals(a))toggle();else if("NEXT".equals(a))next();else if("PREV".equals(a))previous();}
  return START_STICKY;
 }
 @Override public void onTaskRemoved(Intent rootIntent){checkpoint();super.onTaskRemoved(rootIntent);}
 private void releasePlayer(){if(equalizer!=null)equalizer.release();if(player!=null){player.release();player=null;}if(offlineOpen!=null){offlineOpen.close();offlineOpen=null;}}
 public void stopForLocalChange(){wave=false;waveEpoch++;waveBusy=false;waveWaiting=false;handler.removeCallbacks(waveRetry);intent.pause();checkpoint();stopped=true;finishListening(false);++generation;intent.wanted=false;loading=false;prepared=false;releasePlayer();current="";queue.clear();PlaybackCheckpoint.clear(this);if(session!=null){session.setPlaybackState(new PlaybackState.Builder().setState(PlaybackState.STATE_STOPPED,0,0).build());session.setActive(false);}stopForeground(STOP_FOREGROUND_REMOVE);getSystemService(NotificationManager.class).cancel(7);sendBroadcast(new Intent(EVENT).setPackage(getPackageName()));stopSelf();}
 private void load(){load(true);}
 private void load(boolean play){
    if(queue.isEmpty())return;handler.removeCallbacks(listenTick);handler.post(listenTick);playbackOwner=PlaybackCheckpoint.owner(this);finishListening(false);current=queue.get(index);activeTrack=new Library(this).find(current);localEnded=false;if(LocalAudioStore.isLocal(current)&&activeTrack!=null)pendingPosition=activeTrack.savedPosition;error="";notice="";sourceDuration=0;prepared=false;loading=true;offlineUri="";offlineChecked=false;intent.wanted=play;int ticket=++generation;releasePlayer();checkpoint();publish();if(wave){waveRecent.add(current);while(waveRecent.size()>240)waveRecent.remove(waveRecent.iterator().next());refillWave();}Library.Track track=activeTrack;if(track==null){error="Трек отсутствует в очереди";fail();return;}
  listeningTaste=new TasteStore(this);tasteId=track.id();tasteDuration=track.duration;tasteEligible=!track.preview();lastTick=SystemClock.elapsedRealtime();
  Library.Track saved=track.cloud()?new OfflineStore(this).bySource(track.id()):null;
  if(OfflineStore.isOffline(current)||saved!=null){
   final String uri=saved==null?current:saved.uri;offlineUri=uri;offlineExpected=Math.max(OfflineQuality.expected(track),saved==null?0:OfflineQuality.expected(saved));resolver.execute(()->{try{OfflineStore.Open open=new OfflineStore(this).open(uri);handler.post(()->{if(ticket!=generation){open.close();return;}offlineOpen=open;notice=open.drm?"Офлайн · лицензия Widevine":open.url.startsWith("file:")?"На устройстве · оригинал":"Офлайн · кэш потока SoundCloud";try{prepare(open.url,ticket,open.hls,open.data,open.drm?DrmBridge.offline(open.keySetId):null);}catch(Exception e){error=DrmBridge.publicError(e);fail();}});}catch(Exception e){handler.post(()->{if(ticket==generation){error="Офлайн-запись или системная лицензия недоступны. Загрузите трек заново.";fail();}});}});return;
  }
  if(LocalAudioStore.isLocal(current)){final String selected=current;resolver.execute(()->{LocalAudioStore store=localAudioStore();LocalAudioStore.Entry entry=store.get(selected);LocalAudioStore.Access access=entry==null?LocalAudioStore.Access.MISSING:store.probe(entry);if(entry!=null&&access==LocalAudioStore.Access.MISSING)store.remove(selected);handler.post(()->{if(ticket!=generation)return;if(entry==null||access==LocalAudioStore.Access.MISSING){forgetLocal(selected);return;}if(access!=LocalAudioStore.Access.AVAILABLE){error="Файл временно недоступен или доступ отозван. Верните носитель либо выберите файл заново в «Своё».";fail();return;}notice="На устройстве · Своё";try{prepare(entry.document,ticket,false,new DefaultDataSource.Factory(this),null);}catch(Exception e){localError(ticket);}});});return;}
  if(!track.cloud()){prepare(current,ticket,false,new DefaultDataSource.Factory(this),null);return;}
  resolver.execute(()->{try{ScApi.Stream stream=playable(track);handler.post(()->{if(ticket!=generation)return;sourceDuration=stream.duration;tasteDuration=stream.duration;tasteEligible=!stream.preview;notice=stream.preview?stream.note:stream.drm()?"Widevine · лицензия SoundCloud":"Прямой поток SoundCloud";try{prepare(stream.url,ticket,stream.hls,OfflineStore.network(this),stream.drm()?DrmBridge.streaming(stream.licenseToken):null);}catch(Exception e){error=DrmBridge.publicError(e);fail();}});}catch(Exception e){handler.post(()->{if(ticket==generation){error="SoundCloud не выдал доступный поток. Проверьте сеть, аккаунт и доступ к треку.";if(!retryNetwork(e,ticket))fail();}});}});
 }
 protected ScApi.Stream playable(Library.Track track)throws Exception{return new ScApi(this).playable(track);}
 protected LocalAudioStore localAudioStore(){return new LocalAudioStore(this);}
 protected void prepare(String uri,int ticket,boolean hls,androidx.media3.datasource.DataSource.Factory data,androidx.media3.exoplayer.drm.DefaultDrmSessionManager drm){if(ticket!=generation)return;
  DefaultMediaSourceFactory source=new DefaultMediaSourceFactory(data);if(drm!=null)source.setDrmSessionManagerProvider(item->drm);
  final AudioEqualizer.Processor eqProcessor=equalizer.createProcessor();androidx.media3.exoplayer.DefaultRenderersFactory renderers=new androidx.media3.exoplayer.DefaultRenderersFactory(this){@Override protected androidx.media3.exoplayer.audio.AudioSink buildAudioSink(Context c,boolean floating,boolean params){return new androidx.media3.exoplayer.audio.DefaultAudioSink.Builder(c).setEnableFloatOutput(false).setAudioProcessors(new androidx.media3.common.audio.AudioProcessor[]{eqProcessor}).build();}};player=new ExoPlayer.Builder(this,renderers).setMediaSourceFactory(source).setLoadControl(PlaybackMemory.control()).build();player.setWakeMode(C.WAKE_MODE_NETWORK);
  player.setAudioAttributes(new androidx.media3.common.AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(),false);
  player.addListener(new Player.Listener(){
   @Override public void onPlaybackStateChanged(int state){if(ticket!=generation)return;
    if(state==Player.STATE_READY){if(!offlineChecked&&!offlineUri.isEmpty()&&player.getDuration()>0){offlineChecked=true;final long actual=player.getDuration(),expected=offlineExpected;final String copyUri=offlineUri;if(OfflineQuality.shortCopy(actual,expected))resolver.execute(()->{try{OfflineStore store=new OfflineStore(PlaybackService.this);store.observedDuration(copyUri,actual,expected);AutoDownloads.kick(PlaybackService.this);AutoDownloads.retry(PlaybackService.this);}catch(Exception ignored){}});}networkAttempts=0;waitingForNetwork=false;boolean initial=!prepared;prepared=true;loading=false;if(initial&&intent.canPlay())startIfFocused();else publish();checkpoint();if(!intent.wanted){}}
    else if(state==Player.STATE_BUFFERING){loading=true;publish();}
    else if(state==Player.STATE_ENDED){loading=false;localEnded=LocalAudioStore.isLocal(current);if(localEnded)localAudioStore().position(current,0,duration());if(repeat){seek(0);resume();}else if(wave||index+1<queue.size()||shuffle||RepeatPolicy.wraps(playlistContext,repeatPlaylist))advance(false);else{pause();seek(0);}}
   }
   @Override public void onPlayerError(PlaybackException e){if(ticket!=generation)return;if(LocalAudioStore.isLocal(current)){localError(ticket);return;}error=drm!=null?DrmBridge.publicError(e)+" ("+e.getErrorCodeName()+")":"Не удалось открыть поток ("+e.getErrorCodeName()+").";handler.post(()->{if(ticket==generation&&!retryNetwork(e,ticket))fail();});}
   @Override public void onIsPlayingChanged(boolean value){if(ticket==generation)publish();}
  });
  player.setMediaItem(DrmBridge.item(uri,hls,drm!=null),pendingPosition);player.prepare();publish();
 }
 private boolean retryNetwork(Throwable error,int ticket){
  if(ticket!=generation||!intent.wanted||OfflineStore.isOffline(current))return false;Library.Track selected=activeTrack;if(selected==null||!selected.cloud())return false;
  boolean transientError=DownloadFault.of(error).retry;
  if(error instanceof PlaybackException){int code=((PlaybackException)error).errorCode;transientError=code==PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED||code==PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT||code==PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS||code==PlaybackException.ERROR_CODE_IO_UNSPECIFIED;}
  if(!transientError||networkAttempts>=6)return false;
  pendingPosition=position();networkAttempts++;waitingForNetwork=true;prepared=false;loading=true;releasePlayer();this.error="";notice="Восстанавливаем соединение · "+networkAttempts;publish();
  long delay=Math.min(60000,1000L*(1L<<Math.min(6,networkAttempts-1)));
  handler.postDelayed(()->{if(ticket==generation&&waitingForNetwork&&intent.wanted){waitingForNetwork=false;load(true);}},delay);return true;
 }
 public void networkAvailable(){if(wave&&waveWaiting&&intent.wanted){waveRetryAfter=0;refillWave();}if(waitingForNetwork&&intent.wanted){waitingForNetwork=false;networkAttempts=0;load(true);}}
 void forgetLocal(String uri){if(current.equals(uri)){stopForLocalChange();return;}int old=queue.indexOf(uri);if(old>=0){while(queue.remove(uri)){}index=Math.max(0,queue.indexOf(current));checkpoint();}}
 private void localError(int ticket){String selected=current;resolver.execute(()->{LocalAudioStore store=localAudioStore();LocalAudioStore.Entry entry=store.get(selected);LocalAudioStore.Access access=entry==null?LocalAudioStore.Access.MISSING:store.probe(entry);if(entry!=null&&access==LocalAudioStore.Access.MISSING)store.remove(selected);handler.post(()->{if(ticket!=generation)return;if(access==LocalAudioStore.Access.MISSING){forgetLocal(selected);return;}error="Не удалось прочитать локальный файл. Проверьте доступ, носитель и формат аудио.";fail();});});}
 private void fail(){pendingPosition=position();tasteEligible=false;prepared=false;loading=false;intent.wanted=false;checkpoint();releasePlayer();if(error==null||error.isEmpty())error="Поток недоступен";publish();}
 public boolean isPlaying(){return intent.canPlay()&&(loading||prepared&&player!=null&&player.getPlayWhenReady()&&player.getPlaybackState()!=Player.STATE_ENDED);}
 public boolean isLoading(){return loading;}
 public int position(){return player==null?(int)Math.max(0,Math.min(Integer.MAX_VALUE,pendingPosition)):(int)Math.max(0,Math.min(Integer.MAX_VALUE,player.getCurrentPosition()));}
 public int duration(){long d=player==null?C.TIME_UNSET:player.getDuration();if(d==C.TIME_UNSET||d<=0){Library.Track t=activeTrack;d=sourceDuration>0?sourceDuration:t==null?0:t.duration;}return (int)Math.max(0,Math.min(Integer.MAX_VALUE,d));}
 public void toggle(){if(isPlaying())pause();else resume();}
 private void startIfFocused(){
  if(!intent.canPlay())return;startForeground(7,notification());
  if(!focusHeld){int result=audio.requestAudioFocus(focus);focusHeld=result==AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
   if(result==AudioManager.AUDIOFOCUS_REQUEST_DELAYED){intent.transientLoss();notice="Продолжим после освобождения аудио";checkpoint();publish();return;}
   if(!focusHeld){intent.pause();error="Аудио занято другим приложением. Нажмите ▶, чтобы продолжить.";checkpoint();publish();return;}}
  if(prepared&&player!=null){error="";player.setVolume(1);player.play();}publish();
 }
 public void resume(){if(waveWaiting){intent.play();waveRetryAfter=0;if(index+1<queue.size()){waveWaiting=false;advance(false);}else refillWave();return;}localEnded=false;handler.removeCallbacks(listenTick);handler.post(listenTick);if(intent.suspended){intent.suspended=false;focusHeld=false;}intent.play();if(!prepared&&!loading){if(!queue.isEmpty())load();return;}startIfFocused();checkpoint();}
 public void pause(){handler.removeCallbacks(listenTick);if(waitingForNetwork){waitingForNetwork=false;loading=false;}intent.pause();if(player!=null)player.pause();if(audio!=null&&focus!=null)audio.abandonAudioFocusRequest(focus);focusHeld=false;checkpoint();publish();}
 void likeConfirmed(String owner,String id,boolean liked){if(playbackOwner.equals(owner)&&activeTrack!=null&&id.equals(activeTrack.id())){activeTrack.liked=liked;publish();}}
 public void cycleRepeat(){int mode=RepeatPolicy.next(repeat?1:repeatPlaylist?2:0,playlistContext);repeat=mode==1;repeatPlaylist=mode==2;checkpoint();publish();}
 public void next(){networkAttempts=0;waitingForNetwork=false;advance(true);}
 private void advance(boolean manual){if(queue.isEmpty())return;if(wave&&index+1>=queue.size()){waveWaiting=true;loading=true;intent.play();notice="Продолжаем вашу волну…";refillWave();publish();return;}checkpoint();pendingPosition=0;finishListening(manual);index=!wave&&shuffle&&queue.size()>1?(index+1+new Random().nextInt(queue.size()-1))%queue.size():(index+1)%queue.size();load();}
 public void previous(){if(position()>3000){seek(0);return;}skipPrevious();}
 public void skipPrevious(){networkAttempts=0;waitingForNetwork=false;if(queue.isEmpty())return;checkpoint();pendingPosition=0;finishListening(true);index=(index+queue.size()-1)%queue.size();load();}
 public void seek(int p){if(p>0)localEnded=false;if(player!=null&&prepared){player.seekTo(p);pendingPosition=Math.max(0,p);checkpoint();publish();}}
 private Notification notification(){return PlaybackNotice.playing(this,activeTrack,isPlaying(),intent.wanted,loading,error,session==null?null:session.getSessionToken());}
 private void publish(){if(stopped||session==null)return;Library.Track t=activeTrack;if(t!=null)session.setMetadata(PlaybackNotice.metadata(t,duration()));session.setPlaybackState(PlaybackNotice.state(t,isPlaying(),loading,intent.wanted,position()));getSystemService(NotificationManager.class).notify(7,notification());sendBroadcast(new Intent(EVENT).setPackage(getPackageName()));}
 @Override public void onDestroy(){checkpoint();stopped=true;waveEpoch++;waveWorker.shutdownNow();finishListening(false);generation++;resolver.shutdownNow();handler.removeCallbacksAndMessages(null);releasePlayer();if(session!=null)session.release();audio.abandonAudioFocusRequest(focus);unregisterReceiver(noisy);instance=null;stopForeground(STOP_FOREGROUND_REMOVE);getSystemService(NotificationManager.class).cancel(7);sendBroadcast(new Intent(EVENT).setPackage(getPackageName()));super.onDestroy();}
 @Override public IBinder onBind(Intent i){return null;}

 /** A bounded moving window, not a finite playlist. Independent of the Activity lifecycle. */
 protected ScApi.Page waveBatch(String owner,Set<String> excluded,java.util.function.BooleanSupplier cancelled)throws Exception{String mode=getSharedPreferences("recommendation_settings",0).getString(owner+":mode","mix");return new RecommendationEngine(this).generate(owner,mode,excluded,true,cancelled);}
 private void refillWave(){
  if(!wave||stopped||waveBusy||queue.size()-index-1>5)return;
  if(SystemClock.elapsedRealtime()<waveRetryAfter){scheduleWaveRetry();return;}
  waveBusy=true;final int epoch=waveEpoch;final String owner=playbackOwner;final HashSet<String> strict=new HashSet<>(queue);final HashSet<String> excluded=new HashSet<>(strict);excluded.addAll(waveRecent);
  waveWorker.execute(()->{try{java.util.function.BooleanSupplier cancelled=()->epoch!=waveEpoch||Thread.currentThread().isInterrupted()||!owner.equals(PlaybackCheckpoint.owner(this));ScApi.Page initial=waveBatch(owner,excluded,cancelled);final ScApi.Page batch=initial.tracks.isEmpty()&&excluded.size()>strict.size()&&!cancelled.getAsBoolean()?waveBatch(owner,strict,cancelled):initial;handler.post(()->{if(epoch!=waveEpoch||!wave||stopped||!owner.equals(PlaybackCheckpoint.owner(this)))return;waveBusy=false;ArrayList<Library.Track> fresh=new ArrayList<>();HashSet<String> present=new HashSet<>(queue);int added=0;for(Library.Track t:batch.tracks)if(t.cloud()&&!t.preview()&&!strict.contains(t.uri)&&present.add(t.uri)){fresh.add(t);queue.add(t.uri);if(++added==30)break;}
   int remove=Math.max(0,index-10);queue.subList(0,remove).clear();index-=remove;
   new WaveCatalog(this).put(fresh);checkpoint();
   if(added==0){waveRetryAfter=SystemClock.elapsedRealtime()+60000;notice="Нет новых доступных треков · повторим проверку";scheduleWaveRetry();publish();return;}
   waveRetryAfter=0;if(waveWaiting&&intent.wanted){waveWaiting=false;advance(false);}else publish();
  });}catch(Exception failure){handler.post(()->{if(epoch!=waveEpoch||!wave||stopped)return;waveBusy=false;waveRetryAfter=SystemClock.elapsedRealtime()+60000;notice="Волна ждёт соединения · продолжим автоматически";scheduleWaveRetry();publish();});}});
 }
 private void scheduleWaveRetry(){handler.removeCallbacks(waveRetry);if(intent.wanted)handler.postDelayed(waveRetry,Math.max(1000,waveRetryAfter-SystemClock.elapsedRealtime()));}
}
