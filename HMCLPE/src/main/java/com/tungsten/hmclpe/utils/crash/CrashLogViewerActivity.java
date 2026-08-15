package com.tungsten.hmclpe.utils.crash;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.tungsten.hmclpe.R;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

public class CrashLogViewerActivity extends AppCompatActivity {

    public static final String EXTRA_LOG_PATH = "log_path";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crash_log);

        TextView title = findViewById(R.id.crash_title);
        TextView body = findViewById(R.id.crash_body);
        Button close = findViewById(R.id.crash_close);
        Button share = findViewById(R.id.crash_share);

        String path = getIntent().getStringExtra(EXTRA_LOG_PATH);
        if (path == null) {
            File latest = CrashHandler.getLatestCrashLog(this);
            if (latest != null) path = latest.getAbsolutePath();
        }
        final String fpath = path;

        if (path == null) {
            body.setText("未找到崩溃日志。");
            close.setOnClickListener(v -> finish());
            share.setOnClickListener(v -> finish());
            return;
        }

        title.setText("崩溃日志：" + new File(path).getName());
        body.setText(readFile(path));

        close.setOnClickListener(v -> {
            CrashHandler.clearPendingFlag(this);
            finish();
        });

        share.setOnClickListener(v -> {
            try {
                String content = readFile(fpath);
                android.content.Intent sendIntent = new android.content.Intent(android.content.Intent.ACTION_SEND);
                sendIntent.setType("text/plain");
                sendIntent.putExtra(android.content.Intent.EXTRA_TEXT, "StarDockLauncher 崩溃日志：\n\n" + content);
                startActivity(android.content.Intent.createChooser(sendIntent, "分享崩溃日志"));
            } catch (Exception e) {
                Toast.makeText(this, "分享失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String readFile(String path) {
        try (FileInputStream fis = new FileInputStream(path);
             BufferedReader br = new BufferedReader(new InputStreamReader(fis))) {
            StringBuilder sb = new StringBuilder();
            String line;
            int lineCount = 0;
            while ((line = br.readLine()) != null && lineCount < 500) {
                sb.append(line).append("\n");
                lineCount++;
            }
            if (lineCount >= 500) {
                sb.append("\n... (仅显示前 500 行，完整日志请分享)\n");
            }
            return sb.toString();
        } catch (Exception e) {
            return "读取日志失败：" + e.getMessage();
        }
    }
}
