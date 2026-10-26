package com.vstore.app;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
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
    private final int textColor=0xfff6f0f8, mutedColor=0xffb9afc0, accent=0xff7c4dff;

    public VStoreAdapter(Context context, ItemAction onDetails, ItemAction onDownload) {
        super(DIFF);
        this.context=context; this.onDetails=onDetails; this.onDownload=onDownload;
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
        card.setStrokeColor(0x33ffffff); card.setCardBackgroundColor(glass?0x1affffff:0xff201d2a);
        RecyclerView.LayoutParams cp=new RecyclerView.LayoutParams(-1,-2);
        cp.setMargins(0,dp(7),0,dp(7)); card.setLayoutParams(cp);
        LinearLayout body=new LinearLayout(context); body.setOrientation(LinearLayout.VERTICAL); body.setPadding(dp(15),dp(13),dp(15),dp(13)); card.addView(body);
        LinearLayout row=new LinearLayout(context); row.setGravity(Gravity.CENTER_VERTICAL); body.addView(row);
        ImageView icon=new ImageView(context); icon.setImageResource(android.R.drawable.sym_def_app_icon);
        GradientDrawable iconBg=new GradientDrawable();iconBg.setColor(0x26ffffff);iconBg.setCornerRadius(dp(16));icon.setBackground(iconBg);icon.setClipToOutline(true);
        row.addView(icon,new LinearLayout.LayoutParams(dp(56),dp(56)));
        LinearLayout col=new LinearLayout(context);col.setOrientation(LinearLayout.VERTICAL);col.setPadding(dp(12),0,0,0);
        row.addView(col,new LinearLayout.LayoutParams(0,-2,1));
        LinearLayout titleRow=new LinearLayout(context);titleRow.setGravity(Gravity.CENTER_VERTICAL);col.addView(titleRow);
        TextView title=label("",17,textColor);title.setTypeface(null,1);titleRow.addView(title,new LinearLayout.LayoutParams(0,-2,1));
        TextView version=label("",11,textColor);version.setPadding(dp(8),dp(3),dp(8),dp(3));
        GradientDrawable chip=new GradientDrawable();chip.setColor(0x26ffffff);chip.setCornerRadius(dp(12));version.setBackground(chip);titleRow.addView(version);
        TextView description=label("",13,textColor);description.setMaxLines(2);description.setEllipsize(android.text.TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams descParams=new LinearLayout.LayoutParams(-1,-2);descParams.topMargin=dp(compact?5:9);body.addView(description,descParams);
        if(compact){icon.getLayoutParams().width=dp(44);icon.getLayoutParams().height=dp(44);description.setMaxLines(1);}
        MaterialButton action=new MaterialButton(context);action.setCornerRadius(dp(18));action.setMinHeight(dp(40));action.setMinimumHeight(dp(40));
        action.setTextSize(12);action.setTypeface(null,1);action.setInsetTop(0);action.setInsetBottom(0);
        LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(-2,dp(40));ap.topMargin=dp(10);body.addView(action,ap);
        return new Holder(card,icon,title,version,description,action);
    }
    @Override public void onBindViewHolder(@NonNull Holder h,int position) {
        VStoreAppItem item=getItem(position);
        h.title.setText(item.name);
        h.version.setText("v"+item.version);
        h.description.setText(item.description);
        if(item.id.toLowerCase(java.util.Locale.ROOT).contains("vgram")||item.name.toLowerCase(java.util.Locale.ROOT).contains("vgram"))h.icon.setImageResource(R.drawable.ic_vgram);else if(item.id.toLowerCase(java.util.Locale.ROOT).contains("vtok")||item.name.toLowerCase(java.util.Locale.ROOT).equals("vtok"))h.icon.setImageResource(R.drawable.ic_app_default);else try { if(item.packageName!=null&&!item.packageName.isEmpty()){android.content.pm.ApplicationInfo info=context.getPackageManager().getApplicationInfo(item.packageName,0);h.icon.setImageDrawable(context.getPackageManager().getApplicationIcon(info));} else h.icon.setImageResource(R.drawable.ic_app_default); } catch(Exception ignored){h.icon.setImageResource(R.drawable.ic_app_default);}
        boolean ready=item.downloadUrl!=null&&!item.downloadUrl.isEmpty();
        h.action.setText(ready?"СКАЧАТЬ":"В РАЗРАБОТКЕ");
        h.action.setEnabled(ready);
        h.action.setAlpha(ready?1f:0.58f);
        h.action.setBackgroundTintList(android.content.res.ColorStateList.valueOf(ready?accent:0xff55515d));
        h.action.setTextColor(ready?0xffffffff:0xffc7c2cc);
        h.card.setOnClickListener(v->onDetails.onAction(item));
        h.action.setOnClickListener(v->{if(ready)onDownload.onAction(item);});
    }
    private TextView label(String s,int size,int color){TextView t=new TextView(context);t.setText(s);t.setTextSize(size);t.setTextColor(color);return t;}
    private int dp(float n){return (int)(n*context.getResources().getDisplayMetrics().density+0.5f);}
    static final class Holder extends RecyclerView.ViewHolder {
        final MaterialCardView card;final ImageView icon;final TextView title,version,description;final MaterialButton action;
        Holder(MaterialCardView c,ImageView i,TextView t,TextView v,TextView d,MaterialButton a){super(c);card=c;icon=i;title=t;version=v;description=d;action=a;}
    }
}