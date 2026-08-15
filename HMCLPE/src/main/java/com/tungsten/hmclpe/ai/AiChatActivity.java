package com.tungsten.hmclpe.ai;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.tungsten.hmclpe.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AiChatActivity extends AppCompatActivity implements View.OnClickListener {

    private static final Pattern ACTION_PATTERN = Pattern.compile("\\[ACTION:(\\w+)(?:\\s+([^\\]]+))?\\]");
    private static final Pattern URL_PATTERN = Pattern.compile("https?://[\\w\\-./?=&%#:+~]+", Pattern.CASE_INSENSITIVE);

    private ListView messageList;
    private EditText input;
    private TextView typingIndicator;
    private ImageButton scanButton;

    private final List<ChatMessage> messages = new ArrayList<>();
    private MessageAdapter adapter;

    private AiProviderManager providerManager;
    private boolean sending = false;
    private boolean scanEnabled = false;

    private final AiActionExecutor actionExecutor = new AiActionExecutor();

    public static class ChatMessage {
        public boolean isUser;
        public String content;

        public ChatMessage(boolean isUser, String content) {
            this.isUser = isUser;
            this.content = content;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_chat);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN, WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            getWindow().getAttributes().layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        providerManager = AiProviderManager.getInstance(this);

        messageList = findViewById(R.id.ai_message_list);
        input = findViewById(R.id.ai_input);
        typingIndicator = findViewById(R.id.ai_typing_indicator);
        scanButton = findViewById(R.id.ai_scan_button);
        ImageButton backButton = findViewById(R.id.ai_back_button);
        ImageButton providerButton = findViewById(R.id.ai_provider_button);
        ImageButton sendButton = findViewById(R.id.ai_send_button);
        TextView quickSearchMod = findViewById(R.id.ai_quick_search_mod);
        TextView quickSearchShader = findViewById(R.id.ai_quick_search_shader);
        TextView quickAnalyzeLog = findViewById(R.id.ai_quick_analyze_log);
        TextView quickVideo = findViewById(R.id.ai_quick_video);

        backButton.setOnClickListener(this);
        providerButton.setOnClickListener(this);
        sendButton.setOnClickListener(this);
        scanButton.setOnClickListener(this);
        quickSearchMod.setOnClickListener(this);
        quickSearchShader.setOnClickListener(this);
        quickAnalyzeLog.setOnClickListener(this);
        quickVideo.setOnClickListener(this);

        adapter = new MessageAdapter(this, messages);
        messageList.setAdapter(adapter);

        scanEnabled = providerManager.isRealtimeScan();
        updateScanButtonState();
        if (scanEnabled) {
            startRealtimeScan();
        }

        if (messages.isEmpty()) {
            messages.add(new ChatMessage(false,
                    "你好，我是「消息小溪」喵~ 我可以帮你：\n" +
                            "• 回答启动器 / 游戏相关的问题\n" +
                            "• 帮你操作启动器（打开设置、下载、版本列表等）\n" +
                            "• 搜索模组、光影、资源包\n" +
                            "• 粘贴视频链接，自动识别视频里的模组并帮你搜索\n" +
                            "• 分析游戏日志，找出闪退原因\n" +
                            "• 一键搜索教学视频（比如怎么做挖土机）\n" +
                            "直接输入问题，或粘贴一个 B 站 / 视频链接试试吧喵~"));
        }
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.ai_back_button) {
            finish();
        } else if (v.getId() == R.id.ai_send_button) {
            sendMessage(input.getText().toString());
        } else if (v.getId() == R.id.ai_provider_button) {
            showProviderDialog();
        } else if (v.getId() == R.id.ai_scan_button) {
            toggleScan();
        } else if (v.getId() == R.id.ai_quick_search_mod) {
            input.setText("");
            input.setHint("输入要搜索的模组名称，回车发送");
            input.requestFocus();
        } else if (v.getId() == R.id.ai_quick_search_shader) {
            input.setText("");
            input.setHint("输入要搜索的光影名称，回车发送");
            input.requestFocus();
        } else if (v.getId() == R.id.ai_quick_analyze_log) {
            handleAnalyzeLog();
        } else if (v.getId() == R.id.ai_quick_video) {
            input.setText("");
            input.setHint("粘贴视频链接（B站/抖音/YouTube等）后发送");
            input.requestFocus();
        }
    }

    private void toggleScan() {
        scanEnabled = !scanEnabled;
        providerManager.setRealtimeScan(scanEnabled);
        updateScanButtonState();
        if (scanEnabled) {
            startRealtimeScan();
            appendAssistantMessage("已开启实时日志扫描喵~ 游戏运行中如果出现报错，我会第一时间帮你分析。");
        } else {
            AiLogWatcher.getInstance().removeListener(scanListener);
            AiLogWatcher.getInstance().stopWatcher();
            appendAssistantMessage("已关闭实时日志扫描。");
        }
    }

    private void updateScanButtonState() {
        scanButton.setColorFilter(scanEnabled ? Color.parseColor("#FFD54F") : Color.WHITE);
    }

    private final AiLogWatcher.ErrorListener scanListener = line -> {
        String logPath = AiLogAnalyzer.findLatestLog(AiChatActivity.this);
        appendAssistantMessage("【实时扫描】检测到日志异常：\n" + line.trim() + "\n\n正在为你分析原因…");
        AiLogAnalyzer.analyzeAsync(AiChatActivity.this, logPath, true, summary -> {
            appendAssistantMessage("【分析结果】\n" + summary);
        });
    };

    private void startRealtimeScan() {
        String logPath = AiLogAnalyzer.findLatestLog(this);
        AiLogWatcher watcher = AiLogWatcher.getInstance();
        watcher.removeListener(scanListener);
        watcher.addListener(scanListener);
        watcher.start(logPath);
    }

    private void sendMessage(String text) {
        if (sending) {
            return;
        }
        text = text.trim();
        if (text.isEmpty()) {
            return;
        }
        input.setText("");
        hideKeyboard();
        appendUserMessage(text);

        if (isVideoLink(text)) {
            handleVideoLink(text);
            return;
        }
        if (isSearchRequest(text)) {
            handleSearchRequest(text);
            return;
        }
        sendToAi(text);
    }

    private boolean isVideoLink(String text) {
        Matcher m = URL_PATTERN.matcher(text);
        while (m.find()) {
            String url = m.group();
            String low = url.toLowerCase(Locale.ROOT);
            if (low.contains("bilibili") || low.contains("b23.tv") || low.contains("youtube")
                    || low.contains("douyin") || low.contains("ixigua") || low.contains("weibo")
                    || low.contains("youku")) {
                return true;
            }
        }
        return false;
    }

    private boolean isSearchRequest(String text) {
        return text.startsWith("搜") || text.startsWith("搜索")
                || text.contains("帮我找") || text.contains("帮我搜")
                || text.contains("有没有");
    }

    private void handleVideoLink(String text) {
        final String url = extractUrl(text);
        appendAssistantMessage("收到视频链接，正在解析视频标题和简介，识别其中的模组/光影…");
        setTyping(true);
        new AiVideoLinkAnalyzer().analyze(this, url, new AiVideoLinkAnalyzer.AnalyzeCallback() {
            @Override
            public void onResult(String title, String description, String extractedNames) {
                setTyping(false);
                appendAssistantMessage("视频标题：" + title + "\n\n识别到的项目名称：" + extractedNames);
                if (extractedNames != null && !extractedNames.isEmpty() && !extractedNames.contains("未识别到")) {
                    String[] names = extractedNames.split("[,，、;；]");
                    for (String name : names) {
                        String n = name.trim();
                        if (!n.isEmpty()) {
                            searchMod(n, "mod");
                        }
                    }
                } else {
                    appendAssistantMessage("没能从视频里识别出明确的模组名称，你可以手动输入模组名让我搜索喵~");
                }
            }

            @Override
            public void onFailed(String error) {
                setTyping(false);
                appendAssistantMessage("视频解析失败：" + error + "\n你可以把视频里出现的模组名称直接告诉我，我来帮你搜索。");
            }
        });
    }

    private String extractUrl(String text) {
        Matcher m = URL_PATTERN.matcher(text);
        if (m.find()) {
            return m.group();
        }
        return text;
    }

    private void handleSearchRequest(String text) {
        String projectType = "mod";
        String query = text;
        if (text.contains("光影") || text.contains("着色器") || text.toLowerCase(Locale.ROOT).contains("shader")) {
            projectType = "shader";
        } else if (text.contains("资源包") || text.contains("材质")) {
            projectType = "resourcepack";
        } else if (text.contains("数据包")) {
            projectType = "datapack";
        } else if (text.contains("整合包")) {
            projectType = "modpack";
        }
        query = query.replace("搜索", "").replace("帮我", "").replace("找", "")
                .replace("光影", "").replace("资源包", "").replace("材质包", "")
                .replace("数据包", "").replace("整合包", "").replace("模组", "")
                .replace("有没有", "").trim();
        if (query.isEmpty() || query.length() < 2) {
            appendAssistantMessage("请输入要搜索的项目名称，例如：搜模组 sodium 喵~");
            return;
        }
        searchMod(query, projectType);
    }

    private void searchMod(String query, String projectType) {
        setTyping(true);
        appendAssistantMessage("正在搜索「" + query + "」…");
        new AiModSearcher().search(query, projectType, new AiModSearcher.SearchCallback() {
            @Override
            public void onSuccess(List<AiModSearcher.SearchResult> results) {
                setTyping(false);
                if (results.isEmpty()) {
                    appendAssistantMessage("没有找到与「" + query + "」相关的" + typeName(projectType) + "，换个关键词试试喵~");
                    return;
                }
                StringBuilder sb = new StringBuilder();
                sb.append("为你找到了").append(results.size()).append(" 个结果（已按热度排序）：\n\n");
                int index = 1;
                for (AiModSearcher.SearchResult r : results) {
                    sb.append(index++).append(". ").append(r.title).append("\n")
                            .append("   作者：").append(r.author)
                            .append("  下载：" + formatDownloads(r.downloads) + "\n")
                            .append("   简介：").append(truncate(r.description, 60)).append("\n")
                            .append("   下载链接：").append(r.projectUrl).append("\n\n");
                }
                sb.append("点击上面的下载链接即可打开对应页面喵~");
                appendAssistantMessage(sb.toString());
            }

            @Override
            public void onFailed(String error) {
                setTyping(false);
                appendAssistantMessage("搜索失败：" + error);
            }
        });
    }

    private String typeName(String type) {
        switch (type) {
            case "shader":
                return "光影";
            case "resourcepack":
                return "资源包";
            case "datapack":
                return "数据包";
            case "modpack":
                return "整合包";
            default:
                return "模组";
        }
    }

    private String formatDownloads(String d) {
        try {
            long v = Long.parseLong(d);
            if (v >= 10000) {
                return (v / 10000) + "万+";
            }
            return String.valueOf(v);
        } catch (Exception e) {
            return d;
        }
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        if (s.length() <= max) return s;
        return s.substring(0, max) + "…";
    }

    private void sendToAi(String text) {
        setTyping(true);
        sending = true;
        List<AiMessage> payload = new ArrayList<>();
        payload.add(new AiMessage(AiMessage.ROLE_SYSTEM, AiLogAnalyzer.buildSystemPrompt(providerManager, "对话")));
        for (ChatMessage msg : messages) {
            if (msg.content == null || msg.content.isEmpty()) continue;
            payload.add(new AiMessage(msg.isUser ? AiMessage.ROLE_USER : AiMessage.ROLE_ASSISTANT, msg.content));
        }
        payload.add(new AiMessage(AiMessage.ROLE_USER, text));

        new AiChatClient().sendChat(providerManager.getActiveProvider(), payload, 0.7, new AiChatClient.ChatCallback() {
            @Override
            public void onSuccess(String reply) {
                sending = false;
                setTyping(false);
                if (reply == null || reply.isEmpty()) {
                    appendAssistantMessage("抱歉，我没有获取到有效回复，请重试喵~");
                    return;
                }
                executeActions(reply);
                appendAssistantMessage(reply);
            }

            @Override
            public void onFailed(String error) {
                sending = false;
                setTyping(false);
                appendAssistantMessage("出错了：" + error + "\n\n你可以在右上角「服务商设置」里检查配置，或切换其他自定义服务商。");
            }
        });
    }

    private void executeActions(String reply) {
        Matcher m = ACTION_PATTERN.matcher(reply);
        while (m.find()) {
            String action = m.group(1);
            String arg = m.group(2);
            actionExecutor.execute(MainActivityHolder.get(), action, arg, result -> appendAssistantMessage("「消息小溪」已执行：" + result));
        }
    }

    private void handleAnalyzeLog() {
        final String logPath = AiLogAnalyzer.findLatestLog(this);
        appendAssistantMessage("开始分析启动器日志…");
        setTyping(true);
        AiLogAnalyzer.analyzeAsync(this, logPath, true, summary -> {
            setTyping(false);
            appendAssistantMessage("【日志分析结果】\n" + summary);
        });
    }

    private void appendUserMessage(String content) {
        messages.add(new ChatMessage(true, content));
        adapter.notifyDataSetChanged();
        scrollToBottom();
    }

    private void appendAssistantMessage(String content) {
        messages.add(new ChatMessage(false, content));
        adapter.notifyDataSetChanged();
        scrollToBottom();
    }

    private void setTyping(boolean typing) {
        typingIndicator.setVisibility(typing ? View.VISIBLE : View.GONE);
    }

    private void scrollToBottom() {
        messageList.post(() -> messageList.setSelection(messageList.getAdapter().getCount() - 1));
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(input.getWindowToken(), 0);
        }
    }

    private void showProviderDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("服务商设置");
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(24, 16, 24, 8);

        TextView roleLabel = new TextView(this);
        roleLabel.setText("当前 AI 角色：消息小溪（猫娘）");
        roleLabel.setTextColor(Color.parseColor("#555555"));
        layout.addView(roleLabel);

        final ListView providerList = new ListView(this);
        providerList.setDivider(new ColorDrawable(Color.TRANSPARENT));
        final List<AiProvider> providers = providerManager.getProviders();
        final ProviderListAdapter providerAdapter = new ProviderListAdapter(this, providers);
        providerList.setAdapter(providerAdapter);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                (int) (providers.size() * 140 * getResources().getDisplayMetrics().density));
        providerList.setLayoutParams(lp);
        layout.addView(providerList);

        TextView addButton = new TextView(this);
        addButton.setText("＋ 添加自定义服务商");
        addButton.setTextColor(Color.parseColor("#5C6BC0"));
        addButton.setGravity(Gravity.CENTER);
        addButton.setPadding(0, 16, 0, 16);
        addButton.setOnClickListener(v -> showAddProviderDialog());
        layout.addView(addButton);

        TextView roleButton = new TextView(this);
        roleButton.setText("✎ 自定义 AI 角色（默认：关心玩家的猫娘 消息小溪）");
        roleButton.setTextColor(Color.parseColor("#5C6BC0"));
        roleButton.setGravity(Gravity.CENTER);
        roleButton.setPadding(0, 8, 0, 8);
        roleButton.setOnClickListener(v -> showRoleDialog());
        layout.addView(roleButton);

        builder.setView(layout);
        builder.setPositiveButton("完成", null);
        builder.show();
    }

    private void showAddProviderDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("添加自定义服务商");
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 16, 48, 16);

        EditText name = new EditText(this);
        name.setHint("名称（例如：我的 OpenAI）");
        layout.addView(name);

        EditText url = new EditText(this);
        url.setHint("Base URL（例如 https://api.openai.com/v1）");
        layout.addView(url);

        EditText key = new EditText(this);
        key.setHint("API Key（令牌）");
        key.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(key);

        EditText model = new EditText(this);
        model.setHint("模型（例如 gpt-4o-mini / auto）");
        model.setText("auto");
        layout.addView(model);

        builder.setView(layout);
        builder.setPositiveButton("保存", (d, w) -> {
            String n = name.getText().toString().trim();
            String u = url.getText().toString().trim();
            String k = key.getText().toString().trim();
            String mo = model.getText().toString().trim();
            if (n.isEmpty() || u.isEmpty() || k.isEmpty()) {
                Toast.makeText(this, "名称、地址和密钥不能为空", Toast.LENGTH_SHORT).show();
                return;
            }
            AiProvider p = new AiProvider(String.valueOf(System.currentTimeMillis()), n, u, k, mo.isEmpty() ? "auto" : mo, false);
            providerManager.addProvider(p);
            Toast.makeText(this, "已添加自定义服务商，可在列表中切换使用", Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }

    private void showRoleDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("自定义 AI 角色");
        EditText roleInput = new EditText(this);
        roleInput.setHint("描述你想让 AI 扮演的角色…");
        roleInput.setMinLines(3);
        roleInput.setText(providerManager.getRole());
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        roleInput.setPadding(pad, pad, pad, pad);
        builder.setView(roleInput);
        builder.setPositiveButton("保存", (d, w) -> {
            String text = roleInput.getText().toString().trim();
            providerManager.setRole(TextUtils.isEmpty(text) ? AiProviderManager.DEFAULT_ROLE : text);
            Toast.makeText(this, "角色已更新喵~", Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("恢复默认", (d, w) -> {
            providerManager.setRole(AiProviderManager.DEFAULT_ROLE);
            Toast.makeText(this, "已恢复默认角色（消息小溪）", Toast.LENGTH_SHORT).show();
        });
        builder.setNeutralButton("取消", null);
        builder.show();
    }

    private class ProviderListAdapter extends BaseAdapter {
        private final Context context;
        private final List<AiProvider> providers;

        ProviderListAdapter(Context context, List<AiProvider> providers) {
            this.context = context;
            this.providers = providers;
        }

        @Override
        public int getCount() {
            return providers.size();
        }

        @Override
        public Object getItem(int position) {
            return providers.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(R.layout.item_ai_provider, parent, false);
                holder = new ViewHolder();
                holder.name = convertView.findViewById(R.id.ai_provider_name);
                holder.detail = convertView.findViewById(R.id.ai_provider_detail);
                holder.state = convertView.findViewById(R.id.ai_provider_state);
                holder.action = convertView.findViewById(R.id.ai_provider_action);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            final AiProvider provider = providers.get(position);
            holder.name.setText(provider.name + (provider.locked ? "（默认·锁定）" : ""));
            holder.detail.setText(provider.baseUrl + "\n模型：" + provider.model);
            boolean active = providerManager.getActiveProvider().id.equals(provider.id);
            holder.state.setText(active ? "✓ 使用中" : "点击切换");
            holder.state.setTextColor(active ? Color.parseColor("#43A047") : Color.parseColor("#5C6BC0"));
            holder.action.setText(provider.locked ? "🔒 不可修改" : "删除");
            holder.action.setEnabled(!provider.locked);
            holder.action.setOnClickListener(v -> {
                if (provider.locked) {
                    Toast.makeText(context, "默认服务商不可修改、不可删除", Toast.LENGTH_SHORT).show();
                    return;
                }
                providerManager.removeProvider(provider.id);
                notifyDataSetChanged();
            });
            holder.state.setOnClickListener(v -> {
                providerManager.setActiveProvider(provider.id);
                Toast.makeText(context, "已切换至：" + provider.name, Toast.LENGTH_SHORT).show();
                notifyDataSetChanged();
            });
            return convertView;
        }
    }

    private static class ViewHolder {
        TextView name;
        TextView detail;
        TextView state;
        TextView action;
    }

    private class MessageAdapter extends BaseAdapter {
        private final Context context;
        private final List<ChatMessage> messages;

        MessageAdapter(Context context, List<ChatMessage> messages) {
            this.context = context;
            this.messages = messages;
        }

        @Override
        public int getCount() {
            return messages.size();
        }

        @Override
        public Object getItem(int position) {
            return messages.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public int getItemViewType(int position) {
            return messages.get(position).isUser ? 0 : 1;
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ChatMessage msg = messages.get(position);
            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(
                        msg.isUser ? R.layout.item_ai_chat_user : R.layout.item_ai_chat_assistant, parent, false);
            }
            TextView content = convertView.findViewById(R.id.ai_msg_content);
            content.setText(msg.content);
            return convertView;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
