package com.tungsten.hmclpe.launcher.uis.about;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.stardock.launcher.BuildConfig;
import com.stardock.launcher.R;

public class AboutActivity extends AppCompatActivity {

    private static final String TAG = "AboutActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_about);
        } catch (Throwable t) {
            Log.e(TAG, "setContentView failed", t);
            return;
        }
        try {
            MaterialToolbar tb = findViewById(R.id.about_toolbar);
            if (tb != null) {
                setSupportActionBar(tb);
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                }
                tb.setNavigationOnClickListener(v -> finish());
            }
        } catch (Throwable t) {
            Log.e(TAG, "toolbar failed", t);
        }
        try {
            TextView title = findViewById(R.id.about_title);
            TextView ver = findViewById(R.id.about_version);
            if (title != null) {
                title.setText(getString(R.string.app_name));
            }
            if (ver != null) {
                ver.setText("v" + BuildConfig.VERSION_NAME + " · versionCode=" + BuildConfig.VERSION_CODE);
            }
        } catch (Throwable t) {
            Log.e(TAG, "bind failed", t);
        }
    }
}
