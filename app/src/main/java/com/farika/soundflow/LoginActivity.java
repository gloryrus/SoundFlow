package com.farika.soundflow;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.net.http.SslError;
import android.os.*;
import android.view.*;
import android.webkit.*;
import android.widget.*;
import java.util.*;
import java.util.concurrent.*;
import org.json.JSONObject;

/** Credentials are handled by the official page. No JS bridge, script or password access. */
public class LoginActivity extends Activity {
 private WebView web;private TextView status;private boolean checking,confirming,waiting;private volatile boolean done;private boolean webReady;
 private String lastClient="",lastCheck="NOT_STARTED",pageError="NONE",source="NONE";private int apiRequests,authHeaders;private boolean cookieFound;
 private final ArrayDeque<Candidate> queue=new ArrayDeque<>();private final LinkedHashSet<String> attempted=new LinkedHashSet<>();
 private final Handler handler=new Handler(Looper.getMainLooper());private final ExecutorService worker=Executors.newSingleThreadExecutor();
 private static final class Candidate {final String auth,client,source;Candidate(String a,String c,String s){auth=a;client=c;source=s;}}
 private final Runnable poll=new Runnable(){public void run(){scanCookies();if(!done)handler.postDelayed(this,2000);}};
 @Override public void onCreate(Bundle state){super.onCreate(state);getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);getWindow().setStatusBarColor(0xff0e1016);getWindow().setNavigationBarColor(0xff0e1016);
  LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setBackgroundColor(0xff0e1016);
  if(Build.VERSION.SDK_INT>=35)root.setOnApplyWindowInsetsListener((v,insets)->{android.graphics.Insets b=insets.getInsets(WindowInsets.Type.systemBars());v.setPadding(b.left,b.top,b.right,b.bottom);return insets;});
  LinearLayout bar=new LinearLayout(this);bar.setPadding(dp(8),0,dp(8),0);Button back=button("Назад");back.setOnClickListener(v->finish());bar.addView(back,new LinearLayout.LayoutParams(0,dp(52),1));TextView title=new TextView(this);title.setText("SoundCloud");title.setTextSize(16);title.setTextColor(Color.WHITE);title.setGravity(Gravity.CENTER);bar.addView(title,new LinearLayout.LayoutParams(0,dp(52),1));Button ready=button("Готово");ready.setOnClickListener(v->ready());bar.addView(ready,new LinearLayout.LayoutParams(0,dp(52),1));root.addView(bar);
  status=new TextView(this);status.setText("Войдите на сайте SoundCloud, затем нажмите «Готово».");status.setTextColor(0xffc2c8d5);status.setTextSize(14);status.setPadding(dp(16),dp(8),dp(16),dp(12));root.addView(status,new LinearLayout.LayoutParams(-1,-2));
  try{web=new WebView(this);webReady=true;root.addView(web,new LinearLayout.LayoutParams(-1,0,1));}catch(RuntimeException unavailable){pageError="WEBVIEW_UNAVAILABLE";TextView fallback=new TextView(this);fallback.setText("Android System WebView недоступен. Установите или обновите его из доверенного магазина приложений, затем откройте вход заново.");fallback.setTextColor(Color.WHITE);fallback.setTextSize(16);fallback.setPadding(dp(24),dp(32),dp(24),dp(24));root.addView(fallback,new LinearLayout.LayoutParams(-1,0,1));status.setText("Компонент входа недоступен. Сессия не подключена.");}
  LinearLayout tools=new LinearLayout(this);Button library=button("Библиотека сайта");library.setOnClickListener(v->openLibrary());tools.addView(library,new LinearLayout.LayoutParams(0,dp(52),1));Button diagnostics=button("Диагностика");diagnostics.setOnClickListener(v->diagnostics());tools.addView(diagnostics,new LinearLayout.LayoutParams(0,dp(52),1));root.addView(tools);setContentView(root);
  if(!webReady)return;
  WebSettings s=web.getSettings();s.setUserAgentString(AccountBrowserProfile.desktop(s.getUserAgentString()));s.setJavaScriptEnabled(true);s.setDomStorageEnabled(true);s.setAllowFileAccess(false);s.setAllowContentAccess(false);s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);s.setUseWideViewPort(true);s.setLoadWithOverviewMode(true);
  CookieManager.getInstance().setAcceptCookie(true);CookieManager.getInstance().setAcceptThirdPartyCookies(web,true);
  web.setWebViewClient(new WebViewClient(){
   @Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r){if(!r.isForMainFrame())return false;boolean ok=AuthCapture.site(r.getUrl().toString());if(!ok)SoundFlowUi.notice(LoginActivity.this,"Этот переход недоступен в приложении. Используйте вход по email на сайте SoundCloud.");return !ok;}
   @Override public WebResourceResponse shouldInterceptRequest(WebView v,WebResourceRequest r){observe(r);return null;}
   @Override public void onPageFinished(WebView v,String url){CookieManager.getInstance().flush();scanCookies();}
   @Override public void onReceivedSslError(WebView v,SslErrorHandler h,SslError e){h.cancel();pageError="TLS_ERROR";status.setText("Ошибка сертификата. Соединение остановлено.");}
   @Override public void onReceivedError(WebView v,WebResourceRequest r,WebResourceError e){if(r.isForMainFrame()){pageError="WEBVIEW_"+e.getErrorCode();status.setText("Страница недоступна. Проверьте сеть и доступ приложения к VPN.");}}
   @Override public void onReceivedHttpError(WebView v,WebResourceRequest r,WebResourceResponse e){if(r.isForMainFrame())pageError="HTTP_"+e.getStatusCode();}
  });
  ServiceWorkerController.getInstance().setServiceWorkerClient(new ServiceWorkerClient(){@Override public WebResourceResponse shouldInterceptRequest(WebResourceRequest r){observe(r);return null;}});
  web.loadUrl("https://soundcloud.com/signin");handler.postDelayed(poll,2000);
 }
 private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
 private Button button(String text){Button b=new Button(this);b.setText(text);b.setTextSize(14);b.setAllCaps(false);b.setTextColor(0xffb7f568);return b;}
 /** Observe only first-party API headers, without modifying or replaying website requests. */
 private void observe(WebResourceRequest r){String url=r.getUrl().toString();if(done||!AuthCapture.api(url))return;final String auth=AuthCapture.header(r.getRequestHeaders()),client=AuthCapture.client(url);handler.post(()->{if(done)return;apiRequests++;if(!client.isEmpty())lastClient=client;if(!auth.isEmpty()){authHeaders++;offer(auth,client.isEmpty()?lastClient:client,"API_HEADER");}});}
 private void scanCookies(){if(done||web==null)return;ArrayList<String> urls=new ArrayList<>();String current=web.getUrl();if(AuthCapture.site(current))urls.add(current);Collections.addAll(urls,"https://soundcloud.com/","https://www.soundcloud.com/","https://m.soundcloud.com/","https://secure.soundcloud.com/","https://api-auth.soundcloud.com/","https://api-v2.soundcloud.com/");
  for(String url:urls){String auth=AuthCapture.cookie(CookieManager.getInstance().getCookie(url));if(!auth.isEmpty()){cookieFound=true;offer(auth,lastClient,"COOKIE");}}
 }
 private void offer(String auth,String client,String from){if(done||confirming)return;String key=auth+"|"+client;if(attempted.contains(key)||queue.size()>=12)return;for(Candidate c:queue)if(c.auth.equals(auth)&&c.client.equals(client))return;queue.add(new Candidate(auth,client,from));next();}
 private void next(){if(done||checking||confirming||queue.isEmpty())return;Candidate c=queue.removeFirst();String key=c.auth+"|"+c.client;if(attempted.size()>=32)return;attempted.add(key);checking=true;source=c.source;status.setText("Проверяем аккаунт напрямую у SoundCloud…");lastCheck="CHECKING";
  worker.execute(()->{try{ScApi verifier=new ScApi(this,c.auth,c.client);JSONObject me=verifier.me();Candidate confirmed=new Candidate(c.auth,verifier.effectiveClient(),c.source);if(me.optLong("id",0)<=0)throw new org.json.JSONException("Missing account id");handler.post(()->{if(done||isFinishing())return;checking=false;confirming=true;lastCheck="HTTP_200";status.setText("Аккаунт подтверждён. Разрешите подключение.");new AlertDialog.Builder(this).setTitle("Подключить аккаунт?").setMessage(me.optString("username","SoundCloud")+"\n\nСессия будет сохранена только на этом устройстве.").setPositiveButton("Подключить",(d,w)->save(confirmed,me)).setNegativeButton("Не подключать",(d,w)->decline()).setOnCancelListener(d->decline()).show();});}
   catch(Exception e){final String result=errorCode(e);handler.post(()->{if(done)return;checking=false;lastCheck=result;status.setText("Сессия найдена, но SoundCloud её не подтвердил ("+result+"). Можно повторить «Готово» или открыть диагностику.");next();});}});
 }
 private void decline(){confirming=false;status.setText("Подключение отменено. Можно выбрать другой аккаунт на сайте.");next();}
 private void save(Candidate c,JSONObject me){try{String previous=getSharedPreferences("profile",0).getString("id","");if(!previous.isEmpty()&&!previous.equals(me.optString("id"))){confirming=false;status.setText("Это другой аккаунт. Сначала выйдите из прежнего аккаунта через настройки приложения. Офлайн-данные автоматически не удалены.");return;}Session.put(this,c.auth,c.client);getSharedPreferences("profile",0).edit().putString("id",me.optString("id")).putString("name",me.optString("username","SoundCloud")).apply();if(!c.client.isEmpty())getSharedPreferences("web_config",0).edit().putString("id",c.client).putLong("at",System.currentTimeMillis()).apply();done=true;CookieManager.getInstance().flush();setResult(RESULT_OK);finish();}catch(Exception e){confirming=false;lastCheck="STORE_ERROR";status.setText("Не удалось безопасно сохранить сессию. Откройте диагностику.");}}
 private void ready(){if(!webReady){diagnostics();return;}if(done||confirming)return;if(checking){SoundFlowUi.notice(this,"Идёт проверка сессии…");return;}attempted.clear();scanCookies();if(checking)return;openLibrary();}
 private void openLibrary(){if(!webReady){diagnostics();return;}if(done||checking||confirming)return;waiting=true;status.setText("Открываем библиотеку сайта, чтобы обнаружить сессию…");web.loadUrl("https://soundcloud.com/you/likes");handler.postDelayed(()->{if(done||checking||confirming||!waiting)return;waiting=false;status.setText("Если на сайте вход выполнен, но подключения нет, откройте «Диагностика» и скопируйте отчёт. Повторять ввод пароля не нужно.");},12000);}
 private static String errorCode(Exception e){if(e instanceof ScApi.ApiError)return "HTTP_"+((ScApi.ApiError)e).status;if(e instanceof java.net.SocketTimeoutException)return "TIMEOUT";if(e instanceof javax.net.ssl.SSLException)return "TLS_ERROR";if(e instanceof java.net.UnknownHostException)return "DNS_ERROR";if(e instanceof org.json.JSONException)return "PARSE_ERROR";return "NETWORK_ERROR";}
 private String report(){String host="NONE";try{String h=new java.net.URI(web.getUrl()).getHost();if(h!=null&&AuthCapture.site(web.getUrl()))host=h;}catch(Exception ignored){}String version="unknown";try{android.content.pm.PackageInfo pkg=WebView.getCurrentWebViewPackage();if(pkg!=null)version=pkg.packageName+" "+pkg.versionName;}catch(RuntimeException ignored){}
  return "SoundFlow "+Motion.version(this)+"\nAndroid API: "+Build.VERSION.SDK_INT+"\nWebView: "+version+"\nPage host: "+host+"\nPage error: "+pageError+"\nAPI requests: "+apiRequests+"\nAuth headers detected: "+authHeaders+"\nSession cookie found: "+cookieFound+"\nCandidate source: "+source+"\nSession check: "+lastCheck+"\nNo passwords, tokens or cookie values included.";}
 private void diagnostics(){String text=report();new AlertDialog.Builder(this).setTitle("Диагностика входа").setMessage(text).setPositiveButton("Скопировать",(d,w)->{((android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("Диагностика входа",text));SoundFlowUi.notice(this,"Отчёт скопирован — без паролей и токенов");}).setNegativeButton("Закрыть",null).show();}
 @Override protected void onDestroy(){done=true;handler.removeCallbacksAndMessages(null);queue.clear();attempted.clear();worker.shutdownNow();if(webReady)ServiceWorkerController.getInstance().setServiceWorkerClient(new ServiceWorkerClient(){public WebResourceResponse shouldInterceptRequest(WebResourceRequest r){return null;}});if(web!=null){web.stopLoading();web.destroy();web=null;}super.onDestroy();}
 @Override public void onBackPressed(){if(web!=null&&web.canGoBack())web.goBack();else super.onBackPressed();}
}
