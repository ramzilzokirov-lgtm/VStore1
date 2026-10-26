package com.vstore.app;

import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.widget.*;
import android.util.Log;
import org.json.*;
import java.io.*;
import java.net.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;

public class MainActivity extends Activity {
    private static final int PICK_APK=41;
    private static final String PREFS_AUTH = "vstore_auth";
    private static final String PREF_CREATOR_MODE = "isCreatorMode";
    private static final int CREATOR_TAPS_REQUIRED = 5;
    private int logoTapCount = 0;
    private long lastLogoTapAt = 0L;
    private boolean isCreatorMode = false;
    private static final String CATALOG_URL="https://raw.githubusercontent.com/ramzilzokirov-lgtm/VStore1/main/catalog.json";
    private static final String API="https://vstore1-catalog.onrender.com";
    private final ExecutorService pool=Executors.newFixedThreadPool(3);
    private final ArrayList<JSONObject> apps=new ArrayList<>();
    private final ArrayList<File> localApps=new ArrayList<>();
    private LinearLayout list, tabs;
    private EditText search;
    private TextView status, headerTitle;
    private String activeTab="Все проекты";
    private int bg=0xff111116, lavender=0xffd7b9e8, text=0xfff6f0f8, muted=0xffb9afc0, panel=0xff27232d;
    private int animationDuration=260;
    private static final String PREF_UI="vstore_ui";
    private LinearLayout pageRoot;
    private final HashMap<String,Integer> progress=new HashMap<>();
    @Override public void onCreate(Bundle b){
        super.onCreate(b); loadAppearance(); getWindow().setStatusBarColor(bg);getWindow().setNavigationBarColor(bg);
        LinearLayout root=new LinearLayout(this);pageRoot=root;root.setOrientation(1);root.setPadding(16,12,16,6);root.setBackgroundColor(bg);
        LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);
        isCreatorMode = getSharedPreferences(PREFS_AUTH, MODE_PRIVATE).getBoolean(PREF_CREATOR_MODE, false);
        headerTitle=label(isCreatorMode ? "VStore // Developer Hub  •  Создатель / Разиль Закиров" : "VStore // Developer Hub",22,lavender);headerTitle.setTypeface(null,1);top.addView(headerTitle,new LinearLayout.LayoutParams(0,-2,1));
        headerTitle.setOnClickListener(v -> onLogoTap());
        TextView settings=label("⚙",25,text);settings.setGravity(Gravity.CENTER);top.addView(settings,new LinearLayout.LayoutParams(44,48));settings.setOnClickListener(v->showSettings());
        TextView refresh=label("↻",28,text);refresh.setGravity(Gravity.CENTER);top.addView(refresh,new LinearLayout.LayoutParams(44,48));refresh.setOnClickListener(v->loadCatalog(true));
        root.addView(top);
        root.addView(label("Главный разработчик / Разиль Закиров • ID 8436409687",12,muted));
        LinearLayout banner=new LinearLayout(this);banner.setOrientation(1);banner.setPadding(15,12,15,12);banner.setBackground(round(0xff33283d,24));LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(-1,-2);bp.setMargins(0,12,0,10);root.addView(banner,bp);
        banner.addView(label("СООБЩЕСТВО VSTORE",13,lavender));banner.addView(label("Релизы, тестовые сборки и новости проектов",13,text));
        TextView telegram=label("Открыть Telegram-канал  ↗",13,lavender);banner.addView(telegram);telegram.setOnClickListener(v->openUrl("https://t.me/Vmods2"));
        tabs=new LinearLayout(this);tabs.setOrientation(0);HorizontalScrollView hsv=new HorizontalScrollView(this);hsv.setHorizontalScrollBarEnabled(false);hsv.addView(tabs);root.addView(hsv);
        search=new EditText(this);search.setSingleLine(true);search.setHint("Поиск проектов");search.setTextColor(text);search.setHintTextColor(muted);search.setBackgroundTintList(android.content.res.ColorStateList.valueOf(lavender));root.addView(search);
        status=label("Загрузка каталога…",12,muted);root.addView(status);
        ScrollView sc=new ScrollView(this);list=new LinearLayout(this);list.setOrientation(1);sc.addView(list);root.addView(sc,new LinearLayout.LayoutParams(-1,0,1));
        setContentView(root); if(animationDuration>0){root.setAlpha(0f);root.setTranslationY(18f);root.animate().alpha(1f).translationY(0f).setDuration(animationDuration+120L).start();}
        search.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int before,int count){render();}public void afterTextChanged(android.text.Editable e){}});
        File dir=new File(getFilesDir(),"library");if(dir.exists()){File[] fs=dir.listFiles((d,n)->n.toLowerCase(Locale.ROOT).endsWith(".apk"));if(fs!=null)localApps.addAll(Arrays.asList(fs));}
        makeTabs();loadCached();loadCatalog(false);
        headerTitle.setOnLongClickListener(v->{showCreatorTools();return true;});
        logCurrentCertificateSha256();
    }

    private void loadAppearance(){
        android.content.SharedPreferences p=getSharedPreferences(PREF_UI,MODE_PRIVATE);
        String theme=p.getString("theme","dark");
        if("system".equals(theme)){int night=getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;if(night==android.content.res.Configuration.UI_MODE_NIGHT_NO){bg=0xfff7f1f8;panel=0xffeee6f1;text=0xff241d29;muted=0xff6e6374;lavender=0xff8050a0;}}
        boolean dynamic=p.getBoolean("dynamic",false)&&Build.VERSION.SDK_INT>=31;
        if("light".equals(theme)){bg=0xfff7f1f8;panel=0xffeee6f1;text=0xff241d29;muted=0xff6e6374;lavender=0xff8050a0;}
        if(dynamic){try{bg=getColor(android.R.color.system_neutral1_900);panel=getColor(android.R.color.system_neutral1_800);text=getColor(android.R.color.system_neutral1_50);lavender=getColor(android.R.color.system_accent1_200);muted=getColor(android.R.color.system_neutral2_200);}catch(Exception ignored){}}
        String accent=p.getString("accent","lavender");
        if("blue".equals(accent)){lavender=0xff9ccaff;}else if("green".equals(accent)){lavender=0xffa6dfb0;}else if("pink".equals(accent)){lavender=0xffffb6d5;}
        animationDuration=p.getInt("animation_ms",260);
    }
    private void showSettings(){
        LinearLayout content=new LinearLayout(this);content.setOrientation(1);content.setPadding(22,8,22,4);
        TextView appearance=label("ВНЕШНИЙ ВИД",12,lavender);appearance.setTypeface(null,1);content.addView(appearance);
        String theme=getSharedPreferences(PREF_UI,MODE_PRIVATE).getString("theme","dark");
        TextView themeRow=label("Тема: "+("light".equals(theme)?"Светлая":"dark".equals(theme)?"Тёмная":"Как в системе"),16,text);themeRow.setPadding(0,14,0,14);content.addView(themeRow);
        themeRow.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Тема").setSingleChoiceItems(new String[]{"Как в системе","Тёмная","Светлая"}, "system".equals(getSharedPreferences(PREF_UI,0).getString("theme","dark"))?0:"dark".equals(getSharedPreferences(PREF_UI,0).getString("theme","dark"))?1:2,(d,w)->{getSharedPreferences(PREF_UI,0).edit().putString("theme",new String[]{"system","dark","light"}[w]).apply();d.dismiss();recreate();}).setNegativeButton("Отмена",null).show());
        TextView accentRow=label("Акцентный цвет",16,text);accentRow.setPadding(0,14,0,14);content.addView(accentRow);
        accentRow.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Акцентный цвет").setSingleChoiceItems(new String[]{"Лавандовый","Голубой","Зелёный","Розовый"},new String[]{"lavender","blue","green","pink"}[0].equals(getSharedPreferences(PREF_UI,0).getString("accent","lavender"))?0: "blue".equals(getSharedPreferences(PREF_UI,0).getString("accent","lavender"))?1:"green".equals(getSharedPreferences(PREF_UI,0).getString("accent","lavender"))?2:3,(d,w)->{getSharedPreferences(PREF_UI,0).edit().putString("accent",new String[]{"lavender","blue","green","pink"}[w]).apply();d.dismiss();recreate();}).setNegativeButton("Отмена",null).show());
        if(Build.VERSION.SDK_INT>=31){Switch dynamic=new Switch(this);dynamic.setText("Цвета телефона (Android 12+)");dynamic.setTextColor(text);dynamic.setChecked(getSharedPreferences(PREF_UI,0).getBoolean("dynamic",false));content.addView(dynamic);dynamic.setOnCheckedChangeListener((b,checked)->getSharedPreferences(PREF_UI,0).edit().putBoolean("dynamic",checked).apply());}
        TextView motion=label("АНИМАЦИИ",12,lavender);motion.setTypeface(null,1);motion.setPadding(0,18,0,0);content.addView(motion);
        TextView speed=label("Длительность: "+animationDuration+" мс",16,text);speed.setPadding(0,10,0,4);content.addView(speed);
        SeekBar seek=new SeekBar(this);seek.setMax(600);seek.setProgress(animationDuration);content.addView(seek);seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int p,boolean u){speed.setText("Длительность: "+p+" мс");}public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){getSharedPreferences(PREF_UI,0).edit().putInt("animation_ms",s.getProgress()).apply();}});
        new AlertDialog.Builder(this).setTitle("Настройки").setView(content).setPositiveButton("Применить",(d,w)->recreate()).setNegativeButton("Закрыть",null).show();
    }
    private void makeTabs(){tabs.removeAllViews();String[] names={"Все проекты","Мои приложения","Плагины & Моды","Эксклюзив"};for(String n:names){TextView t=label(n,13,n.equals(activeTab)?bg:text);t.setGravity(Gravity.CENTER);t.setPadding(14,10,14,10);t.setBackground(round(n.equals(activeTab)?lavender:panel,22));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-2,-2);p.setMargins(0,0,8,6);tabs.addView(t,p);t.setOnClickListener(v->{activeTab=n;makeTabs();render();});}}
    private GradientDrawable round(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(radius);d.setStroke(1,0x40ffffff);return d;}
    private TextView label(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setPadding(0,3,0,3);return t;}
    private void loadCached(){String raw=getPreferences(0).getString("catalog_json","");if(!raw.isEmpty())try{applyCatalog(new JSONObject(raw),false);}catch(Exception ignored){}}
    private void loadCatalog(boolean force){status.setText("Обновление каталога…");pool.execute(()->{try{HttpURLConnection c=(HttpURLConnection)new URL(CATALOG_URL+(force?"?v="+System.currentTimeMillis():"")).openConnection();c.setConnectTimeout(8000);c.setReadTimeout(12000);c.setUseCaches(!force);String raw=readText(c.getInputStream());JSONObject json=new JSONObject(raw);getPreferences(0).edit().putString("catalog_json",raw).apply();runOnUiThread(()->applyCatalog(json,true));}catch(Exception e){runOnUiThread(()->{status.setText("Показан кэш • сервер каталога недоступен");render();});}});}
    private void applyCatalog(JSONObject json,boolean fresh){JSONArray a=json.optJSONArray("apps");if(a==null)return;synchronized(apps){apps.clear();for(int i=0;i<a.length();i++)apps.add(a.optJSONObject(i));}runOnUiThread(()->{status.setText(fresh?"Каталог обновлён":"Кэш каталога");render();});}
    private void render(){if(list==null)return;list.removeAllViews();String q=search.getText().toString().toLowerCase(Locale.ROOT);ArrayList<JSONObject> snapshot; synchronized(apps){snapshot=new ArrayList<>(apps);}
        int count=0;for(JSONObject a:snapshot){String category=a.optString("category","Apps");if(activeTab.equals("Плагины & Моды")&&!category.toLowerCase(Locale.ROOT).matches(".*(plugin|mod|плагин|мод).*"))continue;if(activeTab.equals("Эксклюзив")&&!a.optBoolean("exclusive",false))continue;if(activeTab.equals("Мои приложения")&&!a.optString("creator_id","8436409687").equals("8436409687"))continue;
            String name=a.optString("name","Приложение");if(!name.toLowerCase(Locale.ROOT).contains(q)&&!a.optString("description","").toLowerCase(Locale.ROOT).contains(q))continue;appCard(a);count++;}
        if(activeTab.equals("Мои приложения"))for(File f:localApps)if(f.getName().toLowerCase(Locale.ROOT).contains(q)){localCard(f);count++;}
        if(count==0){TextView empty=label("\nЗдесь пока нет опубликованных проектов.\nКаталог обновляется отдельно от APK клиента.",15,muted);empty.setGravity(Gravity.CENTER);empty.setPadding(20,48,20,48);list.addView(empty);}
    }
    private LinearLayout card(){LinearLayout c=new LinearLayout(this);c.setOrientation(1);c.setPadding(15,13,15,13);c.setBackground(round(panel,24));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,7,0,7);addSafe(list,c,p);if(animationDuration>0){c.setAlpha(0f);c.setTranslationY(12f);c.animate().alpha(1f).translationY(0f).setDuration(animationDuration).start();}return c;}
    private void addSafe(ViewGroup parent,View child,ViewGroup.LayoutParams params){if(child.getParent()!=null){ViewParent old=child.getParent();if(old instanceof ViewGroup)((ViewGroup)old).removeView(child);}parent.addView(child,params);}
    private void appCard(JSONObject a){LinearLayout c=card();LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);c.addView(row);
        ImageView icon=new ImageView(this);icon.setImageDrawable(getDrawable(android.R.drawable.sym_def_app_icon));icon.setBackground(round(0xff393140,16));icon.setClipToOutline(true);row.addView(icon,new LinearLayout.LayoutParams(48,48));
        String iconUrl=a.optString("icon_url","");if(!iconUrl.isEmpty())loadIcon(iconUrl,icon);
        LinearLayout col=new LinearLayout(this);col.setOrientation(1);col.setPadding(12,0,0,0);row.addView(col,new LinearLayout.LayoutParams(0,-2,1));
        LinearLayout nameRow=new LinearLayout(this);nameRow.setGravity(Gravity.CENTER_VERTICAL);col.addView(nameRow);TextView name=label(a.optString("name","Project"),16,text);name.setTypeface(null,1);nameRow.addView(name);String badge=a.optString("badge","");if(!badge.isEmpty()){TextView b=label(" "+badge+" ",10,bg);b.setBackground(round(lavender,8));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-2,-2);p.setMargins(7,0,0,0);nameRow.addView(b,p);}
        col.addView(label(a.optString("category","Apps")+" • "+a.optString("version",""),11,muted));
        c.addView(label(a.optString("description",""),13,text));
        String id=a.optString("id");Integer pr=progress.get(id);if(pr!=null&&pr>=0&&pr<100){ProgressBar bar=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);bar.setMax(100);bar.setProgress(pr);c.addView(bar);c.addView(label("Загрузка "+pr+"%",11,lavender));}
        LinearLayout actions=new LinearLayout(this);actions.setGravity(Gravity.CENTER_VERTICAL);c.addView(actions);
        String pkg=a.optString("package_name","");boolean installed=!pkg.isEmpty()&&installed(pkg);String url=a.optString("download_url","");
        String action=installed?"ОБНОВИТЬ":(url.isEmpty()?"НЕТ APK":"СКАЧАТЬ");
        TextView button=label(action,13,bg);button.setTypeface(null,1);button.setGravity(Gravity.CENTER);button.setPadding(20,11,20,11);button.setBackground(round(url.isEmpty()?0xff615967:lavender,18));actions.addView(button);button.setOnClickListener(v->{if(url.isEmpty()){toast("Для этого проекта ещё не опубликована ссылка на APK.");return;}download(a);});
        TextView version=label("  SHA-256 "+shortHash(a.optString("sha256","")),10,muted);actions.addView(version);
    }
    private void localCard(File f){LinearLayout c=card();LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);c.addView(row);ImageView icon=new ImageView(this);icon.setImageDrawable(apkIcon(f));row.addView(icon,new LinearLayout.LayoutParams(48,48));TextView name=label(apkLabel(f),16,text);name.setTypeface(null,1);name.setPadding(12,0,0,0);row.addView(name,new LinearLayout.LayoutParams(0,-2,1));TextView t=label("Локальный APK • "+String.format(Locale.ROOT,"%.1f MB",f.length()/1048576f)+" • SHA-256 "+shortHash(safeSha256(f)),12,muted);c.addView(t);LinearLayout actions=new LinearLayout(this);actions.setGravity(Gravity.CENTER_VERTICAL);c.addView(actions);TextView install=label("УСТАНОВИТЬ",13,bg);install.setGravity(Gravity.CENTER);install.setPadding(16,10,16,10);install.setBackground(round(lavender,18));actions.addView(install);install.setOnClickListener(v->install(f));TextView remove=label("  Удалить",13,muted);remove.setPadding(14,10,8,10);actions.addView(remove);remove.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Удалить APK?").setMessage(f.getName()).setNegativeButton("Отмена",null).setPositiveButton("Удалить",(d,w)->{if(f.delete())localApps.remove(f);render();}).show());}
    private String safeSha256(File f){try{return sha256(f);}catch(Exception e){return "ошибка";}}
    private String shortHash(String s){return s.isEmpty()?"—":s.substring(0,Math.min(12,s.length()))+"…";}
    private boolean installed(String pkg){try{getPackageManager().getPackageInfo(pkg,0);return true;}catch(Exception e){return false;}}
    private void loadIcon(String url,ImageView target){pool.execute(()->{try{HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();c.setConnectTimeout(5000);c.setReadTimeout(7000);Bitmap b=BitmapFactory.decodeStream(c.getInputStream());if(b!=null)runOnUiThread(()->target.setImageBitmap(b));}catch(Exception ignored){}});}
    private void download(JSONObject a){String id=a.optString("id"),url=a.optString("download_url");progress.put(id,0);render();pool.execute(()->{File out=new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),id+".apk");try{HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();c.setConnectTimeout(15000);c.setReadTimeout(120000);int len=c.getContentLength();try(InputStream in=c.getInputStream();OutputStream os=new FileOutputStream(out)){byte[] b=new byte[32768];int n;long total=0;while((n=in.read(b))!=-1){os.write(b,0,n);total+=n;if(len>0){int pct=(int)(total*100/len);progress.put(id,pct);runOnUiThread(this::render);}}}String expected=a.optString("sha256","");if(!expected.isEmpty()&&!expected.equalsIgnoreCase(sha256(out)))throw new IOException("SHA-256 не совпал");progress.remove(id);runOnUiThread(()->{render();install(out);});}catch(Exception e){progress.remove(id);if(out.exists())out.delete();runOnUiThread(()->{render();toast("Ошибка загрузки: "+e.getMessage());});}});}
    private void install(File apk){try{PackageInstaller pi=getPackageManager().getPackageInstaller();PackageInstaller.SessionParams p=new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);int id=pi.createSession(p);try(PackageInstaller.Session s=pi.openSession(id);InputStream in=new FileInputStream(apk);OutputStream out=s.openWrite("base.apk",0,apk.length())){byte[] b=new byte[65536];int n;while((n=in.read(b))!=-1)out.write(b,0,n);s.fsync(out);}Intent callback=new Intent(this,InstallResultReceiver.class);callback.setAction("com.vstore.app.INSTALL_RESULT");PendingIntent pending=PendingIntent.getBroadcast(this,id,callback,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_MUTABLE);try(PackageInstaller.Session s=pi.openSession(id)){s.commit(pending.getIntentSender());}}catch(Exception e){toast("Не удалось запустить установщик: "+e.getMessage());}}
    private Drawable apkIcon(File f){try{PackageInfo p=getPackageManager().getPackageArchiveInfo(f.getAbsolutePath(),0);if(p!=null&&p.applicationInfo!=null){p.applicationInfo.sourceDir=f.getAbsolutePath();p.applicationInfo.publicSourceDir=f.getAbsolutePath();return p.applicationInfo.loadIcon(getPackageManager());}}catch(Exception ignored){}return getDrawable(android.R.drawable.sym_def_app_icon);}
    private String apkLabel(File f){try{PackageInfo p=getPackageManager().getPackageArchiveInfo(f.getAbsolutePath(),0);if(p!=null&&p.applicationInfo!=null){p.applicationInfo.sourceDir=f.getAbsolutePath();p.applicationInfo.publicSourceDir=f.getAbsolutePath();CharSequence s=p.applicationInfo.loadLabel(getPackageManager());if(s!=null)return s.toString();}}catch(Exception ignored){}return f.getName();}
    private void onLogoTap() {
        long now = SystemClock.elapsedRealtime();
        if (now - lastLogoTapAt > 1800L) logoTapCount = 0;
        lastLogoTapAt = now;
        logoTapCount++;
        if (logoTapCount >= CREATOR_TAPS_REQUIRED) {
            logoTapCount = 0;
            if (isCreatorMode) showCreatorTools(); else showDeveloperKeyDialog();
        }
    }
    private void showDeveloperKeyDialog() {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setHint("Developer Key");
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        new AlertDialog.Builder(this).setTitle("Режим создателя")
            .setMessage("Введите Developer Key, заданный при сборке приложения. Если ключ не настроен, режим создателя недоступен.")
            .setView(input)
            .setPositiveButton("Проверить", (d,w) -> {
                String expected = BuildConfig.DEVELOPER_KEY_SHA256 == null ? "" : BuildConfig.DEVELOPER_KEY_SHA256.trim();
                String entered = input.getText().toString();
                String actual = sha256Text(entered);
                boolean valid = !entered.isEmpty() && !expected.isEmpty()
                    && constantTimeEquals(expected, actual);
                if (valid) {
                    getSharedPreferences(PREFS_AUTH, MODE_PRIVATE).edit().putBoolean(PREF_CREATOR_MODE, true).apply();
                    isCreatorMode = true;
                    headerTitle.setText("VStore // Developer Hub  •  Создатель / Разиль Закиров");
                    toast("Режим создателя включён на этом устройстве.");
                    showCreatorTools();
                } else {
                    toast("Неверный Developer Key.");
                }
            }).setNegativeButton("Отмена", null).show();
    }
    private void showCreatorTools(){
        if (!isCreatorMode) { showDeveloperKeyDialog(); return; }
        new AlertDialog.Builder(this).setTitle("Инструменты создателя")
            .setMessage("Разиль Закиров • Creator ID 8436409687\nSHA-256 сертификата сборки записан в Logcat с тегом VStoreAuth.")
            .setPositiveButton("Принудительно сбросить кэш", (d,w)->{
                getPreferences(0).edit().remove("catalog_json").apply();
                loadCatalog(true);
            })
            .setNeutralButton("Выбрать тестовый APK",(d,w)->{
                Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);
                i.setType("application/vnd.android.package-archive");
                i.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(i,PICK_APK);
            }).setNegativeButton("Закрыть",null).show();
    }
    private String sha256Text(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes("UTF-8"));
            StringBuilder out = new StringBuilder();
            for (byte b : digest) out.append(String.format(Locale.ROOT, "%02x", b));
            return out.toString();
        } catch (Exception e) { return ""; }
    }
    private boolean constantTimeEquals(String expected, String actual) {
        if (expected == null || actual == null) return false;
        byte[] a = expected.toLowerCase(Locale.ROOT).getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] b = actual.toLowerCase(Locale.ROOT).getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return MessageDigest.isEqual(a, b);
    }
    private void logCurrentCertificateSha256() {
        try {
            PackageManager pm = getPackageManager();
            PackageInfo info = pm.getPackageInfo(getPackageName(), PackageManager.GET_SIGNING_CERTIFICATES);
            Signature[] signatures;
            if (Build.VERSION.SDK_INT >= 28 && info.signingInfo != null) {
                signatures = info.signingInfo.hasMultipleSigners()
                    ? info.signingInfo.getApkContentsSigners()
                    : info.signingInfo.getSigningCertificateHistory();
            } else {
                signatures = info.signatures;
            }
            if (signatures != null && signatures.length > 0) {
                String certSha256 = sha256Bytes(signatures[0].toByteArray());
                Log.d("VStoreAuth", "Current cert: " + certSha256);
            } else {
                Log.d("VStoreAuth", "Current cert: unavailable");
            }
        } catch (Exception e) {
            Log.d("VStoreAuth", "Current cert: unavailable", e);
        }
    }
    private String sha256Bytes(byte[] bytes) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
        StringBuilder out = new StringBuilder();
        for (byte b : digest) out.append(String.format(Locale.ROOT, "%02x", b));
        return out.toString();
    }
    @Override protected void onActivityResult(int r,int result,Intent data){super.onActivityResult(r,result,data);if(r!=PICK_APK||result!=RESULT_OK||data==null||data.getData()==null)return;try{File dir=new File(getFilesDir(),"library");if(!dir.exists()&&!dir.mkdirs())throw new IOException("Не удалось создать библиотеку");String name="import_"+System.currentTimeMillis()+".apk";File f=new File(dir,name);try(InputStream in=getContentResolver().openInputStream(data.getData());OutputStream out=new FileOutputStream(f)){if(in==null)throw new IOException("Файл недоступен");byte[] b=new byte[8192];int n;long total=0;while((n=in.read(b))!=-1){total+=n;if(total>200L*1024L*1024L)throw new IOException("APK больше 200 МБ");out.write(b,0,n);}}if(f.length()==0)throw new IOException("Выбран пустой файл");localApps.add(f);activeTab="Мои приложения";makeTabs();render();toast("APK добавлен в библиотеку: "+f.getName());}catch(Exception e){toast("Не удалось импортировать APK: "+e.getMessage());}}
    private String sha256(File f)throws Exception{MessageDigest md=MessageDigest.getInstance("SHA-256");try(InputStream in=new FileInputStream(f)){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)md.update(b,0,n);}StringBuilder s=new StringBuilder();for(byte b:md.digest())s.append(String.format(Locale.ROOT,"%02x",b));return s.toString();}
    private String readText(InputStream in)throws IOException{ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);return new String(out.toByteArray(),"UTF-8");}
    private void openUrl(String s){try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(s)));}catch(Exception e){toast("Ссылка недоступна");}}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
    @Override protected void onDestroy(){super.onDestroy();pool.shutdown();}
}