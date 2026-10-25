package com.farika.soundflow;

/** Desktop site mode, preserving the installed Chromium version. Not an attestation or anti-bot bypass. */
public final class AccountBrowserProfile {
 private AccountBrowserProfile(){}
 public static String desktop(String userAgent){
  if(userAgent==null)return "";
  if(!userAgent.contains("Android"))return userAgent;
  return userAgent.replaceFirst("\\([^)]*\\)","(X11; Linux x86_64)")
   .replace(" Version/4.0","").replace(" Mobile Safari/"," Safari/");
 }
}
