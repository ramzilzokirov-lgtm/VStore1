package com.vstore.app;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ProgressBar;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

public final class VStoreAdapter extends ListAdapter<VStoreAppItem,VStoreAdapter.Holder> {
    public interface ItemAction { void onAction(VStoreAppItem item); }
    private final Context context;
    private final ItemAction onDetails, onDownload;
    private final Map<String,Integer> progress;
    private final Set<String> entranceAnimatedIds=java.util.Collections.synchronizedSet(new HashSet<String>());
    private final int textColor=0xfff4f0e6, mutedColor=0xffc6c0b2, accent=0xffd6b45c;

    public VStoreAdapter(Context context, ItemAction onDetails, ItemAction onDownload, Map<String,Integer> progress) {
        super(DIFF);
        this.context=context; this.onDetails=onDetails; this.onDownload=onDownload; this.progress=progress;
    }
    private static final DiffUtil.ItemCallback<VStoreAppItem> DIFF=new DiffUtil.ItemCallback<VStoreAppItem>() {
        @Override public boolean areItemsTheSame(@NonNull VStoreAppItem a,@NonNull VStoreAppItem b){return a.id.equals(b.id);}
        @Override public boolean areContentsTheSame(@NonNull VStoreAppItem a,@NonNull VStoreAppItem b){return a.equals(b);}
    };

