package com.farika.soundflow;
import java.io.*;import java.net.*;import java.nio.charset.StandardCharsets;import java.security.MessageDigest;import java.util.*;import java.util.regex.*;
/** Byte-range resume ONLY for an already-authorized uploader original. No streams/DRM/URLs/tokens in metadata. */
public final class OriginalTransfer {
 public interface Source {HttpURLConnection open(long offset,String validator)throws Exception;}
 public interface Progress {boolean cancelled();void update(int percent);}
 private static final Pattern RANGE=Pattern.compile("bytes (\\d+)-(\\d+)/(\\d+)");
 public static String key(String owner,String id)throws Exception{byte[] hash=MessageDigest.getInstance("SHA-256").digest((owner+"\n"+id).getBytes(StandardCharsets.UTF_8));StringBuilder s=new StringBuilder();for(byte b:hash)s.append(String.format(Locale.ROOT,"%02x",b&255));return s.toString();}
 private static void check(Progress p)throws IOException{if(Thread.currentThread().isInterrupted()||p!=null&&p.cancelled())throw new InterruptedIOException("Загрузка приостановлена");}
 private static String validator(HttpURLConnection c){String e=c.getHeaderField("ETag");if(e!=null&&!e.startsWith("W/")&&e.matches("\"[^\\r\\n]+\""))return e;String m=c.getHeaderField("Last-Modified");return m!=null&&c.getHeaderFieldDate("Last-Modified",0)>0&&!m.contains("\r")&&!m.contains("\n")?m:"";}
 private static Properties load(File f){Properties p=new Properties();try(InputStream in=new FileInputStream(f)){p.load(in);}catch(Exception ignored){}return p;}
 private static void save(File f,Properties p)throws IOException{File tmp=new File(f+".tmp");try(FileOutputStream out=new FileOutputStream(tmp)){p.store(out,"Authorized original resume state; no credentials or source URLs");out.getFD().sync();}if(!tmp.renameTo(f))throw new IOException("Не удалось сохранить состояние докачки");}
 private static long number(String s){try{return Long.parseLong(s);}catch(Exception e){return -1;}}
 private static void reset(File part,File meta)throws IOException{meta.delete();try(FileOutputStream out=new FileOutputStream(part)){out.getFD().sync();}}
 public static synchronized File fetch(File dir,String key,String identity,Source source,Progress progress)throws Exception{
  if(!key.matches("[a-f0-9]{64}"))throw new IOException("Некорректное имя докачки");if(!dir.exists()&&!dir.mkdirs())throw new IOException("Не удалось создать папку докачки");
  File part=new File(dir,key+".part"),meta=new File(dir,key+".meta");Properties old=load(meta);long offset=part.length(),previousTotal=number(old.getProperty("total"));String prior=old.getProperty("validator","");
  if(offset<=0||previousTotal<=offset||prior.isEmpty()||!identity.equals(old.getProperty("identity"))){reset(part,meta);offset=0;prior="";}
  for(int pass=0;pass<2;pass++){
   check(progress);HttpURLConnection c=source.open(offset,prior);boolean preserve=false;
   try{
    int status=c.getResponseCode();if(status==416&&offset>0&&pass==0){reset(part,meta);offset=0;prior="";continue;}
    if(status!=200&&status!=206)throw new IOException("ORIGINAL_HTTP_"+status);
    String encoding=c.getContentEncoding();if(encoding!=null&&!encoding.equalsIgnoreCase("identity"))throw new IOException("Неподдерживаемое кодирование оригинала");
    String type=c.getContentType();if(type!=null&&(type.contains("html")||type.contains("json")))throw new IOException("SoundCloud не отдал аудиофайл");
    long total=c.getContentLengthLong(),expectedBody=total;String current=validator(c);boolean append=false;
    if(status==206){Matcher r=RANGE.matcher(String.valueOf(c.getHeaderField("Content-Range")));if(!r.matches())throw new ProtocolException("Некорректный диапазон оригинала");long start=number(r.group(1)),end=number(r.group(2));total=number(r.group(3));if(start!=offset||end<start||end>=total||end!=total-1||expectedBody>=0&&expectedBody!=end-start+1||offset>0&&(!prior.equals(current)||total!=previousTotal))throw new ProtocolException("Диапазон или версия оригинала изменились");expectedBody=end-start+1;append=offset>0;}
    else {offset=0;reset(part,meta);}
    if(total>0&&total-offset>dir.getUsableSpace()-16*1024*1024L)throw new IOException("Недостаточно памяти");
    Properties state=new Properties();state.setProperty("identity",identity);state.setProperty("validator",current);state.setProperty("total",Long.toString(total));save(meta,state);preserve=!current.isEmpty()&&total>0;
    long received=0;try(InputStream in=c.getInputStream();FileOutputStream out=new FileOutputStream(part,append)){byte[] b=new byte[65536];int n;while(true){check(progress);n=in.read(b);if(n==-1)break;check(progress);if(expectedBody>=0&&received+n>expectedBody)throw new ProtocolException("Слишком длинный оригинал");if(dir.getUsableSpace()<4*1024*1024L)throw new IOException("Недостаточно памяти");out.write(b,0,n);received+=n;if(progress!=null)progress.update(total>0?(int)Math.min(99,(offset+received)*100/total):-1);}out.getFD().sync();}
    check(progress);if(expectedBody>=0&&received!=expectedBody||total>=0&&part.length()!=total||part.length()==0)throw new EOFException("Неполный аудиофайл");meta.delete();return part;
   }catch(Exception e){if(!preserve||e instanceof ProtocolException){part.delete();meta.delete();}throw e;}finally{c.disconnect();}
  }throw new IOException("Не удалось возобновить оригинал");
 }
 public static void clear(File dir){File[] files=dir.listFiles();if(files!=null)for(File f:files)if(f.getName().matches("[a-f0-9]{64}\\.(part|meta|meta\\.tmp)"))f.delete();}
}
