package net.kdt.pojavlaunch.fragments;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.customcontrols.CustomControls;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AiAssistantFragment extends Fragment {

    private static final String TAG = "AiAssistantFragment";
    private static final String PREF_NAME = "ai_prefs";
    private static final String KEY_BASE_URL = "ai_base_url";
    private static final String KEY_API_KEY = "ai_api_key";
    private static final String KEY_MODEL = "ai_model";
    private static final String KEY_SYSTEM = "ai_system";
    private static final String KEY_TTS = "ai_tts";
    private static final String KEY_TTS_ENGINE = "ai_tts_engine";

    private static final String DEFAULT_MODEL = "gpt-3.5-turbo";
    private static final String DEFAULT_SYSTEM = "你是一位友好的 Minecraft 助手，回答简洁实用。";

    private static final String KEYBIND_PROMPT = "\n\n请同时根据我的需求生成一份键位布局 JSON。格式如下：\n"
            + "{\"version\":8,\"scaledAt\":100,\"mControlDataList\":[{\"name\":\"攻击\",\"keycodes\":[-3],\"dynamicX\":\"${right} - ${margin} * 2 - ${width}\",\"dynamicY\":\"${bottom} - ${margin} * 2 - ${height}\",\"opacity\":1.0,\"displayInGame\":true,\"displayInMenu\":true}],\"mDrawerDataList\":[],\"mJoystickDataList\":[]}\n"
            + "键码说明：W=87 A=65 S=83 D=68 空格=32 E=69 F5=292 F3=294 左Shift=340 Tab=258。特殊键码用负数：键盘=-1 控制开关=-2 鼠标左键=-3 鼠标右键=-4 虚拟鼠标=-5 鼠标中键=-6 滚轮上=-7 滚轮下=-8 菜单=-9。请在回复中只输出 JSON 键位数据，不要其它文字。";

    private final List<ChatMessage> mMessages = new ArrayList<>();
    private MessageAdapter mAdapter;

    private ListView mChatList;
    private EditText mInputView;
    private Button mSendButton;

    private SpeechRecognizer mSpeechRecognizer;
    private boolean mListening = false;
    private TextToSpeech mTts;
    private boolean mTtsReady = false;

    private final ActivityResultLauncher<String> mRecordAudioLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) startVoiceInput();
                else Toast.makeText(requireContext(), R.string.ai_voice_error, Toast.LENGTH_SHORT).show();
            });

    public AiAssistantFragment() {
        super(R.layout.fragment_ai_assistant);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mChatList = view.findViewById(R.id.ai_chat_list);
        mInputView = view.findViewById(R.id.ai_input);
        mSendButton = view.findViewById(R.id.ai_send_button);
        ImageButton keybindButton = view.findViewById(R.id.ai_keybind_button);
        ImageButton clearButton = view.findViewById(R.id.ai_clear_button);
        ImageButton configButton = view.findViewById(R.id.ai_config_button);

        mAdapter = new MessageAdapter(requireContext(), mMessages);
        mChatList.setAdapter(mAdapter);

        mSendButton.setOnClickListener(v -> sendMessage(false));
        mSendButton.setOnLongClickListener(v -> {
            sendMessage(true);
            return true;
        });

        keybindButton.setOnClickListener(v -> askKeybind());
        clearButton.setOnClickListener(v -> {
            mMessages.clear();
            mAdapter.notifyDataSetChanged();
        });
        configButton.setOnClickListener(v -> showConfigDialog());

        mTts = new TextToSpeech(requireContext(), status -> {
            if (status == TextToSpeech.SUCCESS) {
                int langResult = mTts.setLanguage(Locale.CHINESE);
                if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    mTts.setLanguage(Locale.getDefault());
                }
                String engine = getPrefs().getString(KEY_TTS_ENGINE, "");
                if (!engine.isEmpty()) {
                    try { mTts.setEngineByPackageName(engine); } catch (Exception ignored) {}
                }
                mTtsReady = true;
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mSpeechRecognizer != null) {
            mSpeechRecognizer.destroy();
            mSpeechRecognizer = null;
        }
        if (mTts != null) {
            mTts.stop();
            mTts.shutdown();
            mTts = null;
            mTtsReady = false;
        }
    }

    private SharedPreferences getPrefs() {
        return requireContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    private String getBaseUrl() {
        return getPrefs().getString(KEY_BASE_URL, "");
    }

    private String getApiKey() {
        return getPrefs().getString(KEY_API_KEY, "");
    }

    private String getModel() {
        return getPrefs().getString(KEY_MODEL, DEFAULT_MODEL);
    }

    private String getSystemPrompt() {
        return getPrefs().getString(KEY_SYSTEM, DEFAULT_SYSTEM);
    }

    private boolean isTtsEnabled() {
        return getPrefs().getBoolean(KEY_TTS, true);
    }

    /* ============================= Chat ============================= */

    private void addMessage(String sender, String text) {
        mMessages.add(new ChatMessage(sender, text));
        mAdapter.notifyDataSetChanged();
        mChatList.setSelection(mMessages.size() - 1);
    }

    private void sendMessage(boolean voice) {
        if (voice) {
            startVoiceInput();
            return;
        }
        String text = mInputView.getText().toString().trim();
        if (text.isEmpty()) return;
        if (getBaseUrl().isEmpty() || getApiKey().isEmpty()) {
            Toast.makeText(requireContext(), R.string.ai_no_config, Toast.LENGTH_SHORT).show();
            showConfigDialog();
            return;
        }
        mInputView.setText("");
        addMessage("user", text);
        requestAi(false);
    }

    private void requestAi(boolean keybindMode) {
        setInputEnabled(false);
        addMessage("ai", "…");
        final int thinkingIndex = mMessages.size() - 1;
        new Thread(() -> {
            try {
                StringBuilder urlBuilder = new StringBuilder(getBaseUrl().trim());
                while (urlBuilder.charAt(urlBuilder.length() - 1) == '/') {
                    urlBuilder.deleteCharAt(urlBuilder.length() - 1);
                }
                if (!urlBuilder.toString().endsWith("/chat/completions")) {
                    urlBuilder.append("/chat/completions");
                }
                URL url = new URL(urlBuilder.toString());

                JsonArray messages = new JsonArray();
                JsonObject system = new JsonObject();
                system.addProperty("role", "system");
                system.addProperty("content", getSystemPrompt() + (keybindMode ? KEYBIND_PROMPT : ""));
                messages.add(system);
                for (ChatMessage msg : mMessages) {
                    if (msg.sender.equals("user")) {
                        JsonObject userMsg = new JsonObject();
                        userMsg.addProperty("role", "user");
                        userMsg.addProperty("content", msg.text);
                        messages.add(userMsg);
                    }
                }

                JsonObject body = new JsonObject();
                body.addProperty("model", getModel());
                body.add("messages", messages);
                body.addProperty("stream", false);

                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(60000);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Authorization", "Bearer " + getApiKey());
                conn.setRequestProperty("User-Agent", "StarDockLauncher/0.0.1");
                conn.setDoOutput(true);
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(body.toString().getBytes(StandardCharsets.UTF_8));
                }

                int code = conn.getResponseCode();
                String responseBody = readAll(code >= 400 ? conn.getErrorStream() : conn.getInputStream());
                conn.disconnect();

                String content;
                if (code >= 200 && code < 300) {
                    JsonObject root = Tools.GLOBAL_GSON.fromJson(responseBody, JsonObject.class);
                    content = root.getAsJsonArray("choices").get(0).getAsJsonObject()
                            .getAsJsonObject("message").get("content").getAsString();
                } else {
                    content = "请求失败 (HTTP " + code + ")：" + truncate(responseBody, 200);
                }

                final String reply = content;
                runOnUiThreadSafe(() -> {
                    mMessages.remove(thinkingIndex);
                    addMessage("ai", reply);
                    setInputEnabled(true);
                    speak(reply);
                    if (keybindMode) trySaveKeybind(reply);
                });
            } catch (Exception e) {
                runOnUiThreadSafe(() -> {
                    mMessages.remove(thinkingIndex);
                    addMessage("ai", "请求出错：" + e.getMessage());
                    setInputEnabled(true);
                });
            }
        }).start();
    }

    private void runOnUiThreadSafe(Runnable r) {
        if (getActivity() != null) getActivity().runOnUiThread(r);
    }

    private void setInputEnabled(boolean enabled) {
        mInputView.setEnabled(enabled);
        mSendButton.setEnabled(enabled);
    }

    /* ============================= Keybind ============================= */

    private void askKeybind() {
        if (getBaseUrl().isEmpty() || getApiKey().isEmpty()) {
            Toast.makeText(requireContext(), R.string.ai_no_config, Toast.LENGTH_SHORT).show();
            showConfigDialog();
            return;
        }
        final EditText input = new EditText(requireContext());
        input.setHint(R.string.ai_keybind_hint);
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.ai_keybind_title)
                .setView(input)
                .setPositiveButton(R.string.ai_send, (d, w) -> {
                    String need = input.getText().toString().trim();
                    if (need.isEmpty()) {
                        Toast.makeText(requireContext(), R.string.ai_keybind_hint, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    addMessage("user", "请为我的需求生成键位布局：" + need);
                    requestAi(true);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void trySaveKeybind(String aiReply) {
        try {
            List<CustomControls> parsed = extractKeybinds(aiReply);
            if (parsed.isEmpty()) {
                Toast.makeText(requireContext(), R.string.ai_keybind_invalid, Toast.LENGTH_LONG).show();
                return;
            }
            CustomControls layout = parsed.get(0);
            File target = new File(Tools.CTRLDEF_FILE);
            if (!target.getParentFile().exists()) target.getParentFile().mkdirs();
            layout.save(target.getAbsolutePath());
            Toast.makeText(requireContext(), R.string.ai_keybind_saved, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(requireContext(), R.string.ai_keybind_invalid, Toast.LENGTH_LONG).show();
        }
    }

    private List<CustomControls> extractKeybinds(String text) {
        List<CustomControls> result = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int open = text.indexOf('{', start);
            if (open < 0) break;
            int depth = 0;
            for (int i = open; i < text.length(); i++) {
                char c = text.charAt(i);
                if (c == '{') depth++;
                else if (c == '}') {
                    depth--;
                    if (depth == 0) {
                        String candidate = text.substring(open, i + 1);
                        try {
                            CustomControls controls = Tools.GLOBAL_GSON.fromJson(candidate, CustomControls.class);
                            if (controls != null && controls.mControlDataList != null && controls.version == 8) {
                                result.add(controls);
                            }
                        } catch (Exception ignored) {
                        }
                        start = i + 1;
                        break;
                    }
                }
            }
            if (depth != 0) break;
        }
        return result;
    }

    /* ============================= Voice ============================= */

    private void startVoiceInput() {
        Context context = getContext();
        if (context == null) return;
        if (Build.VERSION.SDK_INT >= 23 && ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            mRecordAudioLauncher.launch(Manifest.permission.RECORD_AUDIO);
            return;
        }
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Toast.makeText(context, R.string.ai_voice_error, Toast.LENGTH_SHORT).show();
            return;
        }
        if (mListening) return;
        try {
            if (mSpeechRecognizer == null) {
                mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
            }
            mSpeechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override public void onReadyForSpeech(Bundle params) {
                    mListening = true;
                    Toast.makeText(context, R.string.ai_voice_thinking, Toast.LENGTH_SHORT).show();
                }
                @Override public void onBeginningOfSpeech() {}
                @Override public void onRmsChanged(float rmsdB) {}
                @Override public void onBufferReceived(byte[] buffer) {}
                @Override public void onEndOfSpeech() { mListening = false; }
                @Override public void onError(int error) {
                    mListening = false;
                    Toast.makeText(context, R.string.ai_voice_error, Toast.LENGTH_SHORT).show();
                }
                @Override public void onResults(Bundle results) {
                    mListening = false;
                    ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null && !matches.isEmpty()) {
                        mInputView.setText(matches.get(0));
                        mInputView.setSelection(matches.get(0).length());
                    } else {
                        Toast.makeText(context, R.string.ai_voice_no_result, Toast.LENGTH_SHORT).show();
                    }
                }
                @Override public void onPartialResults(Bundle partialResults) {}
                @Override public void onEvent(int eventType, Bundle params) {}
            });
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
            intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false);
            mSpeechRecognizer.startListening(intent);
        } catch (Exception e) {
            Toast.makeText(context, R.string.ai_voice_error, Toast.LENGTH_SHORT).show();
        }
    }

    private void speak(String text) {
        if (!isTtsEnabled() || !mTtsReady || mTts == null || text == null || text.isEmpty()) return;
        try {
            mTts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "ai_reply");
        } catch (Exception ignored) {
        }
    }

    /* ============================= Config ============================= */

    private void showConfigDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_ai_config, null);
        EditText baseUrlInput = dialogView.findViewById(R.id.ai_cfg_base_url);
        EditText apiKeyInput = dialogView.findViewById(R.id.ai_cfg_api_key);
        EditText modelInput = dialogView.findViewById(R.id.ai_cfg_model);
        EditText systemInput = dialogView.findViewById(R.id.ai_cfg_system);
        EditText engineInput = dialogView.findViewById(R.id.ai_cfg_tts_engine);
        Switch ttsSwitch = dialogView.findViewById(R.id.ai_cfg_tts_switch);

        baseUrlInput.setText(getBaseUrl());
        apiKeyInput.setText(getApiKey());
        modelInput.setText(getModel());
        systemInput.setText(getSystemPrompt());
        engineInput.setText(getPrefs().getString(KEY_TTS_ENGINE, ""));
        ttsSwitch.setChecked(isTtsEnabled());

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.ai_config_title)
                .setView(dialogView)
                .setPositiveButton(R.string.ai_config_save, (d, w) -> getPrefs().edit()
                        .putString(KEY_BASE_URL, baseUrlInput.getText().toString().trim())
                        .putString(KEY_API_KEY, apiKeyInput.getText().toString().trim())
                        .putString(KEY_MODEL, modelInput.getText().toString().trim().isEmpty() ? DEFAULT_MODEL : modelInput.getText().toString().trim())
                        .putString(KEY_SYSTEM, systemInput.getText().toString().trim().isEmpty() ? DEFAULT_SYSTEM : systemInput.getText().toString().trim())
                        .putString(KEY_TTS_ENGINE, engineInput.getText().toString().trim())
                        .putBoolean(KEY_TTS, ttsSwitch.isChecked())
                        .apply())
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    /* ============================= Utils ============================= */

    private static String readAll(InputStream in) throws Exception {
        if (in == null) return "";
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line).append('\n');
            return sb.toString();
        }
    }

    private static String truncate(String text, int max) {
        return text == null || text.length() <= max ? text : text.substring(0, max);
    }

    private static class ChatMessage {
        final String sender;
        final String text;

        ChatMessage(String sender, String text) {
            this.sender = sender;
            this.text = text;
        }
    }

    private static class MessageAdapter extends BaseAdapter {
        private final Context mContext;
        private final List<ChatMessage> mItems;

        MessageAdapter(Context context, List<ChatMessage> items) {
            mContext = context;
            mItems = items;
        }

        @Override public int getCount() { return mItems.size(); }

        @Override public ChatMessage getItem(int position) { return mItems.get(position); }

        @Override public long getItemId(int position) { return position; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(mContext).inflate(R.layout.item_chat_message, parent, false);
            }
            ChatMessage msg = getItem(position);
            LinearLayout bubble = convertView.findViewById(R.id.chat_bubble);
            TextView textView = convertView.findViewById(R.id.chat_text);
            textView.setText(msg.text);
            boolean isUser = "user".equals(msg.sender);
            bubble.setGravity(isUser ? android.view.Gravity.END : android.view.Gravity.START);
            textView.setBackgroundResource(isUser ? R.drawable.bg_msg_user : R.drawable.bg_msg_ai);
            return convertView;
        }
    }
}