    @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent,int type) {
        MaterialCardView card=new MaterialCardView(context);
        boolean glass=context.getSharedPreferences("vstore_ui",Context.MODE_PRIVATE).getBoolean("glass",true);
        boolean compact=context.getSharedPreferences("vstore_ui",Context.MODE_PRIVATE).getBoolean("compact",false);
        card.setRadius(dp(24)); card.setCardElevation(glass?dp(2):0); card.setStrokeWidth(glass?dp(1):0);
        card.setStrokeColor(0x33d6b45c); card.setCardBackgroundColor(glass?0x1affffff:0xff1b1b1e);
        if(!context.getSharedPreferences("vstore_ui",Context.MODE_PRIVATE).getBoolean("reduce_motion",false)){card.setAlpha(0f);card.setTranslationY(dp(22));card.setScaleX(.975f);card.setScaleY(.975f);}
        RecyclerView.LayoutParams cp=new RecyclerView.LayoutParams(-1,-2);
        cp.setMargins(0,dp(7),0,dp(7)); card.setLayoutParams(cp);
        LinearLayout body=new LinearLayout(context); body.setOrientation(LinearLayout.VERTICAL); body.setPadding(dp(15),dp(13),dp(15),dp(13)); card.addView(body);
        LinearLayout row=new LinearLayout(context); row.setGravity(Gravity.CENTER_VERTICAL); body.addView(row);
        ImageView icon=new ImageView(context); icon.setImageResource(android.R.drawable.sym_def_app_icon);
        GradientDrawable iconBg=new GradientDrawable();iconBg.setColor(0x26d6b45c);iconBg.setCornerRadius(dp(16));icon.setBackground(iconBg);icon.setClipToOutline(true);
        row.addView(icon,new LinearLayout.LayoutParams(dp(56),dp(56)));
        LinearLayout col=new LinearLayout(context);col.setOrientation(LinearLayout.VERTICAL);col.setPadding(dp(12),0,0,0);
        row.addView(col,new LinearLayout.LayoutParams(0,-2,1));
        LinearLayout titleRow=new LinearLayout(context);titleRow.setGravity(Gravity.CENTER_VERTICAL);col.addView(titleRow);
        TextView title=label("",17,textColor);title.setTypeface(null,1);titleRow.addView(title,new LinearLayout.LayoutParams(0,-2,1));
        TextView version=label("",11,textColor);version.setPadding(dp(8),dp(3),dp(8),dp(3));
        GradientDrawable chip=new GradientDrawable();chip.setColor(0x26d6b45c);chip.setCornerRadius(dp(12));version.setBackground(chip);titleRow.addView(version);
        TextView description=label("",13,textColor);description.setMaxLines(2);description.setEllipsize(android.text.TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams descParams=new LinearLayout.LayoutParams(-1,-2);descParams.topMargin=dp(compact?5:9);body.addView(description,descParams);
        if(compact){icon.getLayoutParams().width=dp(44);icon.getLayoutParams().height=dp(44);description.setMaxLines(1);}
        ProgressBar progressBar=new ProgressBar(context,null,android.R.attr.progressBarStyleHorizontal);progressBar.setMax(100);progressBar.setIndeterminate(true);progressBar.setVisibility(View.GONE);LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(-1,dp(4));pp.topMargin=dp(8);body.addView(progressBar,pp);
        MaterialButton action=new MaterialButton(context);action.setCornerRadius(dp(18));action.setMinHeight(dp(40));action.setMinimumHeight(dp(40));
        action.setTextSize(12);action.setTypeface(null,1);action.setInsetTop(0);action.setInsetBottom(0);
        LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(-2,dp(40));ap.topMargin=dp(10);body.addView(action,ap);
        return new Holder(card,icon,title,version,description,action,progressBar);
    }
    @Override public void onBindViewHolder(@NonNull Holder h,int position) {
        VStoreAppItem item=getItem(position);
        h.title.setText(item.name);
        h.version.setText("v"+item.version);
        h.description.setText(item.description);
        if(item.id.toLowerCase(java.util.Locale.ROOT).contains("vgram")||item.name.toLowerCase(java.util.Locale.ROOT).contains("vgram"))h.icon.setImageResource(R.drawable.ic_vgram);else if(item.id.toLowerCase(java.util.Locale.ROOT).contains("vtok")||item.name.toLowerCase(java.util.Locale.ROOT).equals("vtok"))h.icon.setImageResource(R.drawable.ic_vtok);else try { if(item.packageName!=null&&!item.packageName.isEmpty()){android.content.pm.ApplicationInfo info=context.getPackageManager().getApplicationInfo(item.packageName,0);h.icon.setImageDrawable(context.getPackageManager().getApplicationIcon(info));} else h.icon.setImageResource(R.drawable.ic_app_default); } catch(Exception ignored){h.icon.setImageResource(R.drawable.ic_app_default);}
        boolean ready=item.downloadUrl!=null&&!item.downloadUrl.isEmpty();
        Integer pct=progress.get(item.id);boolean downloading=pct!=null;
        h.progressBar.setVisibility(downloading?View.VISIBLE:View.GONE);h.progressBar.setIndeterminate(downloading && pct.intValue()==0);if(pct!=null&&pct>0)h.progressBar.setProgress(pct);
        h.action.setText(downloading?(pct>0?"СКАЧИВАЕТСЯ "+pct+"%":"ПОДКЛЮЧЕНИЕ…"):(ready?"СКАЧАТЬ":"В РАЗРАБОТКЕ"));
        h.action.setEnabled(ready&&!downloading);
        h.action.setAlpha(ready?1f:0.58f);
        h.action.setBackgroundTintList(android.content.res.ColorStateList.valueOf(ready?accent:0xff55515d));
        h.action.setTextColor(ready?0xffffffff:0xffc7c2cc);
        h.card.setOnClickListener(v->{animatePress(v);onDetails.onAction(item);});
        h.action.setOnClickListener(v->{if(ready){animatePress(v);onDownload.onAction(item);}});
        boolean reduce=context.getSharedPreferences("vstore_ui",Context.MODE_PRIVATE).getBoolean("reduce_motion",false);
        if(!reduce&&entranceAnimatedIds.add(item.id)){
            int positionDelay=Math.min(360,Math.max(0,position)*70);
            h.card.animate().cancel();
            h.card.setAlpha(0f);h.card.setTranslationY(dp(22));h.card.setScaleX(.975f);h.card.setScaleY(.975f);
            h.card.animate().alpha(1f).translationY(0f).scaleX(1f).scaleY(1f)
                .setStartDelay(positionDelay).setDuration(420)
                .setInterpolator(new android.view.animation.PathInterpolator(.16f,1f,.3f,1f)).start();
            h.card.setElevation(dp(1));
        } else if(reduce) {
            h.card.animate().cancel();h.card.setAlpha(1f);h.card.setTranslationY(0f);h.card.setScaleX(1f);h.card.setScaleY(1f);
        }
    }
    private void animatePress(View v){if(context.getSharedPreferences("vstore_ui",Context.MODE_PRIVATE).getBoolean("haptics",true))v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);if(!context.getSharedPreferences("vstore_ui",Context.MODE_PRIVATE).getBoolean("reduce_motion",false)){v.animate().cancel();v.setScaleX(.97f);v.setScaleY(.97f);v.animate().scaleX(1f).scaleY(1f).setDuration(180).setInterpolator(new android.view.animation.OvershootInterpolator(1.3f)).start();}}
    private TextView label(String s,int size,int color){TextView t=new TextView(context);t.setText(s);t.setTextSize(size);t.setTextColor(color);return t;}
    private int dp(float n){return (int)(n*context.getResources().getDisplayMetrics().density+0.5f);}
    static final class Holder extends RecyclerView.ViewHolder {
        final MaterialCardView card;final ImageView icon;final TextView title,version,description;final MaterialButton action;final ProgressBar progressBar;
        Holder(MaterialCardView c,ImageView i,TextView t,TextView v,TextView d,MaterialButton a,ProgressBar p){super(c);card=c;icon=i;title=t;version=v;description=d;action=a;progressBar=p;}
    }
}