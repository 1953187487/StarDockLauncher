package com.tungsten.hmclpe.launcher.uis.login;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.stardock.launcher.R;
import com.tungsten.hmclpe.auth.AccountInfo;
import com.tungsten.hmclpe.auth.AccountManager;
import com.tungsten.hmclpe.auth.AuthResult;
import com.tungsten.hmclpe.auth.AuthService;

public class AccountActivity extends AppCompatActivity {

    private static final String TAG = "AccountActivity";

    private AccountManager accountManager;
    private AuthService authService = new AuthService();

    private EditText editUsername;
    private EditText editEmail;
    private EditText editPassword;
    private TextView statusView;
    private ListView list;

    private int mode = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_account);
        } catch (Throwable t) {
            Log.e(TAG, "setContentView failed", t);
            return;
        }
        try {
            accountManager = new AccountManager(getApplicationContext());
        } catch (Throwable t) {
            Log.e(TAG, "AccountManager init failed", t);
        }
        try {
            MaterialToolbar tb = findViewById(R.id.account_toolbar);
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
            editUsername = findViewById(R.id.account_input_username);
            editEmail = findViewById(R.id.account_input_email);
            editPassword = findViewById(R.id.account_input_password);
            statusView = findViewById(R.id.account_status);
            list = findViewById(R.id.account_list);
        } catch (Throwable t) {
            Log.e(TAG, "bind views failed", t);
        }
        try {
            MaterialButtonToggleGroup tg = findViewById(R.id.account_mode_toggle);
            if (tg != null) {
                tg.addOnButtonCheckedListener((g, id, checked) -> {
                    if (!checked) {
                        return;
                    }
                    if (id == R.id.account_btn_mode_offline) {
                        mode = 0;
                    } else if (id == R.id.account_btn_mode_mojang) {
                        mode = 1;
                    } else if (id == R.id.account_btn_mode_microsoft) {
                        mode = 2;
                    }
                    renderMode();
                });
                tg.check(R.id.account_btn_mode_offline);
            }
        } catch (Throwable t) {
            Log.e(TAG, "mode toggle failed", t);
        }
        try {
            MaterialButton btnLogin = findViewById(R.id.account_btn_login);
            if (btnLogin != null) {
                btnLogin.setOnClickListener(v -> doLogin());
            }
        } catch (Throwable t) {
            Log.e(TAG, "btn bind failed", t);
        }
        try {
            renderList();
        } catch (Throwable t) {
            Log.e(TAG, "renderList failed", t);
        }
        renderMode();
    }

    private void renderMode() {
        try {
            if (statusView != null) {
                statusView.setText("当前模式：" + (mode == 0 ? "离线登录" : (mode == 1 ? "Mojang 邮箱" : "Microsoft 登录")));
            }
            if (editUsername != null) {
                editUsername.setVisibility(mode == 0 ? View.VISIBLE : View.GONE);
            }
            if (editEmail != null) {
                editEmail.setVisibility(mode == 1 ? View.VISIBLE : View.GONE);
            }
            if (editPassword != null) {
                editPassword.setVisibility(mode == 1 ? View.VISIBLE : View.GONE);
            }
        } catch (Throwable t) {
            Log.e(TAG, "renderMode failed", t);
        }
    }

    private void doLogin() {
        try {
            AuthResult result;
            if (mode == 0) {
                String name = editUsername == null ? "" : editUsername.getText().toString().trim();
                result = authService.loginOffline(name);
            } else if (mode == 1) {
                String email = editEmail == null ? "" : editEmail.getText().toString().trim();
                String pwd = editPassword == null ? "" : editPassword.getText().toString();
                result = authService.loginMojang(email, pwd);
            } else {
                result = authService.loginMicrosoft();
            }
            if (!result.success) {
                Toast.makeText(this, "登录失败：" + result.error, Toast.LENGTH_SHORT).show();
                return;
            }
            if (accountManager != null && result.account != null) {
                accountManager.add(result.account);
            }
            Toast.makeText(this, "登录成功", Toast.LENGTH_SHORT).show();
            renderList();
        } catch (Throwable t) {
            Log.e(TAG, "doLogin failed", t);
        }
    }

    private void renderList() {
        try {
            if (list == null) {
                return;
            }
            java.util.List<AccountInfo> accounts = accountManager == null ? new java.util.ArrayList<>() : accountManager.all();
            java.util.List<String> labels = new java.util.ArrayList<>();
            for (AccountInfo a : accounts) {
                labels.add((a.username == null ? "(未命名)" : a.username) + " · " + (a.isOffline() ? "离线" : "在线"));
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, labels);
            list.setAdapter(adapter);
            list.setOnItemClickListener((parent, view, position, id) -> {
                if (accountManager != null) {
                    accountManager.setActive(position);
                }
                Toast.makeText(this, "已切换为当前账号", Toast.LENGTH_SHORT).show();
            });
        } catch (Throwable t) {
            Log.e(TAG, "renderList failed", t);
        }
    }
}
