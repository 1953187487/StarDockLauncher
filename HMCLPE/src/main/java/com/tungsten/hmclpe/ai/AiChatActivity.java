package com.tungsten.hmclpe.ai;

import android.animation.ValueAnimator;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.tungsten.hmclpe.launcher.MainActivity;
import com.tungsten.hmclpe.R;

import java.util.ArrayList;
import java.util.List;

public class AiChatActivity extends AppCompatActivity implements View.OnClickListener {

    private static final int MAX_HISTORY = 50;

    private View rootView;
    private View drawerHandle;
    private TextView drawerTitle;
    private TextView connectionStatus;
    private TextView providerBadge;
    private LinearLayout providerBar;
    private MaterialButton providerBtn;
    private MaterialButton historyBtn;
    private MaterialButton settingsBtn;
    private ScrollView messageScroll;
    private LinearLayout messageContainer;
    private EditText input;
    private MaterialButton sendButton;
    private MaterialButton attachButton;
    private MaterialButton stopButton;
    private HorizontalScrollView quickActionScroll;
    private LinearLayout quickActionBar;
    private FrameLayout typingIndicator;
    private TextView typingText;

    private final List<AiMessage> history = new ArrayList<>();
    private final List<Conversation> conversations = new ArrayList<>();
    private int currentConvIdx = -1;

    private AiProviderManager providerManager;
    private AiChatClient chatClient;
    private StringBuilder currentStreamBuffer;
    private TextView currentStreamView;
    private boolean sending = false;
    private boolean drawerExpanded = true;

    private final Handler ui = new Handler(Looper.getMainLooper());

    public static class Conversation {
        public String title;
        public List<AiMessage> messages = new ArrayList<>();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_chat);

        rootView = findViewById(R.id.ai_root);
        drawerHandle = findViewById(R.id.ai_drawer_handle);
        drawerTitle = findViewById(R.id.ai_drawer_title);
        connectionStatus = findViewById(R.id.ai_connection_status);
        providerBadge = findViewById(R.id.ai_provider_badge);
        providerBar = findViewById(R.id.ai_provider_bar);
        providerBtn = findViewById(R.id.ai_btn_provider);
        historyBtn = findViewById(R.id.ai_btn_history);
        settingsBtn = findViewById(R.id.ai_btn_settings);
        messageScroll = findViewById(R.id.ai_message_scroll);
        messageContainer = findViewById(R.id.ai_message_container);
        input = findViewById(R.id.ai_input);
        sendButton = findViewById(R.id.ai_send);
        attachButton = findViewById(R.id.ai_attach);
        stopButton = findViewById(R.id.ai_stop);
        quickActionScroll = findViewById(R.id.ai_quick_scroll);
        quickActionBar = findViewById(R.id.ai_quick_actions);
        typingIndicator = findViewById(R.id.ai_typing);
        typingText = findViewById(R.id.ai_typing_text);

