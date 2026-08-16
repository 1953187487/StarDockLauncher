package com.tungsten.hmclpe.auth;

import android.content.Context;

import com.tungsten.hmclpe.utils.Prefs;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

public class AccountManager {

    private static final String KEY_ACTIVE = "active_account_idx";
    private static final String FILE_NAME = "accounts.dat";

    private final Context ctx;
    private final List<AccountInfo> accounts = new ArrayList<>();
    private int activeIndex = -1;

    public AccountManager(Context ctx) {
        this.ctx = ctx.getApplicationContext();
        load();
    }

    @SuppressWarnings("unchecked")
    private void load() {
        File f = new File(ctx.getFilesDir(), FILE_NAME);
        if (f.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f))) {
                Object o = ois.readObject();
                if (o instanceof List) {
                    accounts.clear();
                    accounts.addAll((List<AccountInfo>) o);
                }
            } catch (Throwable t) {
            }
        }
        activeIndex = Prefs.get(ctx).getInt(KEY_ACTIVE, -1);
        if (activeIndex >= accounts.size()) {
            activeIndex = -1;
        }
    }

    public void save() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(new File(ctx.getFilesDir(), FILE_NAME)))) {
            oos.writeObject(accounts);
        } catch (Throwable t) {
        }
        Prefs.get(ctx).putInt(KEY_ACTIVE, activeIndex);
    }

    public List<AccountInfo> all() {
        return accounts;
    }

    public AccountInfo getActive() {
        if (activeIndex < 0 || activeIndex >= accounts.size()) {
            return null;
        }
        return accounts.get(activeIndex);
    }

    public void setActive(int idx) {
        if (idx >= 0 && idx < accounts.size()) {
            activeIndex = idx;
            save();
        }
    }

    public void add(AccountInfo info) {
        for (int i = 0; i < accounts.size(); i++) {
            AccountInfo a = accounts.get(i);
            if (a.username != null && a.username.equals(info.username)) {
                accounts.set(i, info);
                save();
                return;
            }
        }
        accounts.add(info);
        if (activeIndex < 0) {
            activeIndex = 0;
        }
        save();
    }

    public void remove(int idx) {
        if (idx >= 0 && idx < accounts.size()) {
            accounts.remove(idx);
            if (activeIndex == idx) {
                activeIndex = accounts.isEmpty() ? -1 : 0;
            } else if (activeIndex > idx) {
                activeIndex--;
            }
            save();
        }
    }

    public int size() {
        return accounts.size();
    }
}
