package com.farika.soundflow;
import java.util.regex.*;
/** Extract only enumerated protocol error codes. Never expose arbitrary response content. */
public final class ApiRefusal {
 private static final String CODES="invalid_token|invalid_client|insufficient_scope|access_denied|rate_limit_exceeded";
 private static final Pattern JSON=Pattern.compile("\"(?:error|error_code|code)\"\\s*:\\s*\"("+CODES+")\"",Pattern.CASE_INSENSITIVE);
 private static final Pattern AUTH=Pattern.compile("\\berror\\s*=\\s*\"?("+CODES+")(?:\"|,|\\s|$)",Pattern.CASE_INSENSITIVE);
 public static String extract(String body,String auth){Matcher m=JSON.matcher(body==null?"":body);if(m.find())return m.group(1).toLowerCase(java.util.Locale.ROOT);m=AUTH.matcher(auth==null?"":auth);return m.find()?m.group(1).toLowerCase(java.util.Locale.ROOT):"";}
 public static String label(String code){switch(code){case "invalid_token":return "Сервис сообщил о недействительной сессии";case "invalid_client":return "Сервис отклонил авторизацию приложения";case "insufficient_scope":return "Сервис сообщил о недостаточных правах сессии";case "access_denied":return "Сервис сообщил об отказе в доступе";case "rate_limit_exceeded":return "Сервис сообщил о превышении частоты запросов";default:return "Точная причина отказа сервисом не сообщена";}}
}
