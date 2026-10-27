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
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.button.MaterialButton;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;
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
    private RecyclerView catalogRecycler;
    private VStoreAdapter catalogAdapter;
    private EditText search;
    private TextView status, headerTitle;
    private String activeTab="Все проекты";
    private int bg=0xff0a0910, lavender=0xff7c4dff, text=0xfff6f0f8, muted=0xffb9afc0, panel=0x1affffff;
    private int animationDuration=260;
    private static final String PREF_UI="vstore_ui";
    private LinearLayout pageRoot;
    private final java.util.concurrent.ConcurrentHashMap<String,Integer> progress=new java.util.concurrent.ConcurrentHashMap<>();
    @Override public void onCreate(Bundle b){
        super.onCreate(b); try { loadAppearance(); getWindow().setStatusBarColor(bg);getWindow().setNavigationBarColor(bg);
        LinearLayout root=new LinearLayout(this);pageRoot=root;root.setOrientation(1);root.setPadding(16,12,16,6);GradientDrawable screenBg=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{0xff161424,0xff0a0910});root.setBackground(getDrawable(com.vstore.app.R.drawable.bg_screen_gradient));
        LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);
        isCreatorMode = getSharedPreferences(PREFS_AUTH, MODE_PRIVATE).getBoolean(PREF_CREATOR_MODE, false);
        headerTitle=label("VStore",28,lavender);headerTitle.setTypeface(null,1);top.addView(headerTitle,new LinearLayout.LayoutParams(0,-2,1));
        headerTitle.setOnClickListener(v -> {tap(v);onLogoTap();});
        LinearLayout settings=new LinearLayout(this);settings.setOrientation(0);settings.setGravity(Gravity.CENTER);settings.setPadding(dp(12),0,dp(14),0);settings.setBackground(round(0x22ffffff,18));settings.setContentDescription("Настройки VStore");TextView settingsIcon=label("⚙",20,lavender);settingsIcon.setGravity(Gravity.CENTER);settings.addView(settingsIcon);TextView settingsText=label("Настройки",13,text);settingsText.setTypeface(null,1);LinearLayout.LayoutParams settingsTextParams=new LinearLayout.LayoutParams(-2,-2);settingsTextParams.leftMargin=dp(6);settings.addView(settingsText,settingsTextParams);LinearLayout.LayoutParams settingsParams=new LinearLayout.LayoutParams(-2,dp(44));settingsParams.setMargins(dp(4),0,dp(6),0);top.addView(settings,settingsParams);settings.setOnClickListener(v->{tap(v);showSettings();});
        TextView refresh=label("⟳",28,text);refresh.setGravity(Gravity.CENTER);refresh.setContentDescription("Обновить каталог");refresh.setBackground(round(0x22ffffff,16));top.addView(refresh,new LinearLayout.LayoutParams(dp(44),dp(44)));refresh.setOnClickListener(v->{tap(v);refresh.animate().rotationBy(360f).setDuration(500).start();loadCatalog(true);});
        root.addView(top);
        
        MaterialCardView hero=new MaterialCardView(this);hero.setRadius(dp(24));hero.setCardElevation(0);hero.setStrokeWidth(dp(1));hero.setStrokeColor(0x30ffffff);hero.setCardBackgroundColor(0x1affffff);hero.setUseCompatPadding(false);hero.setContentPadding(dp(16),dp(14),dp(16),dp(14));LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(-1,-2);hp.setMargins(0,dp(12),0,dp(10));root.addView(hero,hp);
        LinearLayout heroBody=new LinearLayout(this);heroBody.setOrientation(1);hero.addView(heroBody);
        LinearLayout heroHead=new LinearLayout(this);heroHead.setGravity(Gravity.CENTER_VERTICAL);heroBody.addView(heroHead);
        ImageView tgIcon=new ImageView(this);GradientDrawable tgBg=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{0xff7c4dff,0xff3478f6});tgBg.setShape(GradientDrawable.OVAL);tgIcon.setBackground(tgBg);tgIcon.setImageDrawable(getDrawable(com.vstore.app.R.drawable.ic_telegram_white));tgIcon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);tgIcon.setPadding(dp(10),dp(10),dp(10),dp(10));heroHead.addView(tgIcon,new LinearLayout.LayoutParams(dp(44),dp(44)));
        TextView heroTitle=label("Канал в Telegram // VGRAM INSIDER",16,text);LinearLayout.LayoutParams htp=new LinearLayout.LayoutParams(0,-2,1);htp.leftMargin=dp(12);heroHead.addView(heroTitle,htp);
        TextView heroSub=label("Новости, обновления модов и плагины: @Vmods2",13,muted);LinearLayout.LayoutParams hsp=new LinearLayout.LayoutParams(-1,-2);hsp.topMargin=dp(10);heroBody.addView(heroSub,hsp);
        TextView telegram=label("Открыть Telegram-канал  ↗",14,lavender);LinearLayout.LayoutParams tlp=new LinearLayout.LayoutParams(-1,-2);tlp.topMargin=dp(4);heroBody.addView(telegram,tlp);hero.setOnClickListener(v->{tap(v);v.animate().scaleX(.985f).scaleY(.985f).setDuration(80).withEndAction(()->v.animate().scaleX(1f).scaleY(1f).setDuration(140).start()).start();openUrl("https://t.me/Vmods2");});telegram.setOnClickListener(v->openUrl("https://t.me/Vmods2"));
        TextView importApk=label("＋ Импортировать APK",14,lavender);importApk.setPadding(4,10,4,10);root.addView(importApk);importApk.setOnClickListener(v->{Intent pick=new Intent(Intent.ACTION_OPEN_DOCUMENT);pick.setType("application/vnd.android.package-archive");pick.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(pick,PICK_APK);});
        tabs=new LinearLayout(this);tabs.setOrientation(0);HorizontalScrollView hsv=new HorizontalScrollView(this);hsv.setHorizontalScrollBarEnabled(false);hsv.addView(tabs);root.addView(hsv);
        LinearLayout searchWrap=new LinearLayout(this);searchWrap.setGravity(Gravity.CENTER_VERTICAL);searchWrap.setPadding(dp(16),0,dp(12),0);searchWrap.setBackground(round(0x15ffffff,26));LinearLayout.LayoutParams swp=new LinearLayout.LayoutParams(-1,dp(52));swp.topMargin=dp(4);root.addView(searchWrap,swp);
        TextView magnifier=label("⌕",25,muted);magnifier.setGravity(Gravity.CENTER);searchWrap.addView(magnifier,new LinearLayout.LayoutParams(dp(28),-1));
        search=new EditText(this);search.setSingleLine(true);search.setTextSize(16);search.setHint("Найти VGRAM, VTok, мод...");search.setTextColor(text);search.setHintTextColor(0xff8e8a9f);search.setPadding(dp(8),0,dp(8),0);search.setBackgroundColor(Color.TRANSPARENT);searchWrap.addView(search,new LinearLayout.LayoutParams(0,-1,1));TextView clearSearch=label("×",25,muted);clearSearch.setGravity(Gravity.CENTER);clearSearch.setVisibility(View.GONE);searchWrap.addView(clearSearch,new LinearLayout.LayoutParams(dp(30),-1));clearSearch.setOnClickListener(v->{tap(v);search.setText("");search.requestFocus();});
        status=label("",12,muted);status.setVisibility(View.GONE);root.addView(status);
        ScrollView sc=new ScrollView(this);list=new LinearLayout(this);list.setOrientation(1);sc.addView(list);root.addView(sc,new LinearLayout.LayoutParams(-1,0,1));
        catalogRecycler=new RecyclerView(this);catalogRecycler.setLayoutManager(new LinearLayoutManager(this));catalogRecycler.setNestedScrollingEnabled(false);catalogAdapter=new VStoreAdapter(this,item->{showAppDetails(item.toJson());},item->{download(item.toJson());},progress);catalogRecycler.setAdapter(catalogAdapter);
        setContentView(root); boolean reduce=getSharedPreferences(PREF_UI,MODE_PRIVATE).getBoolean("reduce_motion",false); if(animationDuration>0&&!reduce){root.setAlpha(0f);root.setTranslationY(dp(22));root.setScaleX(.985f);root.setScaleY(.985f);root.animate().alpha(1f).translationY(0f).scaleX(1f).scaleY(1f).setInterpolator(new android.view.animation.DecelerateInterpolator(1.6f)).setDuration(animationDuration+180L).start(); animateEntrance(top,0); animateEntrance(hero,70); animateEntrance(importApk,110); animateEntrance(hsv,150); animateEntrance(searchWrap,190);}
        search.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int count,int after){}public void onTextChanged(CharSequence s,int st,int before,int count){clearSearch.setVisibility(s.length()>0?View.VISIBLE:View.GONE);render();}public void afterTextChanged(android.text.Editable e){}});
        File dir=new File(getFilesDir(),"library");if(dir.exists()){File[] fs=dir.listFiles((d,n)->n.toLowerCase(Locale.ROOT).endsWith(".apk"));if(fs!=null)localApps.addAll(Arrays.asList(fs));}
        makeTabs();loadCached();loadCatalog(false);
        headerTitle.setOnLongClickListener(v->{showCreatorTools();return true;});
        logCurrentCertificateSha256();
        } catch (Throwable startupError) {
            Log.e("VStoreStartup", "Startup initialization failed", startupError);
            LinearLayout fallback = new LinearLayout(this);
            fallback.setOrientation(LinearLayout.VERTICAL);
            fallback.setGravity(Gravity.CENTER);
            fallback.setPadding(32, 32, 32, 32);
            fallback.setBackgroundColor(0xff0a0910);
            TextView title = new TextView(this);
            title.setText("V Store не смог запуститься");
            title.setTextColor(0xfff6f0f8);
            title.setTextSize(22);
            title.setGravity(Gravity.CENTER);
            fallback.addView(title, new LinearLayout.LayoutParams(-1, -2));
            TextView details = new TextView(this);
            String detail = startupError.getClass().getSimpleName();
            if (startupError.getMessage() != null && !startupError.getMessage().trim().isEmpty()) {
                detail += ": " + startupError.getMessage();
            }
            details.setText("Ошибка инициализации: " + detail + "\\nПодробности записаны в журнал VStoreStartup.");
            details.setTextColor(0xffb9afc0);
            details.setTextSize(14);
            details.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(-1, -2);
            dp.topMargin = 20;
            fallback.addView(details, dp);
            TextView retry = new TextView(this);
            retry.setText("Повторить запуск");
            retry.setTextColor(0xffffffff);
            retry.setTextSize(15);
            retry.setGravity(Gravity.CENTER);
            retry.setPadding(20, 14, 20, 14);
            retry.setBackground(round(0xff7c4dff, 18));
            LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-1, -2);
            rp.topMargin = 24;
            fallback.addView(retry, rp);
            retry.setOnClickListener(v -> recreate());
            setContentView(fallback);
        }
    }

    private void animateEntrance(View view,long delay){if(view==null)return;view.setAlpha(0f);view.setTranslationY(dp(18));view.animate().alpha(1f).translationY(0f).setStartDelay(delay).setDuration(Math.max(180,animationDuration)).setInterpolator(new android.view.animation.DecelerateInterpolator(1.5f)).start();}

    private void loadAppearance(){
        android.content.SharedPreferences p=getSharedPreferences(PREF_UI,MODE_PRIVATE);
        String theme=p.getString("theme","dark");
        if("system".equals(theme)){int night=getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;if(night==android.content.res.Configuration.UI_MODE_NIGHT_NO){bg=0xfff7f1f8;panel=0xffeee6f1;text=0xff241d29;muted=0xff6e6374;lavender=0xff8050a0;}}
        boolean dynamic=p.getBoolean("dynamic",false)&&Build.VERSION.SDK_INT>=31;
        if("light".equals(theme)){bg=0xfff7f1f8;panel=0xffeee6f1;text=0xff241d29;muted=0xff6e6374;lavender=0xff8050a0;}
        if(dynamic){try{bg=getColor(android.R.color.system_neutral1_900);panel=getColor(android.R.color.system_neutral1_800);text=getColor(android.R.color.system_neutral1_50);lavender=getColor(android.R.color.system_accent1_200);muted=getColor(android.R.color.system_neutral2_200);}catch(Exception ignored){}}
        String accent=p.getString("accent","lavender");
        if("blue".equals(accent)){lavender=0xff9ccaff;}else if("green".equals(accent)){lavender=0xffa6dfb0;}else if("pink".equals(accent)){lavender=0xffffb6d5;}else if("custom".equals(accent)){lavender=p.getInt("custom_accent",0xff7c4dff);}
        animationDuration=p.getInt("animation_ms",260);
    }
    private void showCustomColorDialog(){
        android.content.SharedPreferences prefs=getSharedPreferences(PREF_UI,MODE_PRIVATE);
        int current=prefs.getInt("custom_accent",lavender);
        LinearLayout box=new LinearLayout(this);box.setOrientation(1);box.setPadding(dp(20),dp(12),dp(20),dp(4));
        TextView preview=label("ПРЕДПРОСМОТР  ●  VStore",18,text);preview.setGravity(Gravity.CENTER);preview.setPadding(0,dp(18),0,dp(18));preview.setBackground(round(current,20));box.addView(preview,new LinearLayout.LayoutParams(-1,dp(64)));
        EditText hex=new EditText(this);hex.setSingleLine(true);hex.setHint("#7C4DFF или #RRGGBB");hex.setText(String.format(Locale.ROOT,"#%06X",(0xFFFFFF&current)));hex.setInputType(android.text.InputType.TYPE_CLASS_TEXT);LinearLayout.LayoutParams ep=new LinearLayout.LayoutParams(-1,-2);ep.topMargin=dp(10);box.addView(hex,ep);
        TextView rgb=label(String.format(Locale.ROOT,"RGB: %d, %d, %d",Color.red(current),Color.green(current),Color.blue(current)),13,muted);box.addView(rgb);
        hex.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int count,int after){}public void onTextChanged(CharSequence s,int st,int before,int count){try{int color=Color.parseColor(s.toString().trim().startsWith("#")?s.toString().trim():"#"+s.toString().trim());preview.setBackground(round(color,20));rgb.setText(String.format(Locale.ROOT,"RGB: %d, %d, %d",Color.red(color),Color.green(color),Color.blue(color)));}catch(Exception ignored){}}public void afterTextChanged(android.text.Editable e){}});
        new AlertDialog.Builder(this).setTitle("Свой цвет").setMessage("Введи HEX: #RRGGBB или #AARRGGBB").setView(box).setNegativeButton("Отмена",null).setNeutralButton("Сбросить", (d,w)->{prefs.edit().remove("custom_accent").putString("accent","lavender").apply();recreate();}).setPositiveButton("Применить",(d,w)->{try{String raw=hex.getText().toString().trim();int color=Color.parseColor(raw.startsWith("#")?raw:"#"+raw);prefs.edit().putInt("custom_accent",color).putString("accent","custom").apply();recreate();}catch(Exception e){toast("Неверный цвет. Пример: #7C4DFF");}}).show();
    }
    private void showSettings(){
        final int surface=0xff2b2730, iconSurface=0xff51445d, fg=0xffeee7f1, secondary=0xffc4bac9, accent=0xffe0b0ff;
        final android.content.SharedPreferences prefs=getSharedPreferences(PREF_UI,MODE_PRIVATE);
        LinearLayout page=new LinearLayout(this);page.setOrientation(1);page.setBackgroundColor(0xff17111a);
        LinearLayout toolbar=new LinearLayout(this);toolbar.setGravity(Gravity.CENTER_VERTICAL);toolbar.setPadding(dp(18),dp(12),dp(18),dp(12));
        TextView back=label("‹",38,fg);back.setGravity(Gravity.CENTER);toolbar.addView(back,new LinearLayout.LayoutParams(dp(48),dp(52)));
        TextView title=label("Настройки",27,fg);LinearLayout.LayoutParams titleLp=new LinearLayout.LayoutParams(-1,-2);titleLp.leftMargin=dp(12);toolbar.addView(title,titleLp);page.addView(toolbar);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(false);scroll.setClipToPadding(false);scroll.setPadding(dp(16),dp(10),dp(16),dp(24));
        LinearLayout body=new LinearLayout(this);body.setOrientation(1);scroll.addView(body);page.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        addSettingsSection(body,"Внешний вид",surface,accent);
        addSettingsRow(body,"文","Язык","Как в системе",surface,iconSurface,fg,secondary,null,()->new AlertDialog.Builder(this).setTitle("Язык").setMessage("Язык V Store пока определяется настройками системы.").setPositiveButton("ОК",null).show());
        addSettingsRow(body,"☾","Тема", "Как в системе".equals(themeName(prefs.getString("theme","dark")))? "Как в системе":themeName(prefs.getString("theme","dark")),surface,iconSurface,fg,secondary,null,()->new AlertDialog.Builder(this).setTitle("Тема").setSingleChoiceItems(new String[]{"Как в системе","Тёмная","Светлая"},new String[]{"system","dark","light"}.length>0?("system".equals(prefs.getString("theme","dark"))?0:"dark".equals(prefs.getString("theme","dark"))?1:2):0,(d,w)->{prefs.edit().putString("theme",new String[]{"system","dark","light"}[w]).apply();d.dismiss();recreate();}).setNegativeButton("Отмена",null).show());
        if(Build.VERSION.SDK_INT>=31)addSettingsSwitch(body,"●","Цвета телефона","Брать палитру из обоев (Android 12 и новее)",surface,iconSurface,fg,secondary,"dynamic",false);
        else addSettingsRow(body,"●","Акцентный цвет","Выбрать цвет интерфейса",surface,iconSurface,fg,secondary,null,()->showCustomColorDialog());
        addSettingsRow(body,"◉","Акцентный цвет","Палитра и свой HEX/RGB",surface,iconSurface,fg,secondary,null,()->showCustomColorDialog());
        addSettingsSection(body,"Обновления",surface,accent);
        addSettingsSwitch(body,"♧","Уведомлять об обновлениях","Проверять новые версии приложений",surface,iconSurface,fg,secondary,"notify_updates",true);
        addSettingsRow(body,"⟳","Проверить сейчас","Проверить каталог приложений",surface,iconSurface,fg,secondary,null,()->{loadCatalog(true);toast("Проверяем обновления…");});
        addSettingsSection(body,"Интерфейс",surface,accent);
        addSettingsSwitch(body,"◇","Жидкое стекло","Полупрозрачные карточки и тонкая обводка",surface,iconSurface,fg,secondary,"glass",true);
        addSettingsSwitch(body,"▤","Компактный каталог","Уменьшить размер карточек",surface,iconSurface,fg,secondary,"compact",false);
        addSettingsSwitch(body,"♧","Тактильный отклик","Виброотклик при нажатии",surface,iconSurface,fg,secondary,"haptics",true);
        addSettingsSwitch(body,"↝","Уменьшить анимации","Отключить плавные переходы",surface,iconSurface,fg,secondary,"reduce_motion",false);
        addSettingsSection(body,"Хранилище",surface,accent);
        addSettingsSwitch(body,"▣","Хранить оригинальный APK","Сохранять APK для следующих обновлений",surface,iconSurface,fg,secondary,"keep_original_apk",true);
        long used=folderSize(new File(getFilesDir(),"library"))+folderSize(getCacheDir());
        addSettingsRow(body,"▤","Очистить скачанное","Занято "+android.text.format.Formatter.formatShortFileSize(this,used),surface,iconSurface,fg,secondary,null,()->new AlertDialog.Builder(this).setTitle("Очистить скачанное?").setMessage("Удалить сохранённые APK и кэш? Это нельзя отменить.").setNegativeButton("Отмена",null).setPositiveButton("Очистить",(d,w)->{File library=new File(getFilesDir(),"library");deleteContents(library);deleteContents(getCacheDir());localApps.clear();toast("Скачанные файлы очищены");render();showSettings();}).show());
        addSettingsSection(body,"Аккаунт",surface,accent);
        addSettingsRow(body,"●","Войти или создать аккаунт","Аккаунт V Store",surface,iconSurface,fg,secondary,null,()->new AlertDialog.Builder(this).setTitle("Аккаунт V Store").setMessage("Вход в аккаунт пока недоступен в этой версии.").setPositiveButton("Понятно",null).show());
        addSettingsSection(body,"Магазин",surface,accent);
        addSettingsSwitch(body,"!","Показывать непроверенные приложения","Отдельная вкладка с приложениями на проверке",surface,iconSurface,fg,secondary,"show_unverified",false);
        addSettingsRow(body,"＋","Импортировать APK из файла","Выбрать APK на устройстве",surface,iconSurface,fg,secondary,null,()->{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("application/vnd.android.package-archive");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,PICK_APK);});
        addSettingsSection(body,"О приложении",surface,accent);
        addSettingsRow(body,"i","V Store 1.0.0","Каталог приложений и APK · @Vmods2",surface,iconSurface,fg,secondary,null,()->openUrl("https://t.me/Vmods2"));
        LinearLayout footer=new LinearLayout(this);footer.setGravity(Gravity.CENTER);TextView done=label("Закрыть настройки",15,0xffffffff);done.setGravity(Gravity.CENTER);done.setBackground(round(0xff59366b,24));done.setPadding(dp(22),dp(13),dp(22),dp(13));footer.addView(done,new LinearLayout.LayoutParams(-1,-2));LinearLayout.LayoutParams fp=new LinearLayout.LayoutParams(-1,-2);fp.setMargins(0,dp(18),0,dp(8));body.addView(footer,fp);done.setOnClickListener(v->recreate());
        AlertDialog dialog=new AlertDialog.Builder(this).setView(page).create();
        back.setOnClickListener(v->dialog.dismiss());
        dialog.setOnShowListener(x->{android.view.Window w=dialog.getWindow();if(w!=null){w.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(0xff17111a));w.setDimAmount(0f);w.setStatusBarColor(0xff17111a);w.setNavigationBarColor(0xff17111a);w.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);w.setFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS,android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);w.setLayout(-1,-1);if(Build.VERSION.SDK_INT>=31){try{w.setBackgroundBlurRadius(dp(18));}catch(Exception ignored){}}}});
        dialog.show();dialog.getWindow().setLayout(-1,-1);
    }
    private String themeName(String theme){return "light".equals(theme)?"Светлая":"dark".equals(theme)?"Тёмная":"Как в системе";}
    private void addSettingsSection(LinearLayout body,String name,int surface,int accent){TextView heading=label(name,15,0xffe0b0ff);heading.setTypeface(null,1);heading.setPadding(dp(14),dp(20),dp(8),dp(12));body.addView(heading);}
    private void addSettingsRow(LinearLayout body,String glyph,String name,String subtitle,int surface,int iconSurface,int fg,int secondary,String unused,Runnable action){
        LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(15),dp(14),dp(15),dp(14));row.setBackground(round(surface,22));LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,-2);rp.setMargins(0,0,0,dp(3));body.addView(row,rp);
        TextView icon=label(glyph,23,0xfff0d9ff);icon.setGravity(Gravity.CENTER);GradientDrawable ib=new GradientDrawable();ib.setColor(iconSurface);ib.setShape(GradientDrawable.OVAL);icon.setBackground(ib);row.addView(icon,new LinearLayout.LayoutParams(dp(56),dp(56)));
        LinearLayout textBox=new LinearLayout(this);textBox.setOrientation(1);textBox.setPadding(dp(15),0,0,0);row.addView(textBox,new LinearLayout.LayoutParams(0,-2,1));
        TextView heading=label(name,17,fg);textBox.addView(heading);if(subtitle!=null&&!subtitle.isEmpty()){TextView sub=label(subtitle,13,secondary);sub.setPadding(0,dp(3),0,0);textBox.addView(sub);}
        row.setOnClickListener(v->{tap(v);if(action!=null)action.run();});
    }
    private void addSettingsSwitch(LinearLayout body,String glyph,String name,String subtitle,int surface,int iconSurface,int fg,int secondary,String key,boolean defaultValue){
        LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(15),dp(14),dp(15),dp(14));row.setBackground(round(surface,22));LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,-2);rp.setMargins(0,0,0,dp(3));body.addView(row,rp);
        TextView icon=label(glyph,23,0xfff0d9ff);icon.setGravity(Gravity.CENTER);GradientDrawable ib=new GradientDrawable();ib.setColor(iconSurface);ib.setShape(GradientDrawable.OVAL);icon.setBackground(ib);row.addView(icon,new LinearLayout.LayoutParams(dp(56),dp(56)));
        LinearLayout textBox=new LinearLayout(this);textBox.setOrientation(1);textBox.setPadding(dp(15),0,dp(8),0);row.addView(textBox,new LinearLayout.LayoutParams(0,-2,1));
        TextView heading=label(name,16,fg);textBox.addView(heading);TextView sub=label(subtitle,13,secondary);sub.setPadding(0,dp(3),0,0);textBox.addView(sub);
        Switch toggle=new Switch(this);toggle.setButtonTintList(null);toggle.setChecked(getSharedPreferences(PREF_UI,MODE_PRIVATE).getBoolean(key,defaultValue));toggle.setOnCheckedChangeListener((button,checked)->{getSharedPreferences(PREF_UI,MODE_PRIVATE).edit().putBoolean(key,checked).apply();if("reduce_motion".equals(key)&&checked)getSharedPreferences(PREF_UI,MODE_PRIVATE).edit().putInt("animation_ms",0).apply();});row.addView(toggle);row.setOnClickListener(v->toggle.setChecked(!toggle.isChecked()));
    }
    private long folderSize(File f){if(f==null||!f.exists())return 0;if(f.isFile())return f.length();long total=0;File[] files=f.listFiles();if(files!=null)for(File child:files)total+=folderSize(child);return total;}
    private void deleteContents(File f){if(f==null||!f.exists())return;if(f.isDirectory()){File[] files=f.listFiles();if(files!=null)for(File child:files)deleteContents(child);}f.delete();}
    private void makeTabs(){tabs.removeAllViews();String[] names={"Все проекты","Мои приложения","Плагины & Моды","Эксклюзив"};for(String n:names){TextView t=label(n,14,n.equals(activeTab)?0xffffffff:text);t.setGravity(Gravity.CENTER);t.setPadding(dp(17),dp(11),dp(17),dp(11));t.setBackground(round(n.equals(activeTab)?lavender:panel,18));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-2,-2);p.setMargins(0,dp(3),dp(8),dp(7));tabs.addView(t,p);t.setOnClickListener(v->{tap(v);activeTab=n;makeTabs();render();if(!getSharedPreferences(PREF_UI,MODE_PRIVATE).getBoolean("reduce_motion",false)){t.setScaleX(.94f);t.setScaleY(.94f);t.animate().scaleX(1f).scaleY(1f).alpha(1f).setInterpolator(new android.view.animation.OvershootInterpolator(1.2f)).setDuration(220).start();}});}}
    private void tap(View v){if(getSharedPreferences(PREF_UI,MODE_PRIVATE).getBoolean("haptics",true))v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);if(animationDuration>0&&!getSharedPreferences(PREF_UI,MODE_PRIVATE).getBoolean("reduce_motion",false)){v.animate().cancel();v.setScaleX(.97f);v.setScaleY(.97f);v.animate().scaleX(1f).scaleY(1f).setInterpolator(new android.view.animation.OvershootInterpolator(1.4f)).setDuration(Math.min(220,Math.max(100,animationDuration))).start();}}
    private void entrance(View v,long delay){if(v==null||animationDuration<=0||getSharedPreferences(PREF_UI,MODE_PRIVATE).getBoolean("reduce_motion",false))return;v.setAlpha(0f);v.setTranslationY(dp(14));v.animate().alpha(1f).translationY(0f).setStartDelay(delay).setDuration(Math.max(160,animationDuration)).setInterpolator(new android.view.animation.DecelerateInterpolator(1.5f)).start();}
    private int dp(float value){return (int)(value*getResources().getDisplayMetrics().density+0.5f);}
    private GradientDrawable round(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));d.setStroke(dp(1),0x33ffffff);return d;}
    private TextView label(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setPadding(0,3,0,3);return t;}
    private void loadCached(){String raw=getPreferences(0).getString("catalog_json","");if(!raw.isEmpty())try{applyCatalog(new JSONObject(raw),false);}catch(Exception ignored){}if(apps.isEmpty())loadBundledDefaults();}
    private JSONObject bundledVtok(){try{JSONObject t=new JSONObject();t.put("id","vtok");t.put("name","VTok");t.put("category","Clients");t.put("version","47.2.41");t.put("badge","LATEST");t.put("description","VTok — TikTok-мод для Android, версия 47.2.41 arm64-v8a.");t.put("download_url","https://github.com/ramzilzokirov-lgtm/Vtok/releases/download/Main/TikTok_v47.2.41_arm64-v8a.1.apk");t.put("package_name","");t.put("sha256","4bcad2a15ac82da68d373629a9520f855781c3ec46f0e2937e605040285ba75c");return t;}catch(Exception e){return new JSONObject();}}
    private void loadBundledDefaults(){try{JSONObject root=new JSONObject();JSONArray a=new JSONArray();JSONObject v=new JSONObject();v.put("id","vgram_main");v.put("name","VGRAM Main");v.put("category","Clients");v.put("version","0.1.370");v.put("badge","LATEST");v.put("description","Основной клиент VGRAM. APK опубликован в GitHub Releases.");v.put("download_url","https://github.com/ramzilzokirov-lgtm/VGRAM-Main/releases/download/v0.1.370/VGRAM-debug.apk");v.put("package_name","org.telegram.messenger");v.put("sha256","0cae0938c34948b97319dd8fcf55077d932ef2a00c5963216d2606f1741ac3a2");a.put(v);a.put(bundledVtok());root.put("apps",a);applyCatalog(root,false);}catch(Exception e){Log.e("VStore","Bundled catalog failed",e);}}
    private void loadCatalog(boolean force){
        status.setText("");
        pool.execute(()->{
            try {
                HttpURLConnection conn=(HttpURLConnection)new URL(API+"/api/apps"+(force?"?v="+System.currentTimeMillis():"")).openConnection();
                conn.setConnectTimeout(7000);conn.setReadTimeout(12000);conn.setUseCaches(!force);
                String raw=readText(conn.getInputStream());JSONArray remote=new JSONArray(raw);JSONObject json=new JSONObject();JSONArray normalized=new JSONArray();
                for(int i=0;i<remote.length();i++){JSONObject src=remote.optJSONObject(i);if(src==null)continue;JSONObject item=new JSONObject();
                    item.put("id",src.optString("id"));item.put("name",src.optString("name","Приложение"));item.put("category","Apps");item.put("version",src.optString("version_name",""));item.put("description",src.optString("description",""));item.put("package_name",src.optString("package_name",""));item.put("sha256",src.optString("sha256",""));item.put("download_url",API+src.optString("download_url",""));item.put("creator_id",src.optString("creator_id",""));normalized.put(item);}
                json.put("apps",normalized);String cached=json.toString();getPreferences(0).edit().putString("catalog_json",cached).apply();runOnUiThread(()->applyCatalog(json,true));
            }catch(Exception apiError){
                try{HttpURLConnection conn=(HttpURLConnection)new URL(CATALOG_URL+(force?"?v="+System.currentTimeMillis():"")).openConnection();conn.setConnectTimeout(8000);conn.setReadTimeout(12000);conn.setUseCaches(!force);String raw=readText(conn.getInputStream());JSONObject json=new JSONObject(raw);getPreferences(0).edit().putString("catalog_json",raw).apply();runOnUiThread(()->{status.setText("");applyCatalog(json,true);});}
                catch(Exception fallbackError){runOnUiThread(()->{status.setText("");if(apps.isEmpty())loadBundledDefaults();render();});}
            }
        });
    }
    private void applyCatalog(JSONObject json,boolean fresh){JSONArray a=json.optJSONArray("apps");if(a==null)return;ArrayList<JSONObject> filtered=new ArrayList<>();boolean hasVtok=false;for(int i=0;i<a.length();i++){JSONObject item=a.optJSONObject(i);if(item==null)continue;String id=item.optString("id","").toLowerCase(Locale.ROOT);String name=item.optString("name","").toLowerCase(Locale.ROOT);if(id.contains("vstt")||name.equals("vstt"))continue;try{if(id.contains("vgram")||name.contains("vgram")){item.put("id","vgram_main");item.put("name","VGRAM Main");filtered.add(item);}else if(id.equals("vtok")||name.equals("vtok")||name.contains("tiktok")){item.put("id","vtok");item.put("name","VTok");filtered.add(item);hasVtok=true;}}catch(org.json.JSONException ignored){}}if(!hasVtok)filtered.add(bundledVtok());if(filtered.isEmpty())return;synchronized(apps){apps.clear();apps.addAll(filtered);}runOnUiThread(()->{status.setText("");render();});}
    private void render(){
        if(list==null)return;
        if(catalogRecycler!=null)catalogRecycler.setPadding(0,0,0,dp(12));
        list.removeAllViews();
        String q=search.getText().toString().toLowerCase(Locale.ROOT);
        ArrayList<JSONObject> snapshot;
        synchronized(apps){snapshot=new ArrayList<>(apps);}
        ArrayList<VStoreAppItem> visible=new ArrayList<>();
        for(JSONObject a:snapshot){
            String category=a.optString("category","Apps");
            if(activeTab.equals("Плагины & Моды")&&!category.toLowerCase(Locale.ROOT).matches(".*(plugin|mod|плагин|мод).*"))continue;
            if(activeTab.equals("Эксклюзив")&&!a.optBoolean("exclusive",false))continue;
            if(activeTab.equals("Мои приложения")&&!a.optString("creator_id","8436409687").equals("8436409687"))continue;
            String name=a.optString("name","Приложение");
            if(!name.toLowerCase(Locale.ROOT).contains(q)&&!a.optString("description","").toLowerCase(Locale.ROOT).contains(q)&&!a.optString("category","").toLowerCase(Locale.ROOT).contains(q)&&!a.optString("version","").toLowerCase(Locale.ROOT).contains(q)&&!a.optString("id","").toLowerCase(Locale.ROOT).contains(q))continue;
            visible.add(VStoreAppItem.fromJson(a));
        }
        catalogAdapter.submitList(visible);
        if(catalogAdapter!=null)catalogAdapter.notifyDataSetChanged(); // progress map changes independently of ListAdapter item contents
        if(!visible.isEmpty()){catalogRecycler.setVisibility(View.VISIBLE);list.addView(catalogRecycler,new LinearLayout.LayoutParams(-1,-2));}else{catalogRecycler.setVisibility(View.GONE);}
        int count=visible.size();
        if(activeTab.equals("Мои приложения"))for(File f:localApps)if(f.getName().toLowerCase(Locale.ROOT).contains(q)){localCard(f);count++;}
        if(count==0){
            if(snapshot.isEmpty()) loadBundledDefaults();
            TextView empty=label(snapshot.isEmpty()?"Загружаем каталог…":"По этому фильтру ничего не найдено.",15,muted);
            empty.setGravity(Gravity.CENTER);empty.setPadding(20,48,20,48);list.addView(empty);
        }
    }
    private LinearLayout card(){LinearLayout c=new LinearLayout(this);c.setOrientation(1);boolean glass=getSharedPreferences(PREF_UI,MODE_PRIVATE).getBoolean("glass",true);c.setPadding(15,13,15,13);c.setBackground(glass?getDrawable(com.vstore.app.R.drawable.bg_glass_card):round(panel,24));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,7,0,7);addSafe(list,c,p);if(animationDuration>0&&!getSharedPreferences(PREF_UI,MODE_PRIVATE).getBoolean("reduce_motion",false)){c.setAlpha(0f);c.setTranslationY(dp(20));c.setScaleX(.98f);c.setScaleY(.98f);c.animate().alpha(1f).translationY(0f).scaleX(1f).scaleY(1f).setInterpolator(new android.view.animation.DecelerateInterpolator(1.4f)).setDuration(animationDuration+80L).start();}return c;}
    private void addSafe(ViewGroup parent,View child,ViewGroup.LayoutParams params){if(child.getParent()!=null){ViewParent old=child.getParent();if(old instanceof ViewGroup)((ViewGroup)old).removeView(child);}parent.addView(child,params);}
    private void appCard(JSONObject a){LinearLayout c=card();LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);c.addView(row);
        ImageView icon=new ImageView(this);icon.setImageDrawable(getDrawable(android.R.drawable.sym_def_app_icon));icon.setBackground(round(0x26ffffff,16));icon.setClipToOutline(true);row.addView(icon,new LinearLayout.LayoutParams(dp(56),dp(56)));
        String iconUrl=a.optString("icon_url","");if(!iconUrl.isEmpty())loadIcon(iconUrl,icon);
        LinearLayout col=new LinearLayout(this);col.setOrientation(1);col.setPadding(12,0,0,0);row.addView(col,new LinearLayout.LayoutParams(0,-2,1));
        LinearLayout nameRow=new LinearLayout(this);nameRow.setGravity(Gravity.CENTER_VERTICAL);col.addView(nameRow);TextView name=label(a.optString("name","Project"),16,text);name.setTypeface(null,1);nameRow.addView(name);String badge=a.optString("badge","");if(!badge.isEmpty()){TextView b=label(" "+badge+" ",10,bg);b.setBackground(round(lavender,8));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-2,-2);p.setMargins(7,0,0,0);nameRow.addView(b,p);}
        TextView versionChip=label("v"+a.optString("version",""),11,text);versionChip.setPadding(8,3,8,3);versionChip.setBackground(round(0x26ffffff,12));nameRow.addView(versionChip);
        TextView description=label(a.optString("description",""),13,text);description.setMaxLines(2);description.setEllipsize(android.text.TextUtils.TruncateAt.END);description.setPadding(0,8,0,6);c.addView(description);c.setOnClickListener(v->showAppDetails(a));
        String id=a.optString("id");Integer pr=progress.get(id);if(pr!=null&&pr>=0&&pr<100){ProgressBar bar=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);bar.setMax(100);bar.setProgress(pr);c.addView(bar);c.addView(label("Загрузка "+pr+"%",11,lavender));}
        LinearLayout actions=new LinearLayout(this);actions.setGravity(Gravity.CENTER_VERTICAL);c.addView(actions);
        String pkg=a.optString("package_name","");boolean installed=!pkg.isEmpty()&&installed(pkg);String url=a.optString("download_url","");
        String action=installed?"ОБНОВИТЬ":(url.isEmpty()?"НЕТ APK":"СКАЧАТЬ");
        MaterialButton button=new MaterialButton(this);button.setText(action);button.setTextSize(12);button.setTypeface(null,1);button.setCornerRadius(dp(18));button.setMinHeight(dp(40));button.setMinimumHeight(dp(40));button.setPadding(dp(18),0,dp(18),0);button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(url.isEmpty()?0xff55515d:lavender));button.setTextColor(url.isEmpty()?0xffb7b1be:0xffffffff);actions.addView(button);button.setOnClickListener(v->{if(url.isEmpty()){toast("В разработке: APK ещё не опубликован.");return;}download(a);});
        
    }

    private void showAppDetails(JSONObject a){
        LinearLayout box=new LinearLayout(this);box.setOrientation(1);box.setPadding(20,8,20,4);
        String name=a.optString("name","Приложение");
        box.addView(label(a.optString("description","Описание отсутствует"),15,text));
        box.addView(label("Категория: "+a.optString("category","Приложения"),13,muted));
        box.addView(label("Версия: "+a.optString("version","не указана"),13,muted));
        box.addView(label("Пакет: "+a.optString("package_name","не указан"),13,muted));
        box.addView(label("Размер: "+(a.optLong("size",0)>0?String.format(Locale.ROOT,"%.1f МБ",a.optLong("size")/1048576f):"неизвестен"),13,muted));
        String hash=a.optString("sha256","");box.addView(label("SHA-256: "+(hash.isEmpty()?"не опубликован":hash),11,muted));
        new AlertDialog.Builder(this).setTitle(name).setView(box).setPositiveButton("Закрыть",null).show();
    }
    private void localCard(File f){LinearLayout c=card();LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);c.addView(row);ImageView icon=new ImageView(this);icon.setImageDrawable(apkIcon(f));row.addView(icon,new LinearLayout.LayoutParams(48,48));TextView name=label(apkLabel(f),16,text);name.setTypeface(null,1);name.setPadding(12,0,0,0);row.addView(name,new LinearLayout.LayoutParams(0,-2,1));TextView t=label("Локальный APK • "+String.format(Locale.ROOT,"%.1f MB",f.length()/1048576f)+" • SHA-256 "+shortHash(safeSha256(f)),12,muted);c.addView(t);LinearLayout actions=new LinearLayout(this);actions.setGravity(Gravity.CENTER_VERTICAL);c.addView(actions);TextView install=label("УСТАНОВИТЬ",13,bg);install.setGravity(Gravity.CENTER);install.setPadding(16,10,16,10);install.setBackground(round(lavender,18));actions.addView(install);install.setOnClickListener(v->install(f));TextView remove=label("  Удалить",13,muted);remove.setPadding(14,10,8,10);actions.addView(remove);remove.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Удалить APK?").setMessage(f.getName()).setNegativeButton("Отмена",null).setPositiveButton("Удалить",(d,w)->{if(f.delete())localApps.remove(f);render();}).show());}
    private String safeSha256(File f){try{return sha256(f);}catch(Exception e){return "ошибка";}}
    private String shortHash(String s){return s.isEmpty()?"—":s.substring(0,Math.min(12,s.length()))+"…";}
    private boolean installed(String pkg){try{getPackageManager().getPackageInfo(pkg,0);return true;}catch(Exception e){return false;}}
    private void loadIcon(String url,ImageView target){pool.execute(()->{try{HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();c.setConnectTimeout(5000);c.setReadTimeout(7000);Bitmap b=BitmapFactory.decodeStream(c.getInputStream());if(b!=null)runOnUiThread(()->target.setImageBitmap(b));}catch(Exception ignored){}});}
    private void download(JSONObject a){
        final String id=a.optString("id","app"),url=a.optString("download_url","");
        if(url.isEmpty()){toast("Ссылка на APK пока не опубликована");return;}
        synchronized(progress){if(progress.containsKey(id)){toast("Загрузка уже выполняется");return;}progress.put(id,0);}
        render();
        pool.execute(()->{
            File dir=getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
            File out=new File(dir==null?getFilesDir():dir,id+".apk"),part=new File(out.getAbsolutePath()+".part");
            HttpURLConnection conn=null;
            try{
                if(!url.startsWith("https://")&&!url.startsWith("http://"))throw new IOException("Некорректная ссылка");
                conn=(HttpURLConnection)new URL(url).openConnection();conn.setInstanceFollowRedirects(true);conn.setConnectTimeout(20000);conn.setReadTimeout(120000);conn.setRequestProperty("User-Agent","VStore-Android/1.0");conn.connect();
                int code=conn.getResponseCode();if(code<200||code>=300)throw new IOException("HTTP "+code);
                long len=conn.getContentLengthLong();long total=0;long lastUi=0;
                try(InputStream in=conn.getInputStream();OutputStream os=new FileOutputStream(part)){
                    byte[] b=new byte[32768];int n;
                    while((n=in.read(b))!=-1){os.write(b,0,n);total+=n;
                        if(len>0){int pct=(int)Math.min(99,total*100/len);synchronized(progress){progress.put(id,pct);}
                        }else{synchronized(progress){progress.put(id,0);}}
                        long now=android.os.SystemClock.elapsedRealtime();if(now-lastUi>180||n<0){lastUi=now;runOnUiThread(()->{if(!isFinishing())render();});}
                    }
                    os.flush();
                }
                if(total==0)throw new IOException("Сервер вернул пустой файл");
                String expected=a.optString("sha256","");if(!expected.isEmpty()&&!expected.equalsIgnoreCase(sha256(part)))throw new IOException("SHA-256 не совпал");
                if(out.exists()&&!out.delete())throw new IOException("Не удалось заменить старый APK");
                if(!part.renameTo(out))throw new IOException("Не удалось сохранить APK");
                synchronized(progress){progress.remove(id);}
                runOnUiThread(()->{if(!isFinishing()){render();toast("Загрузка завершена");install(out);}});
            }catch(Exception e){
                if(part.exists())part.delete();synchronized(progress){progress.remove(id);}
                String message=e.getMessage()==null?e.getClass().getSimpleName():e.getMessage();
                runOnUiThread(()->{if(!isFinishing()){render();toast("Ошибка загрузки: "+message);}});
            }finally{if(conn!=null)conn.disconnect();}
        });
    }
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
                    headerTitle.setText("VStore // Developer Hub");
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
    @Override protected void onActivityResult(int r,int result,Intent data){super.onActivityResult(r,result,data);if(r!=PICK_APK||result!=RESULT_OK||data==null||data.getData()==null)return;try{File dir=new File(getFilesDir(),"library");if(!dir.exists()&&!dir.mkdirs())throw new IOException("Не удалось создать библиотеку");String name="import_"+System.currentTimeMillis()+".apk";File f=new File(dir,name);try(InputStream in=getContentResolver().openInputStream(data.getData());OutputStream out=new FileOutputStream(f)){if(in==null)throw new IOException("Файл недоступен");byte[] b=new byte[8192];int n;long total=0;while((n=in.read(b))!=-1){total+=n;if(total>2L*1024L*1024L*1024L)throw new IOException("APK больше 2 ГБ");out.write(b,0,n);}}if(f.length()==0)throw new IOException("Выбран пустой файл");localApps.add(f);activeTab="Мои приложения";makeTabs();render();toast("APK добавлен в библиотеку: "+f.getName());}catch(Exception e){toast("Не удалось импортировать APK: "+e.getMessage());}}
    private String sha256(File f)throws Exception{MessageDigest md=MessageDigest.getInstance("SHA-256");try(InputStream in=new FileInputStream(f)){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)md.update(b,0,n);}StringBuilder s=new StringBuilder();for(byte b:md.digest())s.append(String.format(Locale.ROOT,"%02x",b));return s.toString();}
    private String readText(InputStream in)throws IOException{ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);return new String(out.toByteArray(),"UTF-8");}
    private void openUrl(String s){try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(s)));}catch(Exception e){toast("Ссылка недоступна");}}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
    @Override protected void onDestroy(){super.onDestroy();pool.shutdown();}
}