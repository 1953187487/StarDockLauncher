package com.tungsten.hmclpe.launcher.version;

import android.content.Context;
import android.util.Log;

import com.tungsten.hmclpe.manifest.AppManifest;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class VersionManager {

    private static final String TAG = "VersionManager";
    private static final String FILE_NAME = "versions.dat";

    private final Context ctx;
    private final List<VersionInfo> versions = new ArrayList<>();

    public VersionManager(Context ctx) {
        this.ctx = ctx.getApplicationContext();
        load();
        scanInstalled();
    }

    @SuppressWarnings("unchecked")
    private void load() {
        File f = new File(ctx.getFilesDir(), FILE_NAME);
        if (f.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f))) {
                Object o = ois.readObject();
                if (o instanceof List) {
                    versions.clear();
                    versions.addAll((List<VersionInfo>) o);
                }
            } catch (Throwable t) {
                Log.w(TAG, "load failed", t);
            }
        }
    }

    public void save() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(new File(ctx.getFilesDir(), FILE_NAME)))) {
            oos.writeObject(versions);
        } catch (Throwable t) {
            Log.w(TAG, "save failed", t);
        }
    }

    public void scanInstalled() {
        try {
            File dir = new File(AppManifest.VERSION_DIR);
            if (!dir.exists() || !dir.isDirectory()) {
                return;
            }
            File[] subs = dir.listFiles();
            if (subs == null) {
                return;
            }
            Arrays.sort(subs, Comparator.comparing(File::getName));
            for (File sub : subs) {
                if (!sub.isDirectory()) {
                    continue;
                }
                String id = sub.getName();
                boolean has = false;
                for (VersionInfo v : versions) {
                    if (id.equals(v.id)) {
                        v.installed = true;
                        has = true;
                        break;
                    }
                }
                if (!has) {
                    VersionInfo info = new VersionInfo(id, "release", true);
                    info.installedSize = fileSize(sub);
                    versions.add(info);
                }
            }
            save();
        } catch (Throwable t) {
            Log.w(TAG, "scan failed", t);
        }
    }

    private long fileSize(File f) {
        if (f.isFile()) {
            return f.length();
        }
        long total = 0;
        File[] subs = f.listFiles();
        if (subs != null) {
            for (File sub : subs) {
                total += fileSize(sub);
            }
        }
        return total;
    }

    public List<VersionInfo> all() {
        return versions;
    }

    public List<VersionInfo> installed() {
        List<VersionInfo> out = new ArrayList<>();
        for (VersionInfo v : versions) {
            if (v.installed) {
                out.add(v);
            }
        }
        return out;
    }

    public void add(VersionInfo info) {
        for (VersionInfo v : versions) {
            if (info.id != null && info.id.equals(v.id)) {
                v.type = info.type;
                v.url = info.url;
                v.time = info.time;
                v.releaseTime = info.releaseTime;
                v.sha1 = info.sha1;
                v.size = info.size;
                save();
                return;
            }
        }
        versions.add(info);
        save();
    }

    public void remove(String id) {
        for (int i = versions.size() - 1; i >= 0; i--) {
            if (id.equals(versions.get(i).id)) {
                versions.remove(i);
            }
        }
        save();
    }

    public VersionInfo find(String id) {
        if (id == null) {
            return null;
        }
        for (VersionInfo v : versions) {
            if (id.equals(v.id)) {
                return v;
            }
        }
        return null;
    }
}
