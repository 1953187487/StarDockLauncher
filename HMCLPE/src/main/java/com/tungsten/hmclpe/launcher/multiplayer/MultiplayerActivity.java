package com.tungsten.hmclpe.launcher.multiplayer;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.tungsten.hmclpe.R;
import com.tungsten.hmclpe.ai.AiLogAnalyzer;
import com.tungsten.hmclpe.manifest.AppManifest;
import com.tungsten.hmclpe.utils.LocaleUtils;

public class MultiplayerActivity extends AppCompatActivity {

    private MultiplayerPrefs prefs;
    private MaterialSwitch enabledSwitch;
    private TextInputEditText nicknameEdit;
    private TextInputEditText joinCodeEdit;
    private android.widget.TextView inviteCodeText;
    private android.widget.TextView joinResultText;
    private MaterialButton btnScan;
    private MaterialButton btnCopy;
    private MaterialButton btnJoin;

    private String currentInviteCode = "";
    private int currentPort = 0;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleUtils.setLanguage(base));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_multiplayer_v2);
        } catch (Throwable t) {
            android.util.Log.e("MultiplayerActivity", "setContentView failed", t);
            finish();
            return;
        }

        try {
            MaterialToolbar tb = findViewById(R.id.multi_toolbar);
            if (tb != null) tb.setNavigationOnClickListener(v -> finish());

            prefs = new MultiplayerPrefs(this);
            enabledSwitch = findViewById(R.id.multi_switch_enabled);
            nicknameEdit = findViewById(R.id.multi_nickname);
            joinCodeEdit = findViewById(R.id.multi_join_code);
            inviteCodeText = findViewById(R.id.multi_invite_code);
            joinResultText = findViewById(R.id.multi_join_result);
            btnScan = findViewById(R.id.multi_btn_scan_log);
            btnCopy = findViewById(R.id.multi_btn_copy);
            btnJoin = findViewById(R.id.multi_btn_join);

            enabledSwitch.setChecked(prefs.isEnabled());
            enabledSwitch.setOnCheckedChangeListener((v, checked) -> {
                prefs.setEnabled(checked);
                Toast.makeText(this, checked ? "联机功能已启用（悬浮窗会显示联机按钮）" : "联机功能已关闭", Toast.LENGTH_SHORT).show();
            });

            nicknameEdit.setText(prefs.getNickname());
            nicknameEdit.setOnFocusChangeListener((v, hasFocus) -> {
                if (!hasFocus) prefs.setNickname(nicknameEdit.getText().toString().trim());
            });

            btnScan.setOnClickListener(v -> scanAndGenerate());
            btnCopy.setOnClickListener(v -> copyInvite());
            btnJoin.setOnClickListener(v -> parseAndShow());

            currentInviteCode = prefs.getLastInviteCode();
            currentPort = prefs.getLastPort();
            if (!currentInviteCode.isEmpty() && currentPort > 0) {
                inviteCodeText.setText("邀请码：" + currentInviteCode);
            }
        } catch (Throwable t) {
            android.util.Log.e("MultiplayerActivity", "onCreate init failed", t);
            Toast.makeText(this, "联机初始化失败：" + t.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void scanAndGenerate() {
        try {
            String logPath = AiLogAnalyzer.findLatestLog(this);
            if (logPath == null) {
                Toast.makeText(this, "未找到游戏日志。请先在游戏中对局域网开放再点扫描。", Toast.LENGTH_LONG).show();
                return;
            }
            int port = LocalPortScanner.scanLatestLog(logPath);
            if (port <= 0) {
                Toast.makeText(this, "未在日志中识别到房间端口。请确认已在游戏中按 ESC → 对局域网开放。", Toast.LENGTH_LONG).show();
                return;
            }
            currentPort = port;
            currentInviteCode = InviteCodeUtil.generateCode(this, port);
            prefs.setLastInviteCode(currentInviteCode);
            prefs.setLastPort(port);
            inviteCodeText.setText("邀请码：" + currentInviteCode + "\n端口：" + port);
            Toast.makeText(this, "已识别端口 " + port + "，邀请码已生成", Toast.LENGTH_SHORT).show();
        } catch (Throwable t) {
            Toast.makeText(this, "扫描失败：" + t.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void copyInvite() {
        if (currentInviteCode.isEmpty() || currentPort <= 0) {
            Toast.makeText(this, "请先扫描日志生成邀请码", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            String invite = InviteCodeUtil.buildInviteLink(currentInviteCode, currentPort);
            cm.setPrimaryClip(ClipData.newPlainText("StarDock Invite", invite));
            Toast.makeText(this, "已复制邀请链接：" + invite, Toast.LENGTH_LONG).show();
        } catch (Throwable t) {
            Toast.makeText(this, "复制失败：" + t.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void parseAndShow() {
        String code = joinCodeEdit.getText().toString().trim();
        if (code.isEmpty()) {
            Toast.makeText(this, "请粘贴邀请码", Toast.LENGTH_SHORT).show();
            return;
        }
        int port = InviteCodeUtil.parsePort(code);
        String extractedCode = InviteCodeUtil.parseCode(code);
        if (extractedCode.isEmpty()) extractedCode = code;
        StringBuilder sb = new StringBuilder();
        sb.append("邀请码：").append(extractedCode).append("\n");
        if (port > 0) sb.append("端口：").append(port).append("\n");
        sb.append("\n在游戏中选择多人游戏 → 添加服务器\n服务器地址：见邀请方提供的局域网 IP\n端口：").append(port > 0 ? port : 25565);
        joinResultText.setText(sb.toString());
    }
}
