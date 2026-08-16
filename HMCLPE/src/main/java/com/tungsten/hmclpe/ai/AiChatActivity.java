package com.tungsten.hmclpe.ai;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.stardock.launcher.R;

import java.util.ArrayList;
import java.util.List;

public class AiChatActivity extends AppCompatActivity {

    private static final String TAG = "AiChatActivity";

    private EditText input;
    private LinearLayout log;
    private ScrollView scroll;
    private AiChatService service = new AiChatService();
    private final List<AiMessage> history = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_ai_chat);
        } catch (Throwable t) {
            Log.e(TAG, "setContentView failed", t);
            return;
        }
        try {
            input = findViewById(R.id.ai_input);
            log = findViewById(R.id.ai_log);
            scroll = findViewById(R.id.ai_scroll);
        } catch (Throwable t) {
            Log.e(TAG, "bind views failed", t);
        }
        try {
            MaterialButton btnSend = findViewById(R.id.ai_btn_send);
            if (btnSend != null) {
                btnSend.setOnClickListener(v -> onSend());
            }
            MaterialButton btnClear = findViewById(R.id.ai_btn_clear);
            if (btnClear != null) {
                btnClear.setOnClickListener(v -> {
                    history.clear();
                    if (log != null) {
                        log.removeAllViews();
                    }
                });
            }
            MaterialButtonToggleGroup tg = findViewById(R.id.ai_provider_toggle);
            if (tg != null) {
                tg.addOnButtonCheckedListener((g, id, checked) -> {
                    if (!checked) {
                        return;
                    }
                    try {
                        AiProviderManager mgr = AiProviderManager.get(this);
                        if (id == R.id.ai_btn_provider_deepseek) {
                            mgr.setActive("deepseek");
                        } else if (id == R.id.ai_btn_provider_openai) {
                            mgr.setActive("openai");
                        } else if (id == R.id.ai_btn_provider_mock) {
                            mgr.setActive("mock");
                        }
                    } catch (Throwable ignored) {
                    }
                });
                tg.check(R.id.ai_btn_provider_mock);
            }
        } catch (Throwable t) {
            Log.e(TAG, "bind buttons failed", t);
        }
        appendLine("assistant", "你好，我是 StarDock AI 助手。选择左侧模型即可开始对话。");
    }

    private void onSend() {
        try {
            String text = input == null ? "" : input.getText().toString().trim();
            if (TextUtils.isEmpty(text)) {
                return;
            }
            input.setText("");
            appendLine("user", text);
            history.add(AiMessage.user(text));
            AiProvider provider;
            try {
                provider = AiProviderManager.get(this).active();
            } catch (Throwable t) {
                provider = null;
            }
            appendLine("assistant", "（思考中...）");
            final TextView anchor = lastText;
            service.chat(provider, history, new AiChatService.StreamCallback() {
                @Override
                public void onDelta(String delta) {
                    runOnUiThread(() -> {
                        if (anchor != null) {
                            anchor.setText(delta);
                            scrollDown();
                        }
                    });
                }

                @Override
                public void onDone(String full) {
                    runOnUiThread(() -> {
                        if (anchor != null) {
                            anchor.setText(full);
                            scrollDown();
                        }
                        history.add(AiMessage.assistant(full));
                    });
                }

                @Override
                public void onError(Throwable t) {
                    runOnUiThread(() -> {
                        if (anchor != null) {
                            anchor.setText("（出错：" + t.getMessage() + "）");
                        }
                    });
                }
            });
        } catch (Throwable t) {
            Log.e(TAG, "send failed", t);
        }
    }

    private TextView lastText;

    private void appendLine(String role, String text) {
        if (log == null) {
            return;
        }
        TextView tv = new TextView(this);
        tv.setText((role.equals("user") ? "我：" : "AI：") + text);
        tv.setPadding(12, 12, 12, 12);
        tv.setTextSize(14);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = 8;
        log.addView(tv, lp);
        lastText = tv;
        scrollDown();
    }

    private void scrollDown() {
        if (scroll != null) {
            scroll.post(() -> scroll.fullScroll(View.FOCUS_DOWN));
        }
    }
}
