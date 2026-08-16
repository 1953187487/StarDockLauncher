package com.tungsten.hmclpe.launcher.uis.mods;

import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.stardock.launcher.R;
import com.tungsten.hmclpe.launcher.mod.ModrinthProject;
import com.tungsten.hmclpe.launcher.mod.ModrinthService;
import com.tungsten.hmclpe.launcher.mod.ModrinthVersion;
import com.tungsten.hmclpe.launcher.download.DownloadService;
import com.tungsten.hmclpe.launcher.download.DownloadTask;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ModrinthActivity extends AppCompatActivity {

    private ModrinthService service;
    private final List<ModrinthProject> projects = new ArrayList<>();
    private RecyclerView list;
    private ModrinthAdapter adapter;
    private String currentType = "mod";
    private String currentQuery = "";
    private int offset = 0;
    private final int LIMIT = 20;
    private LinearProgressIndicator progress;
    private boolean loading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_modrinth);
        try {
            service = new ModrinthService();
            MaterialToolbar toolbar = findViewById(R.id.modrinth_toolbar);
            toolbar.setNavigationOnClickListener(v -> finish());
            list = findViewById(R.id.modrinth_list);
            progress = findViewById(R.id.loading_progress_bar);
            if (progress == null) {
                progress = new LinearProgressIndicator(this);
            }
            list.setLayoutManager(new LinearLayoutManager(this));
            adapter = new ModrinthAdapter(this, projects, this::downloadProject);
            list.setAdapter(adapter);
            setupTypeChips();
            setupSearch();
            findViewById(R.id.modrinth_btn_search).setOnClickListener(v -> doSearch());
            findViewById(R.id.modrinth_btn_more).setOnClickListener(v -> loadMore());
            doSearch();
        } catch (Throwable t) {
            AlertDialog.Builder b = new AlertDialog.Builder(this);
            b.setTitle(R.string.download_title);
            b.setMessage("Modrinth 初始化失败：" + t.getMessage());
            b.setPositiveButton(R.string.dialog_ok, (d, w) -> finish());
            b.show();
        }
    }

    private void setupTypeChips() {
        try {
            ChipGroup group = findViewById(R.id.modrinth_type_group);
            group.setOnCheckedStateChangeListener((g, checkedIds) -> {
                if (checkedIds.isEmpty()) {
                    return;
                }
                int id = checkedIds.get(0);
                if (id == R.id.modrinth_chip_mod) {
                    currentType = "mod";
                } else if (id == R.id.modrinth_chip_modpack) {
                    currentType = "modpack";
                } else if (id == R.id.modrinth_chip_resourcepack) {
                    currentType = "resourcepack";
                } else if (id == R.id.modrinth_chip_shader) {
                    currentType = "shader";
                }
                doSearch();
            });
        } catch (Throwable ignored) {
        }
    }

    private void setupSearch() {
        try {
            TextInputEditText input = findViewById(R.id.modrinth_input);
            input.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    doSearch();
                    return true;
                }
                return false;
            });
        } catch (Throwable ignored) {
        }
    }

    private void doSearch() {
        if (loading) {
            return;
        }
        try {
            TextInputEditText input = findViewById(R.id.modrinth_input);
            currentQuery = input.getText() == null ? "" : input.getText().toString().trim();
            offset = 0;
            projects.clear();
            adapter.notifyDataSetChanged();
            fetch(false);
        } catch (Throwable t) {
            showError(t);
        }
    }

    private void loadMore() {
        if (loading) {
            return;
        }
        fetch(true);
    }

    private void fetch(boolean more) {
        loading = true;
        if (progress != null) {
            progress.setVisibility(View.VISIBLE);
        }
        service.search(currentQuery, currentType, LIMIT, offset, new ModrinthService.SearchCallback() {
            @Override
            public void onResult(List<ModrinthProject> list, int total) {
                runOnUiThread(() -> {
                    loading = false;
                    if (progress != null) {
                        progress.setVisibility(View.GONE);
                    }
                    projects.addAll(list);
                    adapter.notifyDataSetChanged();
                    offset += list.size();
                });
            }

            @Override
            public void onError(Throwable t) {
                runOnUiThread(() -> {
                    loading = false;
                    if (progress != null) {
                        progress.setVisibility(View.GONE);
                    }
                    showError(t);
                });
            }
        });
    }

    private void downloadProject(ModrinthProject project) {
        try {
            service.getLatestVersion(project.projectId, new ModrinthService.VersionCallback() {
                @Override
                public void onResult(ModrinthVersion version) {
                    runOnUiThread(() -> {
                        if (version == null || version.primaryUrl() == null) {
                            AlertDialog.Builder b = new AlertDialog.Builder(ModrinthActivity.this);
                            b.setTitle(project.title);
                            b.setMessage("该项目暂无可用下载版本");
                            b.setPositiveButton(R.string.dialog_ok, null);
                            b.show();
                            return;
                        }
                        confirmDownload(project, version);
                    });
                }

                @Override
                public void onError(Throwable t) {
                    runOnUiThread(() -> showError(t));
                }
            });
        } catch (Throwable t) {
            showError(t);
        }
    }

    private void confirmDownload(ModrinthProject project, ModrinthVersion version) {
        String fileName = version.primaryName();
        File target = new File(getExternalFilesDir(null), "mods/" + fileName);
        File target2 = new File(getExternalFilesDir(null), "mods");
        if (!target2.exists()) {
            target2.mkdirs();
        }
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle(project.title);
        b.setMessage(version.versionNumber + "\n" + fileName + "\n" + String.format("%.1f", version.files.get(0).size / 1024f / 1024f) + " MB");
        b.setPositiveButton(R.string.modrinth_download, (d, w) -> startDownload(project, version, target));
        b.setNegativeButton(R.string.dialog_cancel, null);
        b.show();
    }

    private void startDownload(ModrinthProject project, ModrinthVersion version, File target) {
        try {
            AlertDialog progressDialog = new AlertDialog.Builder(this)
                    .setTitle(project.title)
                    .setMessage(R.string.downloading)
                    .create();
            progressDialog.show();
            DownloadTask task = new DownloadTask(version.primaryUrl(), target.getAbsolutePath(), version.primaryName());
            DownloadService.get().download(this, task, new DownloadService.Callback() {
                @Override
                public void onProgress(long downloaded, long total) {
                    runOnUiThread(() -> {
                        String msg = "下载中 %d / %d KB";
                        progressDialog.setMessage(String.format(msg, downloaded / 1024, total / 1024));
                    });
                }

                @Override
                public void onDone(File file) {
                    runOnUiThread(() -> {
                        progressDialog.dismiss();
                        AlertDialog.Builder b = new AlertDialog.Builder(ModrinthActivity.this);
                        b.setMessage("下载完成：" + file.getAbsolutePath());
                        b.setPositiveButton(R.string.dialog_ok, null);
                        b.show();
                    });
                }

                @Override
                public void onError(Throwable t) {
                    runOnUiThread(() -> {
                        progressDialog.dismiss();
                        showError(t);
                    });
                }
            });
        } catch (Throwable t) {
            showError(t);
        }
    }

    private void showError(Throwable t) {
        try {
            AlertDialog.Builder b = new AlertDialog.Builder(this);
            b.setTitle(R.string.download_title);
            b.setMessage("请求失败：" + t.getMessage());
            b.setPositiveButton(R.string.dialog_ok, null);
            b.show();
        } catch (Throwable ignored) {
        }
    }
}
