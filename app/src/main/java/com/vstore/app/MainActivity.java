package com.vstore.app;

import android.app.*;
import android.content.*;
import android.content.pm.PackageInstaller;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.io.*;
import java.security.MessageDigest;
import java.util.*;

public class MainActivity extends Activity {
    private final int PICK_APK = 41;
    private LinearLayout list;
    private EditText search;
    private final ArrayList<File> apps = new ArrayList<>();
    private int lavender = Color.rgb(215,185,232), bg = Color.rgb(23,17,25), panel = Color.rgb(39,30,43);
    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(bg); getWindow().setNavigationBarColor(bg);
        LinearLayout root = new LinearLayout(this); root.setOrientation(1); root.setPadding(20,20,20,12); root.setBackgroundColor(bg);
        TextView brand = new TextView(this); brand.setText("V Store"); brand.setTextColor(lavender); brand.setTextSize(30); brand.setTypeface(null,1); root.addView(brand);
        TextView subtitle = new TextView(this); subtitle.setText("Твоя библиотека приложений"); subtitle.setTextColor(0xffb7aebc); subtitle.setTextSize(14); root.addView(subtitle);
        search = new EditText(this); search.setSingleLine(true); search.setHint("Поиск приложений"); search.setTextColor(Color.WHITE); search.setHintTextColor(0xffa69aa9); search.setBackgroundTintList(android.content.res.ColorStateList.valueOf(lavender)); root.addView(search);
        Button add = new Button(this); add.setText("+  Добавить APK"); add.setTextColor(bg); add.setBackgroundTintList(android.content.res.ColorStateList.valueOf(lavender)); root.addView(add);
        ScrollView scroll = new ScrollView(this); list = new LinearLayout(this); list.setOrientation(1); scroll.addView(list); root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        setContentView(root);
        add.setOnClickListener(v -> { Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT); i.setType("application/vnd.android.package-archive"); i.addCategory(Intent.CATEGORY_OPENABLE); startActivityForResult(i,PICK_APK); });
        search.addTextChangedListener(new android.text.TextWatcher(){ public void beforeTextChanged(CharSequence s,int st,int c,int a){} public void onTextChanged(CharSequence s,int st,int before,int count){render();} public void afterTextChanged(android.text.Editable e){} });
        File dir = new File(getFilesDir(),"library"); if(dir.exists()){ File[] fs=dir.listFiles((d,n)->n.toLowerCase(Locale.ROOT).endsWith(".apk")); if(fs!=null) apps.addAll(Arrays.asList(fs)); } render();
    }
    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data) {
        super.onActivityResult(requestCode,resultCode,data); if(requestCode!=PICK_APK || resultCode!=RESULT_OK || data==null || data.getData()==null)return;
        Uri uri=data.getData(); File dir=new File(getFilesDir(),"library"); dir.mkdirs(); File out=new File(dir,"app_"+System.currentTimeMillis()+".apk");
        try(InputStream in=getContentResolver().openInputStream(uri); OutputStream os=new FileOutputStream(out)){ byte[] buf=new byte[8192]; int n; while((n=in.read(buf))>0)os.write(buf,0,n); apps.add(out); render(); Toast.makeText(this,"APK добавлен в библиотеку",Toast.LENGTH_SHORT).show(); }
        catch(Exception e){ Toast.makeText(this,"Не удалось импортировать APK: "+e.getMessage(),Toast.LENGTH_LONG).show(); }
    }
    private void render() {
        if(list==null)return; list.removeAllViews(); String q=search==null?"":search.getText().toString().toLowerCase(Locale.ROOT);
        if(apps.isEmpty()){ TextView empty=new TextView(this); empty.setText("\nВ библиотеке пока пусто.\n\nНажми «Добавить APK», чтобы импортировать приложение."); empty.setTextColor(0xffb7aebc); empty.setTextSize(16); empty.setGravity(Gravity.CENTER); empty.setPadding(8,50,8,50); list.addView(empty); return; }
        for(File f:new ArrayList<>(apps)){ if(!f.getName().toLowerCase(Locale.ROOT).contains(q))continue;
            LinearLayout card=new LinearLayout(this); card.setOrientation(1); card.setPadding(16,12,16,12); card.setBackgroundColor(panel); LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2); cp.setMargins(0,10,0,0); list.addView(card,cp);
            TextView name=new TextView(this); name.setText(f.getName().replace(".apk","")); name.setTextColor(Color.WHITE); name.setTextSize(17); name.setTypeface(null,1); card.addView(name);
            TextView meta=new TextView(this); meta.setText(String.format(Locale.ROOT,"APK • %.1f MB",f.length()/1048576f)); meta.setTextColor(0xffb7aebc); card.addView(meta);
            TextView hash=new TextView(this); hash.setText("SHA-256: "+sha256(f)); hash.setTextColor(0xffb7aebc); hash.setTextSize(10); card.addView(hash);
            LinearLayout actions=new LinearLayout(this); card.addView(actions);
            Button install=new Button(this); install.setText("Установить"); install.setTextColor(lavender); actions.addView(install); install.setOnClickListener(v->install(f));
            Button remove=new Button(this); remove.setText("Удалить"); remove.setTextColor(0xffffb4ab); actions.addView(remove); remove.setOnClickListener(v->{new AlertDialog.Builder(this).setMessage("Удалить APK из библиотеки?").setNegativeButton("Отмена",null).setPositiveButton("Удалить",(d,w)->{f.delete();apps.remove(f);render();}).show();});
        }
    }
    private String sha256(File f){ try{MessageDigest md=MessageDigest.getInstance("SHA-256");try(InputStream in=new FileInputStream(f)){byte[] b=new byte[8192];int n;while((n=in.read(b))>0)md.update(b,0,n);}StringBuilder s=new StringBuilder();for(byte x:md.digest())s.append(String.format(Locale.ROOT,"%02x",x));return s.toString();}catch(Exception e){return "недоступен";} }
    private void install(File apk) {
        try { PackageInstaller pi=getPackageManager().getPackageInstaller(); PackageInstaller.SessionParams p=new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL); int id=pi.createSession(p);
            try(PackageInstaller.Session session=pi.openSession(id); InputStream in=new FileInputStream(apk); OutputStream out=session.openWrite("base.apk",0,apk.length())){byte[] b=new byte[65536];int n;while((n=in.read(b))>0)out.write(b,0,n);session.fsync(out);}
            Intent callback=new Intent(this,InstallResultReceiver.class); callback.setAction("com.vstore.app.INSTALL_RESULT"); PendingIntent pending=PendingIntent.getBroadcast(this,id,callback,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_MUTABLE); pi.openSession(id).commit(pending.getIntentSender());
        } catch(Exception e){Toast.makeText(this,"Не удалось начать установку: "+e.getMessage(),Toast.LENGTH_LONG).show();}
    }
}