        if (MainActivityHolder.get() != null && getIntent().getBooleanExtra("drawer_mode", true)) {
            try {
                WindowManager.LayoutParams lp = getWindow().getAttributes();
                lp.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.42f);
                lp.height = ViewGroup.LayoutParams.MATCH_PARENT;
                lp.gravity = Gravity.END | Gravity.CENTER_VERTICAL;
                lp.dimAmount = 0.0f;
                getWindow().setAttributes(lp);
                getWindow().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#CC0F1419")));
            } catch (Throwable ignored) {
            }
        }

        providerManager = AiProviderManager.getInstance(this);
        chatClient = new AiChatClient();

        sendButton.setOnClickListener(this);
        attachButton.setOnClickListener(this);
        stopButton.setOnClickListener(this);
        providerBtn.setOnClickListener(this);
        historyBtn.setOnClickListener(this);
        settingsBtn.setOnClickListener(this);
        drawerHandle.setOnClickListener(this);

        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                int len = s == null ? 0 : s.length();
                sendButton.setEnabled(!sending && len > 0);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        sendButton.setEnabled(false);
        updateProviderBadge();
        setConnectionStatus("已连接", "#4CAF50");

        startNewConversation();

        buildQuickActions();
        scrollToBottom();
    }

    @Override
    public void onClick(View v) {
        if (v == sendButton) doSend();
        else if (v == attachButton) showAttachMenu();
        else if (v == stopButton) stopSending();
        else if (v == providerBtn) showProviderPicker();
        else if (v == historyBtn) showHistoryPicker();
        else if (v == settingsBtn) showSettings();
        else if (v == drawerHandle) toggleDrawer();
    }

    private void toggleDrawer() {
        drawerExpanded = !drawerExpanded;
        ViewGroup.LayoutParams lp = rootView.getLayoutParams();
        if (lp instanceof FrameLayout.LayoutParams) {
            FrameLayout.LayoutParams flp = (FrameLayout.LayoutParams) lp;
            ValueAnimator anim = ValueAnimator.ofInt(flp.width, drawerExpanded ? (int) (getResources().getDisplayMetrics().widthPixels * 0.42f) : (int) (getResources().getDisplayMetrics().widthPixels * 0.08f));
            anim.addUpdateListener(a -> {
                flp.width = (int) a.getAnimatedValue();
                rootView.setLayoutParams(flp);
            });
            anim.setDuration(220);
            anim.start();
        }
    }

    private void buildQuickActions() {
        quickActionBar.removeAllViews();
        String[][] actions = {
                {"找模组", "[ACTION:SEARCH_MOD shader]"},
                {"找光影", "[ACTION:SEARCH_MOD shader]"},
                {"找整合包", "[ACTION:SEARCH_MOD modpack]"},
                {"复制MC", "复制我的世界最新版本号给我，并告诉我适配的启动器版本"},
                {"崩溃?", "[ACTION:ANALYZE_CRASH]"},
                {"日志", "[ACTION:SUMMARIZE_LOG]"},
                {"搜教程", "[ACTION:VIDEO_HELP]"}
        };
        for (String[] a : actions) {
            MaterialButton btn = new MaterialButton(this);
            btn.setText(a[0]);
            btn.setTextSize(11);
            btn.setPadding(0, 0, 0, 0);
            btn.setMinWidth(0);
            btn.setMinHeight(0);
            btn.setMinimumWidth(0);
            btn.setMinimumHeight(0);
            btn.setAllCaps(false);
            btn.setCornerRadius(28);
            btn.setOnClickListener(v -> {
                input.setText(a[1]);
                input.setSelection(a[1].length());
                input.requestFocus();
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, (int) (36 * getResources().getDisplayMetrics().density));
            lp.setMargins(6, 0, 6, 0);
            quickActionBar.addView(btn, lp);
        }
    }

    private void showAttachMenu() {
        PopupMenu pm = new PopupMenu(this, attachButton);
        pm.getMenu().add(0, 1, 0, "粘贴视频链接识别模组");
        pm.getMenu().add(0, 2, 1, "粘贴崩溃日志让 AI 总结");
        pm.getMenu().add(0, 3, 2, "粘贴日志文件路径");
        pm.getMenu().add(0, 4, 3, "搜索教学视频");
        pm.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 1: pasteAndProcess("video"); return true;
                case 2: pasteAndProcess("log"); return true;
                case 3: pasteAndProcess("logpath"); return true;
                case 4: input.setText("[ACTION:VIDEO_HELP]"); return true;
            }
            return false;
        });
        pm.show();
    }

    private void pasteAndProcess(String kind) {
        android.content.ClipboardManager cm = (android.content.ClipboardManager) getSystemService(android.content.Context.CLIPBOARD_SERVICE);
        if (cm == null || !cm.hasPrimaryClip()) {
            Toast.makeText(this, "剪贴板为空", Toast.LENGTH_SHORT).show();
            return;
        }
        CharSequence text = cm.getPrimaryClip().getItemAt(0).getText();
        if (text == null) {
            Toast.makeText(this, "剪贴板无文本", Toast.LENGTH_SHORT).show();
            return;
        }
        String content = text.toString();
        if ("video".equals(kind)) {
            input.setText("[ACTION:VIDEO_LINK] " + content);
        } else if ("log".equals(kind)) {
            input.setText("[ACTION:SUMMARIZE_LOG]\n\n" + content);
        } else if ("logpath".equals(kind)) {
            input.setText("[ACTION:ANALYZE_LOG_PATH] " + content);
        }
        input.setSelection(input.getText().length());
    }

    private void doSend() {
        if (sending) return;
        String text = input.getText().toString().trim();
        if (text.isEmpty()) return;
        input.setText("");
        appendUserMessage(text);
        history.add(new AiMessage("user", text));
        if (currentConvIdx >= 0 && currentConvIdx < conversations.size()) {
            conversations.get(currentConvIdx).messages.add(new AiMessage("user", text));
        }
        doStreamCall(history);
    }

    private void appendUserMessage(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(Color.parseColor("#FFFFFF"));
        tv.setTextSize(14);
        tv.setBackgroundResource(R.drawable.bg_ai_chat_user_bubble);
        tv.setPadding(24, 16, 24, 16);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.END;
        lp.setMargins(48, 8, 12, 8);
        messageContainer.addView(tv, lp);
        scrollToBottom();
    }

    private void appendAssistantPlaceholder() {
        TextView tv = new TextView(this);
        tv.setText("「消息小溪」正在思考喵...");
        tv.setTextColor(Color.parseColor("#A0FFFFFF"));
        tv.setTextSize(13);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.START;
        lp.setMargins(12, 8, 48, 8);
        messageContainer.addView(tv, lp);
        currentStreamView = tv;
        currentStreamBuffer = new StringBuilder();
        typingIndicator.setVisibility(View.VISIBLE);
        typingText.setText("正在连接 AI...");
        scrollToBottom();
    }

    private void doStreamCall(List<AiMessage> messages) {
        sending = true;
        sendButton.setEnabled(false);
        stopButton.setVisibility(View.VISIBLE);
        appendAssistantPlaceholder();
        final AiProvider provider = providerManager.getActiveProvider();
        final long t0 = System.currentTimeMillis();

        chatClient.send(provider, messages, new AiChatClient.StreamCallback() {
            @Override
            public void onChunk(String chunk, String fullText) {
                ui.post(() -> {
                    if (currentStreamBuffer != null) currentStreamBuffer.append(chunk);
                    String display = currentStreamBuffer == null ? "" : currentStreamBuffer.toString();
                    if (currentStreamView != null) {
                        currentStreamView.setText("「消息小溪」：" + display + " ▍");
                    }
                    typingText.setText("正在生成... " + display.length() + " 字");
                    scrollToBottom();
                });
            }

            @Override
            public void onComplete(String fullText) {
                ui.post(() -> finalizeStream(fullText, false, null, System.currentTimeMillis() - t0));
            }

            @Override
            public void onError(String error) {
                ui.post(() -> finalizeStream(currentStreamBuffer == null ? "" : currentStreamBuffer.toString(), true, error, System.currentTimeMillis() - t0));
            }
        });
    }

    private void finalizeStream(String fullText, boolean errored, String error, long elapsed) {
        sending = false;
        sendButton.setEnabled(input.getText().length() > 0);
        stopButton.setVisibility(View.GONE);
        typingIndicator.setVisibility(View.GONE);
        if (currentStreamView != null) {
            String prefix = "「消息小溪」：";
            String body = fullText == null ? "" : fullText;
            if (errored) {
                currentStreamView.setText(prefix + body + (body.isEmpty() ? "" : "\n\n") + "（生成失败：" + error + "）");
                setConnectionStatus("连接失败", "#F44336");
            } else {
                currentStreamView.setText(prefix + body + "\n\n— 用时 " + elapsed + " ms");
                setConnectionStatus("已连接", "#4CAF50");
            }
        }
        if (!errored && !TextUtils.isEmpty(fullText)) {
            history.add(new AiMessage("assistant", fullText));
            if (currentConvIdx >= 0 && currentConvIdx < conversations.size()) {
                conversations.get(currentConvIdx).messages.add(new AiMessage("assistant", fullText));
            }
            while (history.size() > MAX_HISTORY * 2) history.remove(0);
        }
        currentStreamView = null;
        currentStreamBuffer = null;
        scrollToBottom();
    }

    private void stopSending() {
        chatClient.cancel();
        if (sending) {
            finalizeStream(currentStreamBuffer == null ? "" : currentStreamBuffer.toString(), true, "已停止", 0);
        }
    }

    private void scrollToBottom() {
        messageScroll.post(() -> messageScroll.fullScroll(View.FOCUS_DOWN));
    }

    private void setConnectionStatus(String text, String colorHex) {
        connectionStatus.setText(text);
        connectionStatus.setTextColor(Color.parseColor(colorHex));
    }

    private void updateProviderBadge() {
        AiProvider p = providerManager.getActiveProvider();
        providerBadge.setText("服务商：" + p.name + " · 模型 " + p.model);
    }

    private void showProviderPicker() {
        List<AiProvider> list = providerManager.listProviders();
        String[] names = new String[list.size()];
        for (int i = 0; i < list.size(); i++) names[i] = list.get(i).name + (list.get(i).isLocked ? " 🔒" : "");
        new AlertDialog.Builder(this)
                .setTitle("选择 AI 服务商")
                .setItems(names, (d, idx) -> {
                    AiProvider selected = list.get(idx);
                    providerManager.setActiveProvider(selected.id);
                    updateProviderBadge();
                    Toast.makeText(this, "已切换到：" + selected.name, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("管理自定义服务商", (d, w) -> manageCustomProviders())
                .show();
    }

    private void manageCustomProviders() {
        List<AiProvider> customs = new ArrayList<>();
        for (AiProvider p : providerManager.listProviders()) if (!p.isLocked) customs.add(p);
        if (customs.isEmpty()) {
            showAddProviderDialog();
            return;
        }
        String[] names = new String[customs.size() + 1];
        for (int i = 0; i < customs.size(); i++) names[i] = customs.get(i).name;
        names[customs.size()] = "＋ 添加新服务商";
        new AlertDialog.Builder(this)
                .setTitle("自定义服务商")
                .setItems(names, (d, idx) -> {
                    if (idx == customs.size()) showAddProviderDialog();
                    else showProviderActions(customs.get(idx));
                })
                .show();
    }

    private void showProviderActions(AiProvider p) {
        String[] actions = {"设为默认", "删除"};
        new AlertDialog.Builder(this)
                .setTitle(p.name)
                .setItems(actions, (d, idx) -> {
                    if (idx == 0) {
                        providerManager.setActiveProvider(p.id);
                        updateProviderBadge();
                    } else if (idx == 1) {
                        providerManager.removeProvider(p.id);
                        Toast.makeText(this, "已删除：" + p.name, Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    private void showAddProviderDialog() {
        final EditText nameEt = new EditText(this);
        nameEt.setHint("名称（例：我的 OpenAI）");
        final EditText urlEt = new EditText(this);
        urlEt.setHint("Base URL（例：https://api.openai.com/v1）");
        final EditText keyEt = new EditText(this);
        keyEt.setHint("API Key");
        final EditText modelEt = new EditText(this);
        modelEt.setHint("模型名（例：gpt-4o-mini）");
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(48, 24, 48, 8);
        container.addView(nameEt);
        container.addView(urlEt);
        container.addView(keyEt);
        container.addView(modelEt);

        new AlertDialog.Builder(this)
                .setTitle("添加自定义服务商")
                .setView(container)
                .setPositiveButton("添加", (d, w) -> {
                    String name = nameEt.getText().toString().trim();
                    String url = urlEt.getText().toString().trim();
                    String key = keyEt.getText().toString().trim();
                    String model = modelEt.getText().toString().trim();
                    if (name.isEmpty() || url.isEmpty() || key.isEmpty()) {
                        Toast.makeText(this, "名称、URL、Key 必填", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (model.isEmpty()) model = "auto";
                    AiProvider p = new AiProvider(
                            "custom_" + System.currentTimeMillis(),
                            name, url, key, model,
                            AiProviderManager.DEFAULT_ROLE,
                            false
                    );
                    providerManager.addProvider(p);
                    Toast.makeText(this, "已添加：" + name, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void showHistoryPicker() {
        if (conversations.isEmpty()) {
            Toast.makeText(this, "暂无历史对话", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] titles = new String[conversations.size() + 1];
        titles[0] = "＋ 新建对话";
        for (int i = 0; i < conversations.size(); i++) {
            titles[i + 1] = conversations.get(i).title == null ? "未命名" : conversations.get(i).title;
        }
        new AlertDialog.Builder(this)
                .setTitle("对话历史")
                .setItems(titles, (d, idx) -> {
                    if (idx == 0) {
                        startNewConversation();
                    } else {
                        loadConversation(idx - 1);
                    }
                })
                .show();
    }

    private void startNewConversation() {
        Conversation c = new Conversation();
        c.title = "对话 " + (conversations.size() + 1);
        conversations.add(c);
        currentConvIdx = conversations.size() - 1;
        history.clear();
        messageContainer.removeAllViews();
        appendAssistantMessage("喵~ 你好呀玩家，欢迎来找「消息小溪」聊天。我熟悉 Minecraft、HMCL 系启动器、模组/光影/资源包。你可以问我任何问题，或者直接发视频链接给我帮你找模组喵~");
    }

    private void loadConversation(int idx) {
        if (idx < 0 || idx >= conversations.size()) return;
        currentConvIdx = idx;
        Conversation c = conversations.get(idx);
        history.clear();
        history.addAll(c.messages);
        messageContainer.removeAllViews();
        for (AiMessage m : c.messages) {
            if ("user".equals(m.role)) appendUserMessage(m.content);
            else appendAssistantMessage(m.content);
        }
    }

    private void appendAssistantMessage(String text) {
        TextView tv = new TextView(this);
        tv.setText("「消息小溪」：" + text);
        tv.setTextColor(Color.parseColor("#E0FFFFFF"));
        tv.setTextSize(13);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.START;
        lp.setMargins(12, 8, 48, 8);
        messageContainer.addView(tv, lp);
        scrollToBottom();
    }

    private void showSettings() {
        final boolean realtime = providerManager.isRealtimeScan();
        final boolean overlay = providerManager.isOverlayEnabled();
        final float temp = providerManager.getTemperature();
        final int maxTokens = providerManager.getMaxTokens();
        CheckBox realtimeCb = new CheckBox(this);
        realtimeCb.setText("游戏运行时实时日志扫描");
        realtimeCb.setChecked(realtime);
        CheckBox overlayCb = new CheckBox(this);
        overlayCb.setText("启动 AI 浮动球");
        overlayCb.setChecked(overlay);
        EditText tempEt = new EditText(this);
        tempEt.setHint("Temperature (0.0 - 2.0)");
        tempEt.setText(String.valueOf(temp));
        EditText maxEt = new EditText(this);
        maxEt.setHint("Max Tokens");
        maxEt.setText(String.valueOf(maxTokens));
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(48, 24, 48, 8);
        container.addView(realtimeCb);
        container.addView(overlayCb);
        container.addView(tempEt);
        container.addView(maxEt);

        new AlertDialog.Builder(this)
                .setTitle("AI 设置")
                .setView(container)
                .setPositiveButton("保存", (d, w) -> {
                    providerManager.setRealtimeScan(realtimeCb.isChecked());
                    providerManager.setOverlayEnabled(overlayCb.isChecked());
                    try { providerManager.setTemperature(Float.parseFloat(tempEt.getText().toString())); } catch (Exception ignored) {}
                    try { providerManager.setMaxTokens(Integer.parseInt(maxEt.getText().toString())); } catch (Exception ignored) {}
                    Toast.makeText(this, "设置已保存", Toast.LENGTH_SHORT).show();
                })
                .setNeutralButton("重启浮动球服务", (d, w) -> {
                    Intent s = new Intent(this, AiOverlayService.class);
                    s.setAction(AiOverlayService.ACTION_START);
                    startService(s);
                })
                .setNegativeButton("取消", null)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        chatClient.cancel();
    }
}
