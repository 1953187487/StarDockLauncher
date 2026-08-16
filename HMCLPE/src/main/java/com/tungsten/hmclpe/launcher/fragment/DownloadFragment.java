package com.tungsten.hmclpe.launcher.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.tungsten.hmclpe.R;
import com.tungsten.hmclpe.ai.AiModSearcher;
import com.tungsten.hmclpe.launcher.fragment.MinecraftVersionService;
import com.tungsten.hmclpe.launcher.fragment.MinecraftVersionService.MinecraftVersion;
import com.tungsten.hmclpe.launcher.fragment.MinecraftVersionService.MinecraftVersionFile;
import com.tungsten.hmclpe.launcher.download.DownloadService;
import com.tungsten.hmclpe.launcher.download.ModrinthService;
import com.tungsten.hmclpe.launcher.setting.AppPrefs;
import com.tungsten.hmclpe.launcher.setting.DownloadSource;
import com.tungsten.hmclpe.launcher.setting.VersionManager;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DownloadFragment extends Fragment implements DownloadService.Listener {

    private static final Pattern MODRINTH_URL = Pattern.compile("modrinth\\.com/(mod|modpack|shader|resourcepack|data-pack|datapack)/([A-Za-z0-9_-]+)");

    private TabLayout tabs;
    private TextInputEditText input;
    private MaterialButton btnSearch;
    private LinearProgressIndicator progressBar;
    private RecyclerView list;
    private TextView empty;
    private ViewGroup activeContainer;

    private final List<DownloadService.Task> completedTasks = new ArrayList<>();
    private final CompletedAdapter completedAdapter = new CompletedAdapter();

    private int currentTab = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_download, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        try {
            tabs = view.findViewById(R.id.download_tabs);
            input = view.findViewById(R.id.download_input);
            btnSearch = view.findViewById(R.id.download_btn_search);
            progressBar = view.findViewById(R.id.download_progress_bar);
            list = view.findViewById(R.id.download_list);
            empty = view.findViewById(R.id.download_empty);
            activeContainer = view.findViewById(R.id.download_active);

            list.setLayoutManager(new LinearLayoutManager(requireContext()));
            list.setAdapter(completedAdapter);
            refreshActive();

            tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override public void onTabSelected(TabLayout.Tab tab) {
                    currentTab = tab.getPosition();
                    if (currentTab == 0) input.setHint("关键词 / 链接（如 sodium）");
                    else if (currentTab == 1) input.setHint("模组名 / Modrinth 链接");
                    else input.setHint("整合包名 / Modrinth 链接");
                }
                @Override public void onTabUnselected(TabLayout.Tab tab) {}
                @Override public void onTabReselected(TabLayout.Tab tab) {}
            });

            btnSearch.setOnClickListener(v -> doSearch());
            input.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    doSearch();
                    return true;
                }
                return false;
            });

            DownloadService.addListener(this);
        } catch (Throwable t) {
            android.util.Log.e("DownloadFragment", "init failed", t);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        try { DownloadService.removeListener(this); } catch (Throwable ignored) {}
    }

    private void doSearch() {
        String q = input.getText().toString().trim();
        if (q.isEmpty()) {
            Toast.makeText(requireContext(), "请输入关键词或链接", Toast.LENGTH_SHORT).show();
            return;
        }
        if (currentTab == 0) {
            searchMinecraftVersions();
            return;
        }
        String type = currentTab == 1 ? "mod" : currentTab == 2 ? "modpack" : "resourcepack";
        Matcher m = MODRINTH_URL.matcher(q);
        if (m.find()) {
            String slug = m.group(2);
            searchBySlug(slug, type);
            return;
        }
        searchByQuery(q, type);
    }

    private void searchMinecraftVersions() {
        progressBar.setVisibility(View.VISIBLE);
        new Thread(() -> {
            final List<MinecraftVersion> list = MinecraftVersionService.list();
            requireActivity().runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                if (list == null || list.isEmpty()) {
                    Toast.makeText(requireContext(), "获取版本列表失败", Toast.LENGTH_SHORT).show();
                    return;
                }
                showVersionPicker(list);
            });
        }).start();
    }

    private void showVersionPicker(List<MinecraftVersion> versions) {
        String[] items = new String[Math.min(versions.size(), 40)];
        for (int i = 0; i < items.length; i++) {
            MinecraftVersion v = versions.get(i);
            items[i] = v.id + " · " + v.type + (v.releaseTime != null ? " · " + v.releaseTime.substring(0, Math.min(10, v.releaseTime.length())) : "");
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("选择游戏版本")
                .setItems(items, (d, which) -> {
                    MinecraftVersion v = versions.get(which);
                    promptInstall(v.id);
                })
                .show();
    }

    private void promptInstall(String mcVersion) {
        final EditTextWrap wrap = EditTextWrap.build(requireContext(), "安装到的版本文件夹名", mcVersion);
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("下载 " + mcVersion)
                .setView(wrap.view)
                .setPositiveButton("开始下载", (d, w) -> {
                    String folder = wrap.edit.getText().toString().trim();
                    if (folder.isEmpty()) folder = mcVersion;
                    installMinecraftVersion(mcVersion, folder);
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void installMinecraftVersion(String mcVersion, String folderName) {
        progressBar.setVisibility(View.VISIBLE);
        new Thread(() -> {
            final List<MinecraftVersionFile> files = MinecraftVersionService.assetsFor(mcVersion);
            requireActivity().runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                if (files == null || files.isEmpty()) {
                    Toast.makeText(requireContext(), "未获取到该版本的下载清单", Toast.LENGTH_SHORT).show();
                    return;
                }
                int source = DownloadSource.current();
                for (MinecraftVersionFile f : files) {
                    DownloadService.Task t = new DownloadService.Task();
                    t.id = f.id;
                    t.name = mcVersion + " · " + f.name;
                    t.url = DownloadSource.url(source, f.path);
                    t.targetVersion = folderName;
                    t.destination = new File(VersionManager.gamesDir(), folderName + "/" + f.name);
                    DownloadService.enqueue(t);
                }
            });
        }).start();
    }

    private void searchByQuery(String q, String type) {
        progressBar.setVisibility(View.VISIBLE);
        new AiModSearcher().search(q, type, new AiModSearcher.SearchCallback() {
            @Override public void onSuccess(List<AiModSearcher.SearchResult> results) {
                requireActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    if (results == null || results.isEmpty()) {
                        Toast.makeText(requireContext(), "无结果", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String[] names = new String[results.size()];
                    for (int i = 0; i < results.size(); i++) names[i] = results.get(i).title;
                    new MaterialAlertDialogBuilder(requireContext())
                            .setTitle("搜索结果")
                            .setItems(names, (d, which) -> pickVersion(results.get(which).slug, results.get(which).title))
                            .show();
                });
            }
            @Override public void onFailed(String error) {
                requireActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), "搜索失败：" + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void searchBySlug(String slug, String type) {
        progressBar.setVisibility(View.VISIBLE);
        new Thread(() -> {
            String mc = pickLocalVersion();
            AppPrefs.setString(requireContext(), AppPrefs.KEY_LAST_GAME_VERSION, mc);
            final List<ModrinthService.VersionInfo> list = ModrinthService.searchVersions(slug, mc);
            final List<String> installedVersions = localInstalledVersions();
            requireActivity().runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                if (list == null || list.isEmpty()) {
                    Toast.makeText(requireContext(), "未找到该项目的兼容版本", Toast.LENGTH_SHORT).show();
                    return;
                }
                pickVersionFromList(slug, list, installedVersions);
            });
        }).start();
    }

    private void pickVersion(String slug, String title) {
        new Thread(() -> {
            String mc = pickLocalVersion();
            AppPrefs.setString(requireContext(), AppPrefs.KEY_LAST_GAME_VERSION, mc);
            List<ModrinthService.VersionInfo> list = ModrinthService.searchVersions(slug, mc);
            final List<String> installedVersions = localInstalledVersions();
            requireActivity().runOnUiThread(() -> {
                if (list == null || list.isEmpty()) {
                    Toast.makeText(requireContext(), "未找到兼容版本", Toast.LENGTH_SHORT).show();
                    return;
                }
                pickVersionFromList(slug, list, installedVersions);
            });
        }).start();
    }

    private String pickLocalVersion() {
        List<String> installed = localInstalledVersions();
        if (!installed.isEmpty()) {
            String last = AppPrefs.getString(requireContext(), AppPrefs.KEY_LAST_GAME_VERSION, "");
            if (last != null && !last.isEmpty() && installed.contains(last)) return last;
            return installed.get(0);
        }
        String fallback = AppPrefs.getString(requireContext(), AppPrefs.KEY_LAST_GAME_VERSION, "");
        if (fallback == null || fallback.isEmpty()) fallback = "1.20.1";
        return fallback;
    }

    private List<String> localInstalledVersions() {
        List<String> list = new ArrayList<>();
        File[] children = VersionManager.gamesDir().listFiles();
        if (children == null) return list;
        for (File c : children) {
            if (c.isDirectory() && (new File(c, "mods").exists() || new File(c, "client.jar").exists() || new File(c, "version.json").exists())) {
                list.add(c.getName());
            }
        }
        java.util.Collections.sort(list);
        return list;
    }

    private void pickVersionFromList(String slug, List<ModrinthService.VersionInfo> list, List<String> installedVersions) {
        String[] names = new String[list.size()];
        int firstInstalled = -1;
        for (int i = 0; i < list.size(); i++) {
            ModrinthService.VersionInfo v = list.get(i);
            String mc = v.gameVersions.isEmpty() ? "?" : v.gameVersions.get(0);
            String tag = "";
            if (!installedVersions.isEmpty() && installedVersions.contains(mc)) {
                tag = " · 本地已安装";
                if (firstInstalled < 0) firstInstalled = i;
            }
            names[i] = v.versionNumber + " · MC " + mc + " · " + (v.loaders.isEmpty() ? "" : v.loaders.get(0)) + tag;
        }
        final int defaultIndex = firstInstalled >= 0 ? firstInstalled : 0;
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(installedVersions.isEmpty() ? "选择文件版本" : "已自动跳到本地版本")
                .setSingleChoiceItems(names, defaultIndex, null)
                .setPositiveButton("下载", (d, w) -> {
                    android.app.AlertDialog dialog = (android.app.AlertDialog) d;
                    int which = dialog.getListView().getCheckedItemPosition();
                    if (which < 0) which = defaultIndex;
                    ModrinthService.VersionInfo v = list.get(which);
                    String mc = v.gameVersions.isEmpty() ? AppPrefs.getString(requireContext(), AppPrefs.KEY_LAST_GAME_VERSION, "1.20.1") : v.gameVersions.get(0);
                    AppPrefs.setString(requireContext(), AppPrefs.KEY_LAST_GAME_VERSION, mc);
                    if (!installedVersions.isEmpty() && installedVersions.contains(mc)) {
                        Toast.makeText(requireContext(), "已自动跳转到本地已安装的 " + mc + " 版本文件夹", Toast.LENGTH_SHORT).show();
                    }
                    promptDownloadWithDeps(slug, v, mc);
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void promptDownloadWithDeps(String slug, ModrinthService.VersionInfo v, String folderName) {
        final EditTextWrap wrap = EditTextWrap.build(requireContext(), "安装到版本文件夹", AppPrefs.getString(requireContext(), AppPrefs.KEY_LAST_GAME_VERSION, "1.20.1"));
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("下载到")
                .setMessage("主文件：" + v.primaryFileName + (v.dependencies.isEmpty() ? "\n无依赖" : "\n依赖 " + v.dependencies.size() + " 个，将一并下载"))
                .setView(wrap.view)
                .setPositiveButton("开始下载（含依赖）", (d, w) -> {
                    String folder = wrap.edit.getText().toString().trim();
                    if (folder.isEmpty()) folder = AppPrefs.getString(requireContext(), AppPrefs.KEY_LAST_GAME_VERSION, "1.20.1");
                    downloadWithDeps(slug, v, folder);
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void downloadWithDeps(String slug, ModrinthService.VersionInfo v, String folderName) {
        Set<String> downloaded = new HashSet<>();
        File modsDir = new File(VersionManager.gamesDir(), folderName + "/mods");
        if (!modsDir.exists()) modsDir.mkdirs();
        enqueueModDownload(v, modsDir, downloaded);
        for (String dep : v.dependencies) {
            new Thread(() -> {
                ModrinthService.VersionInfo depInfo = ModrinthService.fetchVersion(dep);
                if (depInfo != null && !downloaded.contains(depInfo.id)) {
                    requireActivity().runOnUiThread(() -> enqueueModDownload(depInfo, modsDir, downloaded));
                }
            }).start();
        }
    }

    private void enqueueModDownload(ModrinthService.VersionInfo v, File modsDir, Set<String> guard) {
        if (v.primaryFileUrl == null || v.primaryFileName == null) return;
        if (!guard.add(v.id)) return;
        DownloadService.Task t = new DownloadService.Task();
        t.id = v.id;
        t.name = v.primaryFileName;
        t.url = v.primaryFileUrl;
        t.targetVersion = modsDir.getName();
        t.destination = new File(modsDir, v.primaryFileName);
        DownloadService.enqueue(t);
    }

    @Override
    public void onUpdate(List<DownloadService.Task> snapshot) {
        requireActivity().runOnUiThread(() -> {
            refreshActive();
        });
    }

    @Override
    public void onCompleted(DownloadService.Task task) {
        requireActivity().runOnUiThread(() -> {
            completedTasks.add(task);
            completedAdapter.notifyDataSetChanged();
            if (empty != null) empty.setVisibility(View.GONE);
            refreshActive();
        });
    }

    @Override
    public void onFailed(DownloadService.Task task, String error) {
        requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), "下载失败：" + task.name + " · " + error, Toast.LENGTH_SHORT).show());
    }

    private void refreshActive() {
        if (activeContainer == null) return;
        activeContainer.removeAllViews();
        List<DownloadService.Task> snap = DownloadService.snapshot();
        for (DownloadService.Task t : snap) {
            if (t.state == DownloadService.Task.State.COMPLETED || t.state == DownloadService.Task.State.FAILED) continue;
            View v = LayoutInflater.from(requireContext()).inflate(R.layout.item_download_active, activeContainer, false);
            TextView name = v.findViewById(R.id.dl_active_name);
            TextView status = v.findViewById(R.id.dl_active_status);
            com.google.android.material.progressindicator.LinearProgressIndicator bar = v.findViewById(R.id.dl_active_bar);
            name.setText(t.name);
            bar.setProgressCompat(t.progress, true);
            status.setText(t.state.name() + " · " + t.progress + "%");
            v.setOnClickListener(view -> {
                if (t.state == DownloadService.Task.State.RUNNING || t.state == DownloadService.Task.State.QUEUED) {
                    DownloadService.cancel(t);
                    Toast.makeText(requireContext(), "已取消：" + t.name, Toast.LENGTH_SHORT).show();
                }
            });
            activeContainer.addView(v);
        }
    }

    private static class EditTextWrap {
        View view;
        TextInputEditText edit;
        static EditTextWrap build(android.content.Context ctx, String hint, String def) {
            EditTextWrap w = new EditTextWrap();
            w.view = LayoutInflater.from(ctx).inflate(R.layout.dialog_single_input, null);
            w.edit = w.view.findViewById(R.id.dialog_input);
            w.edit.setHint(hint);
            w.edit.setText(def);
            return w;
        }
    }

    private class CompletedAdapter extends RecyclerView.Adapter<CompletedAdapter.VH> {
        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_download_completed, parent, false);
            return new VH(v);
        }
        @Override public void onBindViewHolder(@NonNull VH h, int position) {
            DownloadService.Task t = completedTasks.get(position);
            h.name.setText(t.name);
            h.path.setText("保存到：" + t.destination.getAbsolutePath());
        }
        @Override public int getItemCount() { return completedTasks.size(); }
        class VH extends RecyclerView.ViewHolder {
            TextView name;
            TextView path;
            VH(View v) { super(v); name = v.findViewById(R.id.dl_done_name); path = v.findViewById(R.id.dl_done_path); }
        }
    }
}
