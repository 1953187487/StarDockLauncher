package com.tungsten.hmclpe.utils;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;

import java.util.Locale;

public class LocaleUtils {

    public static void apply(Context ctx, String langTag) {
        try {
            Locale locale;
            if (langTag == null || langTag.isEmpty() || "auto".equalsIgnoreCase(langTag)) {
                locale = Locale.getDefault();
            } else if (langTag.contains("_")) {
                String[] parts = langTag.split("_");
                locale = new Locale(parts[0], parts[1]);
            } else {
                locale = new Locale(langTag);
            }
            Locale.setDefault(locale);
            Configuration cfg = new Configuration(ctx.getResources().getConfiguration());
            cfg.setLocale(locale);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                cfg.setLocales(new android.os.LocaleList(locale));
            }
            ctx.createConfigurationContext(cfg);
            Resources.getSystem().updateConfiguration(cfg, ctx.getResources().getDisplayMetrics());
        } catch (Throwable t) {
        }
    }
}
