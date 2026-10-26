package com.vstore.app;

import org.json.JSONObject;
import java.util.Objects;

public final class VStoreAppItem {
    public final String id, name, category, version, badge, description, iconUrl, downloadUrl, packageName, sha256;
    public final boolean exclusive;

    public VStoreAppItem(String id, String name, String category, String version, String badge,
                         String description, String iconUrl, String downloadUrl, String packageName,
                         String sha256, boolean exclusive) {
        this.id=id; this.name=name; this.category=category; this.version=version; this.badge=badge;
        this.description=description; this.iconUrl=iconUrl; this.downloadUrl=downloadUrl;
        this.packageName=packageName; this.sha256=sha256; this.exclusive=exclusive;
    }

    public static VStoreAppItem fromJson(JSONObject o) {
        return new VStoreAppItem(o.optString("id"), o.optString("name","Приложение"),
            o.optString("category","Apps"), o.optString("version",""), o.optString("badge",""),
            o.optString("description",""), o.optString("icon_url",""), o.optString("download_url",""),
            o.optString("package_name",""), o.optString("sha256",""), o.optBoolean("exclusive",false));
    }

    public JSONObject toJson() {
        JSONObject o=new JSONObject();
        try {
            o.put("id",id); o.put("name",name); o.put("category",category); o.put("version",version);
            o.put("badge",badge); o.put("description",description); o.put("icon_url",iconUrl);
            o.put("download_url",downloadUrl); o.put("package_name",packageName); o.put("sha256",sha256);
            o.put("exclusive",exclusive);
        } catch (Exception ignored) {}
        return o;
    }

    @Override public boolean equals(Object other) {
        if (this==other) return true;
        if (!(other instanceof VStoreAppItem)) return false;
        VStoreAppItem o=(VStoreAppItem)other;
        return exclusive==o.exclusive && Objects.equals(id,o.id) && Objects.equals(name,o.name)
            && Objects.equals(category,o.category) && Objects.equals(version,o.version)
            && Objects.equals(badge,o.badge) && Objects.equals(description,o.description)
            && Objects.equals(iconUrl,o.iconUrl) && Objects.equals(downloadUrl,o.downloadUrl)
            && Objects.equals(packageName,o.packageName) && Objects.equals(sha256,o.sha256);
    }
    @Override public int hashCode() { return Objects.hash(id,name,category,version,badge,description,iconUrl,downloadUrl,packageName,sha256,exclusive); }
}