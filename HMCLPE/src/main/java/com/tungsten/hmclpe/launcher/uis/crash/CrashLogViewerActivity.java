package com.tungsten.hmclpe.launcher.uis.crash;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.stardock.launcher.R;
import com.tungsten.hmclpe.manifest.LauncherDirs;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class CrashLogViewerActivity extends AppCompatActivity {

    private static final String TAG = "CrashLogViewerActivity";

    private ListView list;
    private TextView content;
    private File[] logs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_crash_log);
        } catch (Throwable t) {
            Log.e(TAG, "setContentView failed", t);
            return;
        }
        try {
            MaterialToolbar tb = findViewById(R.id.crash_toolbar);
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
            list = findViewById(R.id.crash_list);
            content = findViewById(R.id.crash_content);
            renderList();
        } catch (Throwable t) {
            Log.e(TAG, "bind failed", t);
        }
    }

    private void renderList() {
        try {
            File dir = LauncherDirs.crashDir(this);
            File[] arr = dir == null ? new File[0] : dir.listFiles();
            if (arr == null) {
                arr = new File[0];
            }
            Arrays.sort(arr, Comparator.comparing(File::getName).reversed());
            logs = arr;
            List<String> names = new ArrayList<>();
            for (File f : arr) {
                names.add(f.getName());
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, names);
            if (list != null) {
                list.setAdapter(adapter);
                final File[] finalArr = arr;
                list.setOnItemClickListener((p, v, position, id) -> showFile(finalArr[position]));
            }
            if (content != null) {
                if (arr.length == 0) {
                    content.setText("(暂无崩溃日志)");
                } else {
                    showFile(arr[0]);
                }
            }
        } catch (Throwable t) {
            Log.e(TAG, "renderList failed", t);
        }
    }

    private void showFile(File f) {
        try {
            if (content == null) {
                return;
            }
            java.io.FileInputStream fis = new java.io.FileInputStream(f);
            byte[] buf = new byte[(int) Math.min(8192, f.length())];
            int n = fis.read(buf);
            fis.close();
            String text = new String(buf, 0, Math.max(0, n));
            content.setText(text);
        } catch (Throwable t) {
            Log.e(TAG, "showFile failed", t);
            content.setText("读取失败：" + t.getMessage());
        }
    }
}
