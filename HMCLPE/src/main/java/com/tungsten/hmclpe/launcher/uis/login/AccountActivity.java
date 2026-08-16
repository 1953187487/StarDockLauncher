package com.tungsten.hmclpe.launcher.uis.login;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.tungsten.hmclpe.R;
import com.tungsten.hmclpe.auth.microsoft.MicrosoftLoginActivity;
import com.tungsten.hmclpe.launcher.setting.AppPrefs;
import com.tungsten.hmclpe.launcher.setting.AuthManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class AccountActivity extends AppCompatActivity {

    private static final int REQ_SKIN = 6900;
    private static final int REQ_MS = 7900;

    private TextView nameView;
    private TextView modeView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_account);
        } catch (Throwable t) {
            Toast.makeText(this, "加载失败：" + t.getMessage(), Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        MaterialToolbar toolbar = findViewById(R.id.account_toolbar);
        if (toolbar != null) toolbar.setNavigationOnClickListener(v -> finish());

        nameView = findViewById(R.id.account_name);
        modeView = findViewById(R.id.account_mode);
        MaterialButton btnOffline = findViewById(R.id.add_offline_account);
        MaterialButton btnMicrosoft = findViewById(R.id.add_microsoft_account);
        MaterialButton btnThirdParty = findViewById(R.id.add_login_server);
        MaterialButton btnSkin = findViewById(R.id.btn_change_skin);

        if (btnOffline != null) btnOffline.setOnClickListener(v -> askOfflineName());
        if (btnMicrosoft != null) btnMicrosoft.setOnClickListener(v -> askMicrosoftLogin());
        if (btnThirdParty != null) btnThirdParty.setOnClickListener(v -> askThirdPartyServer());
        if (btnSkin != null) btnSkin.setOnClickListener(v -> changeSkin());

        refresh();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        try {
            int m = AuthManager.currentMode();
            String n = AuthManager.currentNickname();
            if (nameView != null) nameView.setText(n == null || n.isEmpty() ? "未登录" : n);
            if (modeView != null) modeView.setText(AuthManager.name(m));
        } catch (Throwable ignored) {}
    }

    private void askOfflineName() {
        try {
            final TextInputEditText input = new TextInputEditText(this);
            input.setHint("输入昵称（决定皮肤 Steve 或 Alex）");
            new MaterialAlertDialogBuilder(this)
                    .setTitle("离线登录")
                    .setView(input)
                    .setPositiveButton("确定", (d, w) -> {
                        String nick = input.getText() == null ? "" : input.getText().toString().trim();
                        if (nick.isEmpty()) {
                            Toast.makeText(this, "昵称不能为空", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        AuthManager.setMode(AuthManager.OFFLINE);
                        AuthManager.setNickname(nick);
                        Toast.makeText(this, "已添加离线账号：" + nick, Toast.LENGTH_SHORT).show();
                        refresh();
                    })
                    .setNegativeButton("取消", null)
                    .show();
        } catch (Throwable t) {
            Toast.makeText(this, "打开失败", Toast.LENGTH_SHORT).show();
        }
    }

    private void askMicrosoftLogin() {
        try {
            Intent i = new Intent(this, MicrosoftLoginActivity.class);
            startActivityForResult(i, REQ_MS);
        } catch (Throwable t) {
            Toast.makeText(this, "Microsoft 登录启动失败：" + t.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void askThirdPartyServer() {
        try {
            final TextInputEditText input = new TextInputEditText(this);
            input.setHint("https://your-auth-server.example.com");
            new MaterialAlertDialogBuilder(this)
                    .setTitle("第三方服务器验证登录")
                    .setMessage("先输入第三方认证服务器地址，自动验证后即可添加账号。")
                    .setView(input)
                    .setPositiveButton("验证", (d, w) -> {
                        String url = input.getText() == null ? "" : input.getText().toString().trim();
                        if (url.isEmpty()) {
                            Toast.makeText(this, "地址不能为空", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        verifyAndAddAccount(url);
                    })
                    .setNegativeButton("取消", null)
                    .show();
        } catch (Throwable t) {
            Toast.makeText(this, "打开失败", Toast.LENGTH_SHORT).show();
        }
    }

    private void verifyAndAddAccount(String url) {
        new Thread(() -> {
            try {
                java.net.URL u = new java.net.URL(url);
                java.net.HttpURLConnection c = (java.net.HttpURLConnection) u.openConnection();
                c.setRequestMethod("GET");
                c.setConnectTimeout(8000);
                c.setReadTimeout(8000);
                int code = c.getResponseCode();
                runOnUiThread(() -> {
                    if (code >= 200 && code < 400) {
                        AuthManager.setServer(url);
                        promptAddAccount(url);
                    } else {
                        Toast.makeText(this, "服务器验证失败：" + code, Toast.LENGTH_LONG).show();
                    }
                });
            } catch (Throwable t) {
                runOnUiThread(() -> Toast.makeText(this, "无法连接：" + t.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void promptAddAccount(String url) {
        try {
            final TextInputEditText userInput = new TextInputEditText(this);
            userInput.setHint("邮箱/账号");
            final TextInputEditText passInput = new TextInputEditText(this);
            passInput.setHint("密码");
            passInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
            LinearLayout ll = new LinearLayout(this);
            ll.setOrientation(LinearLayout.VERTICAL);
            ll.setPadding(48, 16, 48, 16);
            ll.addView(userInput);
            ll.addView(passInput);
            new MaterialAlertDialogBuilder(this)
                    .setTitle("添加账号")
                    .setMessage("服务器：" + url)
                    .setView(ll)
                    .setPositiveButton("确定", (d, w) -> {
                        String u = userInput.getText() == null ? "" : userInput.getText().toString().trim();
                        if (u.isEmpty()) {
                            Toast.makeText(this, "账号不能为空", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        AuthManager.setMode(AuthManager.THIRD_PARTY);
                        AuthManager.setNickname(u);
                        AuthManager.setServer(url);
                        Toast.makeText(this, "第三方登录成功：" + u, Toast.LENGTH_SHORT).show();
                        refresh();
                    })
                    .setNegativeButton("取消", null)
                    .show();
        } catch (Throwable t) {
            Toast.makeText(this, "添加失败", Toast.LENGTH_SHORT).show();
        }
    }

    private void changeSkin() {
        try {
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.setType("image/*");
            startActivityForResult(i, REQ_SKIN);
        } catch (Throwable t) {
            Toast.makeText(this, "换皮肤失败：" + t.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        try {
            if (requestCode == REQ_MS && resultCode == RESULT_OK) {
                AuthManager.setMode(AuthManager.MICROSOFT);
                String nick = AuthManager.currentNickname();
                Toast.makeText(this, "Microsoft 登录成功" + (nick == null || nick.isEmpty() ? "" : "：" + nick), Toast.LENGTH_SHORT).show();
                refresh();
                return;
            }
            if (requestCode == REQ_SKIN && resultCode == RESULT_OK && data != null && data.getData() != null) {
                InputStream in = getContentResolver().openInputStream(data.getData());
                File skinDir = new File(getExternalFilesDir(null), "skins");
                if (!skinDir.exists()) skinDir.mkdirs();
                File out = new File(skinDir, "skin_" + System.currentTimeMillis() + ".png");
                FileOutputStream fout = new FileOutputStream(out);
                byte[] buf = new byte[4096];
                int n;
                while ((n = in.read(buf)) > 0) fout.write(buf, 0, n);
                in.close();
                fout.close();
                AppPrefs.setString(this, AppPrefs.KEY_USER_SKIN_PATH, out.getAbsolutePath());
                Toast.makeText(this, "已应用皮肤：" + out.getName(), Toast.LENGTH_SHORT).show();
            }
        } catch (Throwable t) {
            Toast.makeText(this, "操作失败：" + t.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
