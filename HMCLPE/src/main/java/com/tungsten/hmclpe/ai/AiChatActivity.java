package com.tungsten.hmclpe.ai;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.tungsten.hmclpe.R;
import com.tungsten.hmclpe.launcher.setting.AppPrefs;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AiChatActivity extends AppCompatActivity {

    private TextInputEditText input;
    private MaterialButton btnSearch;
    private ChipGroup chips;
    private RecyclerView recycler;
    private LinearProgressIndicator progress;
    private ResultAdapter adapter;
    private final AiModSearcher searcher = new AiModSearcher();

    private String currentType = "mod";

    private static final Pattern MODRINTH_URL = Pattern.compile("modrinth\\.com/(mod|modpack|shader|resourcepack|data-pack|datapack)/([A-Za-z0-9_-]+)");
    private static final Pattern CURSEFORGE_URL = Pattern.compile("curseforge\\.com/(minecraft|mc-mods|mc-addons|mc-texture-packs|mc-shaders|mc-modpacks)/([^/]+)/([^/]+)");

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!AppPrefs.getBool(this, AppPrefs.KEY_AI_AGREED, false)) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("AI 服务协议")
                    .setMessage("AI 功能由第三方 API 提供。\n\n" +
                            "您发送的关键词、日志、链接会传递给 AI 服务端用于返回结果。\n\n" +
                            "请勿发送个人敏感信息。")
                    .setPositiveButton("同意", (d, w) -> AppPrefs.setBool(this, AppPrefs.KEY_AI_AGREED, true))
                    .setNegativeButton("取消", (d, w) -> finish())
                    .setCancelable(false)
                    .show();
        }

        setContentView(R.layout.activity_ai_chat);

        MaterialToolbar toolbar = findViewById(R.id.ai_toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        input = findViewById(R.id.ai_search_input);
        btnSearch = findViewById(R.id.ai_btn_search);
        chips = findViewById(R.id.ai_chips);
        recycler = findViewById(R.id.ai_results);
        progress = findViewById(R.id.ai_progress);

        adapter = new ResultAdapter();
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);

        chips.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int id = checkedIds.get(0);
            if (id == R.id.ai_chip_mod) currentType = "mod";
            else if (id == R.id.ai_chip_shader) currentType = "shader";
            else if (id == R.id.ai_chip_resourcepack) currentType = "resourcepack";
            else if (id == R.id.ai_chip_modpack) currentType = "modpack";
        });

        btnSearch.setOnClickListener(v -> doSearch());
        input.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                doSearch();
                return true;
            }
            return false;
        });
    }

    private void doSearch() {
        String q = input.getText().toString().trim();
        if (q.isEmpty()) {
            Toast.makeText(this, "请输入关键词或链接", Toast.LENGTH_SHORT).show();
            return;
        }

        Matcher mr = MODRINTH_URL.matcher(q);
        Matcher cr = CURSEFORGE_URL.matcher(q);
        if (mr.find()) {
            String slug = mr.group(2);
            String type = mr.group(1);
            if (type.equals("data-pack") || type.equals("datapack")) type = "datapack";
            fetchBySlug(slug, type);
            return;
        }
        if (cr.find()) {
            String slug = cr.group(3);
            fetchBySlug(slug, currentType);
            return;
        }
        fetchByQuery(q, currentType);
    }

    private void fetchBySlug(String slug, String type) {
        progress.setVisibility(View.VISIBLE);
        searcher.fetchBySlug(slug, type, new AiModSearcher.SearchCallback() {
            @Override public void onSuccess(List<AiModSearcher.SearchResult> results) {
                runOnUiThread(() -> {
                    progress.setVisibility(View.GONE);
                    adapter.replace(results);
                });
            }
            @Override public void onFailed(String error) {
                runOnUiThread(() -> {
                    progress.setVisibility(View.GONE);
                    Toast.makeText(AiChatActivity.this, "查询失败：" + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void fetchByQuery(String q, String type) {
        progress.setVisibility(View.VISIBLE);
        searcher.search(q, type, new AiModSearcher.SearchCallback() {
            @Override public void onSuccess(List<AiModSearcher.SearchResult> results) {
                runOnUiThread(() -> {
                    progress.setVisibility(View.GONE);
                    adapter.replace(results);
                });
            }
            @Override public void onFailed(String error) {
                runOnUiThread(() -> {
                    progress.setVisibility(View.GONE);
                    Toast.makeText(AiChatActivity.this, "查询失败：" + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private static class ResultAdapter extends RecyclerView.Adapter<ResultAdapter.VH> {
        private final List<AiModSearcher.SearchResult> data = new ArrayList<>();

        void replace(List<AiModSearcher.SearchResult> list) {
            data.clear();
            if (list != null) data.addAll(list);
            notifyDataSetChanged();
        }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ai_search_result, parent, false);
            return new VH(v);
        }

        @Override public void onBindViewHolder(@NonNull VH h, int position) {
            AiModSearcher.SearchResult r = data.get(position);
            h.title.setText(r.title == null ? "" : r.title);
            h.author.setText(r.author == null ? "" : r.author);
            h.desc.setText(r.description == null ? "" : r.description);
            h.itemView.setOnClickListener(v -> {
                try {
                    if (r.projectUrl != null && !r.projectUrl.isEmpty()) {
                        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(r.projectUrl));
                        v.getContext().startActivity(i);
                    }
                } catch (Throwable ignored) {}
            });
        }

        @Override public int getItemCount() { return data.size(); }

        static class VH extends RecyclerView.ViewHolder {
            final TextView title;
            final TextView author;
            final TextView desc;
            VH(View v) {
                super(v);
                title = v.findViewById(R.id.ai_result_title);
                author = v.findViewById(R.id.ai_result_author);
                desc = v.findViewById(R.id.ai_result_desc);
            }
        }
    }
}
