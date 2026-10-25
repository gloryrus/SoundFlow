package com.farika.soundflow;
import java.net.*;import java.io.*;
/** Stable, sanitized codes. Never store response bodies, signed URLs, tokens or arbitrary exception text. */
public final class DownloadFault {
 public final String code;public final boolean retry;private DownloadFault(String c,boolean r){code=c;retry=r;}
 public static DownloadFault of(Throwable e){for(Throwable x=e;x!=null;x=x.getCause()){String raw=x.getMessage()==null?"":x.getMessage();if(raw.equals("DRM_TIMEOUT")||raw.equals("DRM_DNS_ERROR")||raw.equals("DRM_TLS_ERROR"))return new DownloadFault("NETWORK",true);if(raw.matches("DRM_HTTP_[0-9]{3}")){int status=Integer.parseInt(raw.substring(9));return new DownloadFault(raw,status==429||status>=500);}
  if(x instanceof ScApi.ApiError){int s=((ScApi.ApiError)x).status;if((s==404||s==410)&&"TRACK".equals(((ScApi.ApiError)x).stage))return new DownloadFault("TRACK_UNAVAILABLE",false);return new DownloadFault("HTTP_"+s,s==408||s==429||s>=500);}
  if(x instanceof EOFException)return new DownloadFault("INCOMPLETE_FILE",true);
  if(x instanceof javax.net.ssl.SSLHandshakeException||x instanceof javax.net.ssl.SSLPeerUnverifiedException)return new DownloadFault("TLS",false);
  if(x instanceof SocketTimeoutException||x instanceof UnknownHostException||x instanceof ConnectException||x instanceof SocketException)return new DownloadFault("NETWORK",true);
  if(x instanceof androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException){int s=((androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException)x).responseCode;return new DownloadFault("CDN_HTTP_"+s,s==408||s==429||s>=500||s==404||s==410);}
 }
 String m=e.getMessage()==null?"":e.getMessage();
 if(m.contains("Недостаточно памяти")||m.contains("ENOSPC"))return new DownloadFault("STORAGE",false);
 if(m.contains("только превью"))return new DownloadFault("PREVIEW_ONLY",false);
 if(m.equals("CLEAR_STREAM_ENCRYPTED"))return new DownloadFault("ENCRYPTED_STREAM",false);
 if(m.equals("NO_OFFLINE_SOURCE"))return new DownloadFault("NO_OFFLINE_SOURCE",false);
 if(m.contains("не разрешил скачивание"))return new DownloadFault("ORIGINAL_DISABLED",false);
 if(m.contains("Widevine")&&m.contains("устройств"))return new DownloadFault("DEVICE_DRM",false);
 if(m.startsWith("Офлайн-лицензия не получена"))return new DownloadFault("LICENSE_DENIED",false);
 if(m.contains("инициализации")||m.contains("PSSH")||m.contains("HLS")||m.contains("аудиопоток")||m.contains("совместимый")||m.contains("потока"))return new DownloadFault("FORMAT",false);
 if(m.contains("регион")||m.contains("не разрешает воспроизведение"))return new DownloadFault("ACCESS",false);
 if(m.contains("Неполный аудиофайл")||m.contains("пустой файл"))return new DownloadFault("INCOMPLETE_FILE",true);
 if(e instanceof org.json.JSONException)return new DownloadFault("RESPONSE_FORMAT",true);
 if(e instanceof FileNotFoundException)return new DownloadFault("LOCAL_IO",false);
 if(e instanceof IOException)return new DownloadFault("IO_ERROR",true);
 return new DownloadFault("OTHER",false);
 }
 public static String label(String code){switch(code){case "ARTWORK_PENDING":return "Аудио сохранено, обложка ещё не загрузилась — повторим без скачивания аудио заново";case "TRACK_UNAVAILABLE":return "SoundCloud не отдаёт сам трек (404/410): он удалён, скрыт или недоступен аккаунту; позже проверим снова";case "TLS":return "Не подтверждён защищённый канал: проверьте дату, сертификаты и сеть";case "RESPONSE_FORMAT":return "Сервис вернул некорректные данные — будет повтор";case "LOCAL_IO":return "Не удалось открыть или создать локальный файл";case "IO_ERROR":return "Ошибка чтения или передачи данных — будет повтор";case "NETWORK":return "Сеть или тайм-аут";case "STORAGE":return "Недостаточно памяти";case "PREVIEW_ONLY":return "Доступно только превью";case "ORIGINAL_DISABLED":return "Оригинал не разрешён; лицензия в этой попытке не запрашивалась";case "NO_OFFLINE_SOURCE":return "Старый отказ версии до прямого кэша — повторите загрузку";case "DEVICE_DRM":return "На устройстве недоступен Widevine";case "LICENSE_DENIED":return "Сервис не выдал офлайн-лицензию";case "ENCRYPTED_STREAM":return "Поток содержит шифрование: нужен совместимый источник с лицензией";case "FORMAT":return "Не удалось подготовить формат потока";case "ACCESS":return "Ограничен доступ к треку";case "INCOMPLETE_FILE":return "Файл загружен не полностью";case "RETRY_LATER":return "Ожидает повторной попытки";case "LICENSE_OR_NETWORK":return "Старый отказ: повторите для уточнения причины";case "OTHER":return "Неопределённая ошибка — нужен повтор для диагностики";default:return code.matches("(CDN_|DRM_)?HTTP_[0-9]{3}")?"Ответ сервиса: "+code:"Требуется повторная проверка";}}
}
