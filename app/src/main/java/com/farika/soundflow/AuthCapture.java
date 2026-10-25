package com.farika.soundflow;

import java.net.URI;
import java.net.URLDecoder;
import java.util.Map;

/** Strict parsing of session material from this app's own SoundCloud WebView. */
public final class AuthCapture {
 private AuthCapture(){}
 public static boolean site(String raw){try{URI u=new URI(raw);String h=u.getHost();return "https".equalsIgnoreCase(u.getScheme())&&u.getRawUserInfo()==null&&(u.getPort()==-1||u.getPort()==443)&&h!=null&&(h.equalsIgnoreCase("soundcloud.com")||h.toLowerCase(java.util.Locale.ROOT).endsWith(".soundcloud.com"));}catch(Exception e){return false;}}
 public static boolean challenge(String raw){try{URI u=new URI(raw);String h=u.getHost();return "https".equals(u.getScheme())&&u.getRawUserInfo()==null&&(u.getPort()==-1||u.getPort()==443)&&h!=null&&(h.equals("captcha-delivery.com")||h.endsWith(".captcha-delivery.com"));}catch(Exception e){return false;}}
 public static boolean api(String raw){if(!site(raw))return false;try{String h=new URI(raw).getHost();return "api-v2.soundcloud.com".equalsIgnoreCase(h)||"api.soundcloud.com".equalsIgnoreCase(h);}catch(Exception e){return false;}}
 private static boolean token(String t){if(t.length()<8||t.length()>8192)return false;for(int i=0;i<t.length();i++){char c=t.charAt(i);if(c<=32||c>=127)return false;}return true;}
 public static String authorization(String value){if(value==null)return "";String v=value.trim();int space=v.indexOf(' ');if(space<0)return "";String scheme=v.substring(0,space),t=v.substring(space+1).trim();if(!scheme.equalsIgnoreCase("OAuth")&&!scheme.equalsIgnoreCase("Bearer"))return "";return token(t)?(scheme.equalsIgnoreCase("Bearer")?"Bearer ":"OAuth ")+t:"";}
 public static String header(Map<String,String> headers){if(headers==null)return "";for(Map.Entry<String,String> e:headers.entrySet())if("Authorization".equalsIgnoreCase(e.getKey()))return authorization(e.getValue());return "";}
 public static String cookie(String cookies){if(cookies==null)return "";for(String p:cookies.split(";")){p=p.trim();if(!p.startsWith("oauth_token="))continue;String v=p.substring(12);try{v=URLDecoder.decode(v.replace("+","%2B"),"UTF-8");}catch(Exception e){return "";}return token(v)?"OAuth "+v:"";}return "";}
 public static String client(String raw){if(!api(raw))return "";try{String query=new URI(raw).getRawQuery();if(query==null)return "";for(String p:query.split("&")){if(!p.startsWith("client_id="))continue;String v=URLDecoder.decode(p.substring(10),"UTF-8");return v.matches("[A-Za-z0-9_-]{16,256}")?v:"";}}catch(Exception ignored){}return "";}
}
