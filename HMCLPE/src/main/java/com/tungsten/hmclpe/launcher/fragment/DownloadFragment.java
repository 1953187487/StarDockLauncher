package com.tungsten.hmclpe.launcher.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textview.MaterialTextView;
import com.tungsten.hmclpe.R;
import com.tungsten.hmclpe.ai.AiModSearcher;
import com.tungsten.hmclpe.launcher.download.DownloadService;
import com.tungsten.hmclpe.launcher.download.ModrinthService;
import com.tungsten.hmclpe.launcher.fragment.MinecraftVersionService;
import com.tungsten.hmclpe.launcher.setting.AppPrefs;
import com.tungsten.hmclpe.launcher.setting.DownloadSource;
import com.tungsten.hmclpe.launcher.setting.VersionManager;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DownloadFragment extends Fragment implements DownloadService.Listener {

    private static final Pattern MODRINTH_URL =
            Pattern.compile("modrinth\\.com/(mod|modpack|shader|resourcepack|data-pack|datapack)/([A-Za-z0-9_-]+)");

    private TabLayout tabs;
    private TextInputEditText input;
    private MaterialButton btnSearch;
    private ChipGroup sourceChips;
    private ChipGroup filterChips;
    private Chip chipBmclapi;
    private Chip chipModrinth;
    private Chip chipCurseforge;
    private LinearProgressIndicator progressBar;
    private RecyclerView grid;
    private RecyclerView completedList;
    private MaterialTextView loadMore;
    private TextView empty;
    private ViewGroup activeContainer;
    private TextView sectionTitle;

    private final List<DownloadService.Task> completedTasks = new ArrayList<>();
    private CompletedAdapter completedAdapter;
    private CardAdapter cardAdapter;

    private int currentTab = 0;
    private String currentSource = "modrinth";
    private String currentFilter = "all";
    private int currentPage = 0;
    private boolean loadingMore = false;
    private boolean hasMore = true;

    private final List<CardItem> currentItems = new ArrayList<>();

    private final Handler main = new Handler(Looper.getMainLooper());

    private static final String[][] DEFAULT_PICKS = {
            {"sodium", "iris", "fabric-api", "lithium", "modmenu", "cloth-config", "lazy-dfc"},
            {"create", "jade", "journeymap", "jei", "twilight-forest", "create-fabric"},
            {"all-the-mods-9", "vault-pickers", "create-astral", "better-mc", "prominence-2-rpg"},
            {"complementary-reimagined", "battered-old-shield", "seus-renewed", "continuity", "photoreal"}
    };

    private static final String[][] GAME_VERSIONS = {
            {"1.21", "1.20.6", "1.20.4", "1.20.1", "1.19.4", "1.18.2", "1.16.5"},
            {"1.20.4", "1.19.2", "1.18.2", "1.16.5", "1.12.2", "1.7.10"},
            {"1.21", "1.20.6", "1.20.4"},
            {"1.21", "1.20.6", "1.20.4", "1.20.1", "1.19.4"}
    };

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
            sourceChips = view.findViewById(R.id.download_source_chips);
            filterChips = view.findViewById(R.id.download_filter_chips);
            chipBmclapi = view.findViewById(R.id.dl_chip_bmclapi);
            chipModrinth = view.findViewById(R.id.dl_chip_modrinth);
            chipCurseforge = view.findViewById(R.id.dl_chip_curseforge);
            progressBar = view.findViewById(R.id.download_progress_bar);
            grid = view.findViewById(R.id.download_grid);
            completedList = view.findViewById(R.id.download_list);
            loadMore = view.findViewById(R.id.download_load_more);
            empty = view.findViewById(R.id.download_empty);
            activeContainer = view.findViewById(R.id.download_active);
            sectionTitle = view.findViewById(R.id.download_section_title);

            setupTabs();
            setupSourceChips();
            setupFilterChips();
            setupSearch();

            cardAdapter = new CardAdapter(currentItems);
            if (grid != null) {
                grid.setLayoutManager(new GridLayoutManager(requireContext(), 2));
                grid.setAdapter(cardAdapter);
            }

            completedAdapter = new CompletedAdapter();
            if (completedList != null) {
                completedList.setLayoutManager(new LinearLayoutManager(requireContext()));
                completedList.setAdapter(completedAdapter);
            }

            if (loadMore != null) {
                loadMore.setOnClickListener(v -> {
                    if (loadingMore || !hasMore) return;
                    currentPage++;
                    loadCurrentTab(true);
                });
            }

            DownloadService.addListener(this);
            refreshActive();

            loadCurrentTab(false);
        } catch (Throwable t) {
            android.util.Log.e("DownloadFragment", "init failed", t);
        }
    }

    @Override
    public void onDestroy() {
        try { DownloadService.removeListener(this); } catch (Throwable ignored) {}
        super.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshActive();
    }

    private void setupTabs() {
        if (tabs == null) return;
        tabs.removeOnTabSelectedListener(null);
        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentTab = tab.getPosition();
                currentPage = 0;
                hasMore = true;
                loadCurrentTab(false);
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupSourceChips() {
        if (sourceChips == null) return;
        sourceChips.setOnCheckedStateChangeListener((g, ids) -> {
            if (ids.isEmpty()) return;
            int id = ids.get(0);
            if (id == R.id.dl_chip_bmclapi) currentSource = "bmclapi";
            else if (id == R.id.dl_chip_modrinth) currentSource = "modrinth";
            else if (id == R.id.dl_chip_curseforge) currentSource = "curseforge";
            currentPage = 0;
            hasMore = true;
            loadCurrentTab(false);
        });
        chipModrinth.setChecked(true);
    }

    private void setupFilterChips() {
        if (filterChips == null) return;
        filterChips.setOnCheckedStateChangeListener((g, ids) -> {
            if (ids.isEmpty()) return;
            int id = ids.get(0);
            if (id == R.id.dl_filter_all) currentFilter = "all";
            else if (id == R.id.dl_filter_release) currentFilter = "release";
            else if (id == R.id.dl_filter_snapshot) currentFilter = "snapshot";
            else if (id == R.id.dl_filter_old_beta) currentFilter = "old_beta";
            else if (id == R.id.dl_filter_old_alpha) currentFilter = "old_alpha";
            currentPage = 0;
            hasMore = true;
            loadCurrentTab(false);
        });
    }

    private void setupSearch() {
        if (btnSearch != null) btnSearch.setOnClickListener(v -> doSearch());
        if (input != null) input.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                doSearch();
                return true;
            }
            return false;
        });
    }

    private void doSearch() {
        String q = input.getText() == null ? "" : input.getText().toString().trim();
        if (q.isEmpty()) {
            Toast.makeText(requireContext(), "请输入关键词", Toast.LENGTH_SHORT).show();
            return;
        }
        if (currentTab == 0) {
            loadGameVersion(q);
            return;
        }
        Matcher m = MODRINTH_URL.matcher(q);
        if (m.find()) {
            String slug = m.group(2);
            searchBySlug(slug, currentTab);
            return;
        }
        runAiSearch(q);
    }

    private void loadCurrentTab(boolean append) {
        if (!append) {
            currentItems.clear();
            cardAdapter.notifyDataSetChanged();
            currentPage = 0;
            hasMore = true;
        }
        if (currentTab == 0) {
            loadGameVersions(append);
        } else {
            loadDefaultPicks(append);
        }
    }

    private void loadGameVersions(boolean append) {
        try {
            String[] defaults;
            if (currentFilter.equals("all")) defaults = GAME_VERSIONS[0];
            else if (currentFilter.equals("release")) defaults = GAME_VERSIONS[1];
            else if (currentFilter.equals("snapshot")) defaults = GAME_VERSIONS[2];
            else if (currentFilter.equals("old_beta")) defaults = GAME_VERSIONS[3];
            else defaults = GAME_VERSIONS[3];

            progressBar.setVisibility(View.VISIBLE);
            new Thread(() -> {
                List<CardItem> items = new ArrayList<>();
                for (String ver : defaults) {
                    try {
                        MinecraftVersionService.MinecraftVersion v = MinecraftVersionService.find(ver);
                        if (v == null) continue;
                        CardItem c = new CardItem();
                        c.id = ver;
                        c.title = "Minecraft " + ver;
                        c.subtitle = v.type + (v.releaseTime != null ? " · " + v.releaseTime : "");
                        c.type = "version";
                        items.add(c);
                    } catch (Throwable ignored) {}
                }
                main.post(() -> {
                    progressBar.setVisibility(View.GONE);
                    if (!append) currentItems.clear();
                    currentItems.addAll(items);
                    cardAdapter.notifyDataSetChanged();
                    hasMore = false;
                    updateEmpty();
                });
            }).start();
        } catch (Throwable t) {
            progressBar.setVisibility(View.GONE);
        }
    }

    private void loadDefaultPicks(boolean append) {
        try {
            String[] defaults = DEFAULT_PICKS[currentTab];
            int pageSize = 6;
            int start = currentPage * pageSize;
            int end = Math.min(start + pageSize, defaults.length);
            if (start >= defaults.length) {
                hasMore = false;
                if (loadMore != null) loadMore.setText("没有更多了");
                return;
            }
            progressBar.setVisibility(View.VISIBLE);
            String[] batch = Arrays.copyOfRange(defaults, start, end);
            new Thread(() -> {
                List<CardItem> items = new ArrayList<>();
                for (String slug : batch) {
                    CardItem c = new CardItem();
                    c.id = slug;
                    c.title = slug;
                    c.subtitle = "热门推荐";
                    c.type = currentTab == 1 ? "mod" : currentTab == 2 ? "modpack" : "shader";
                    items.add(c);
                }
                main.post(() -> {
                    progressBar.setVisibility(View.GONE);
                    if (!append) currentItems.clear();
                    currentItems.addAll(items);
                    cardAdapter.notifyDataSetChanged();
                    if (end >= defaults.length) {
                        hasMore = false;
                        if (loadMore != null) loadMore.setText("没有更多了");
                    } else {
                        if (loadMore != null) loadMore.setText("加载更多");
                    }
                    updateEmpty();
                });
            }).start();
        } catch (Throwable t) {
            progressBar.setVisibility(View.GONE);
        }
    }

    private void loadGameVersion(String keyword) {
        try {
            progressBar.setVisibility(View.VISIBLE);
            new Thread(() -> {
                try {
                    MinecraftVersionService.MinecraftVersion v = MinecraftVersionService.find(keyword);
                    main.post(() -> {
                        progressBar.setVisibility(View.GONE);
                        if (v == null) {
                            Toast.makeText(requireContext(), "未找到版本：" + keyword, Toast.LENGTH_SHORT).show();
                            return;
                        }
                        showGameVersionDetail(v);
                    });
                } catch (Throwable t) {
                    main.post(() -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(requireContext(), "搜索失败：" + t.getMessage(), Toast.LENGTH_LONG).show();
                    });
                }
            }).start();
        } catch (Throwable t) {
            progressBar.setVisibility(View.GONE);
        }
    }

    private void showGameVersionDetail(MinecraftVersionService.MinecraftVersion v) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Minecraft " + v.id)
                .setMessage("类型：" + v.type + "\n发布时间：" + (v.releaseTime == null ? "未知" : v.releaseTime))
                .setPositiveButton("下载", (d, w) -> downloadGameVersion(v))
                .setNegativeButton("关闭", null)
                .show();
    }

    private int sourceToInt() {
        if ("bmclapi".equals(currentSource)) return DownloadSource.BMCLAPI;
        if ("curseforge".equals(currentSource)) return DownloadSource.MOJANG;
        return DownloadSource.BMCLAPI;
    }

    private void downloadGameVersion(MinecraftVersionService.MinecraftVersion v) {
        try {
            String ver = v.id;
            File gameDir = new File(VersionManager.gamesDir(), ver);
            if (!gameDir.exists()) gameDir.mkdirs();
            File jarDest = new File(gameDir, "client.jar");
            File jsonDest = new File(gameDir, ver + ".json");

            DownloadService.Task jarT = new DownloadService.Task();
            jarT.id = "mc-" + ver + "-jar";
            jarT.name = "client.jar";
            jarT.url = DownloadSource.url(sourceToInt(), "mc/game/" + ver + "/client.jar");
            jarT.destination = jarDest;
            jarT.targetVersion = ver;
            DownloadService.enqueue(jarT);

            DownloadService.Task jsonT = new DownloadService.Task();
            jsonT.id = "mc-" + ver + "-json";
            jsonT.name = ver + ".json";
            jsonT.url = DownloadSource.url(sourceToInt(), "mc/game/" + ver + "/" + ver + ".json");
            jsonT.destination = jsonDest;
            jsonT.targetVersion = ver;
            DownloadService.enqueue(jsonT);

            Toast.makeText(requireContext(), "已加入下载队列：" + ver, Toast.LENGTH_SHORT).show();
        } catch (Throwable t) {
            Toast.makeText(requireContext(), "加入队列失败：" + t.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void runAiSearch(String keyword) {
        progressBar.setVisibility(View.VISIBLE);
        new Thread(() -> {
            try {
                final String projectType = currentTab == 1 ? "mod" : currentTab == 2 ? "modpack" : "shader";
                AiModSearcher s = new AiModSearcher();
                List<AiModSearcher.SearchResult> results = s.searchSyncWithSource(keyword, projectType, currentSource);
                main.post(() -> {
                    progressBar.setVisibility(View.GONE);
                    if (results == null || results.isEmpty()) {
                        Toast.makeText(requireContext(), "未找到结果", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    showSearchResults(results);
                });
            } catch (Throwable t) {
                main.post(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), "搜索失败：" + t.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void showSearchResults(List<AiModSearcher.SearchResult> results) {
        String[] names = new String[results.size()];
        for (int i = 0; i < results.size(); i++) {
            String title = results.get(i).title == null ? "" : results.get(i).title;
            String desc = results.get(i).description == null ? "" : results.get(i).description;
            names[i] = title + " · " + desc.substring(0, Math.min(30, desc.length()));
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("搜索结果 · 共 " + results.size() + " 项")
                .setItems(names, (d, which) -> searchBySlug(results.get(which).slug, currentTab))
                .setNegativeButton("关闭", null)
                .show();
    }

    private void searchBySlug(String slug, int type) {
        progressBar.setVisibility(View.VISIBLE);
        new Thread(() -> {
            String mc = pickLocalVersion();
            AppPrefs.setString(requireContext(), AppPrefs.KEY_LAST_GAME_VERSION, mc);
            final List<ModrinthService.VersionInfo> list = ModrinthService.searchVersions(slug, mc);
            final List<String> installedVersions = localInstalledVersions();
            main.post(() -> {
                progressBar.setVisibility(View.GONE);
                if (list == null || list.isEmpty()) {
                    Toast.makeText(requireContext(), "未找到兼容版本", Toast.LENGTH_SHORT).show();
                    return;
                }
                pickVersionFromList(slug, list, installedVersions);
            });
        }).start();
    }

    private void pickVersionFromList(String slug, List<ModrinthService.VersionInfo> list, List<String> installed) {
        String[] names = new String[list.size()];
        for (int i = 0; i < list.size(); i++) {
            ModrinthService.VersionInfo v = list.get(i);
            StringBuilder sb = new StringBuilder();
            sb.append(v.versionNumber == null ? v.name : v.versionNumber);
            if (v.gameVersions != null) {
                for (String gv : v.gameVersions) {
                    if (installed.contains(gv)) {
                        sb.append(" · 可用：").append(gv);
                        break;
                    }
                }
            }
            names[i] = sb.toString();
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(slug + " · 共 " + list.size() + " 项")
                .setItems(names, (d, which) -> {
                    ModrinthService.VersionInfo v = list.get(which);
                    String mc = pickLocalVersion();
                    File modsDir = new File(VersionManager.gamesDir(), mc + "/mods");
                    if (!modsDir.exists()) modsDir.mkdirs();
                    Set<String> guard = new HashSet<>();
                    if (v.dependencies != null) {
                        for (String depId : v.dependencies) {
                            if (depId == null || depId.isEmpty()) continue;
                            ModrinthService.VersionInfo dep = findVersionInfoById(list, depId);
                            if (dep != null) enqueueModDownload(dep, modsDir, guard);
                        }
                    }
                    enqueueModDownload(v, modsDir, guard);
                    Toast.makeText(requireContext(), "已加入下载队列（" + slug + "）", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("关闭", null)
                .show();
    }

    private ModrinthService.VersionInfo findVersionInfoById(List<ModrinthService.VersionInfo> list, String id) {
        for (ModrinthService.VersionInfo v : list) if (id.equals(v.id)) return v;
        return null;
    }

    private String pickLocalVersion() {
        List<String> installed = localInstalledVersions();
        if (!installed.isEmpty()) {
            String last = AppPrefs.getString(requireContext(), AppPrefs.KEY_LAST_GAME_VERSION, "");
            if (last != null && !last.isEmpty() && installed.contains(last)) return last;
            return installed.get(0);
        }
        return AppPrefs.getString(requireContext(), AppPrefs.KEY_LAST_GAME_VERSION, "1.20.4");
    }

    private List<String> localInstalledVersions() {
        List<String> list = new ArrayList<>();
        try {
            File dir = VersionManager.gamesDir();
            if (dir.exists()) {
                File[] arr = dir.listFiles();
                if (arr != null) for (File f : arr) if (f.isDirectory()) list.add(f.getName());
            }
        } catch (Throwable ignored) {}
        return list;
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
        t.category = "mod";
        t.autoTranslate = true;
        DownloadService.enqueue(t);
    }

    private void updateEmpty() {
        if (empty == null) return;
        boolean noCompleted = completedTasks.isEmpty();
        boolean noItems = currentItems.isEmpty();
        empty.setVisibility(noCompleted && noItems ? View.VISIBLE : View.GONE);
    }

    private void refreshActive() {
        if (activeContainer == null) return;
        activeContainer.removeAllViews();
        List<DownloadService.Task> snap = DownloadService.snapshot();
        for (DownloadService.Task t : snap) {
            if (t.state == DownloadService.Task.State.COMPLETED) {
                if (!completedTasks.contains(t)) completedTasks.add(0, t);
                continue;
            }
            View row = LayoutInflater.from(requireContext()).inflate(R.layout.item_active_download, activeContainer, false);
            MaterialTextView title = row.findViewById(R.id.active_title);
            MaterialTextView speed = row.findViewById(R.id.active_speed);
            MaterialTextView size = row.findViewById(R.id.active_size);
            LinearProgressIndicator bar = row.findViewById(R.id.active_progress);
            MaterialButton cancel = row.findViewById(R.id.active_btn_cancel);
            title.setText(t.name);
            bar.setProgressCompat(t.progress, true);
            speed.setText(t.state == DownloadService.Task.State.RUNNING ? "下载中" : t.state.toString());
            size.setText(formatSize(t.downloadedBytes) + " / " + formatSize(t.totalBytes));
            cancel.setOnClickListener(v -> DownloadService.cancel(t));
            activeContainer.addView(row);
        }
        if (completedAdapter != null) {
            completedAdapter.setData(completedTasks);
            completedAdapter.notifyDataSetChanged();
        }
        updateEmpty();
    }

    @Override
    public void onUpdate(List<DownloadService.Task> snapshot) {
        main.post(this::refreshActive);
    }

    @Override
    public void onCompleted(DownloadService.Task task) {
        main.post(this::refreshActive);
    }

    @Override
    public void onFailed(DownloadService.Task task, String error) {
        main.post(() -> Toast.makeText(requireContext(), "下载失败：" + task.name + " · " + error, Toast.LENGTH_LONG).show());
    }

    static class CardItem {
        String id;
        String title;
        String subtitle;
        String type;
    }

    private class CardAdapter extends RecyclerView.Adapter<CardAdapter.VH> {
        private List<CardItem> data;
        CardAdapter(List<CardItem> data) { this.data = data; }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_download_card, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            try {
                CardItem c = data.get(pos);
                h.title.setText(c.title == null ? c.id : c.title);
                h.subtitle.setText(c.subtitle == null ? "" : c.subtitle);
                h.btn.setOnClickListener(v -> {
                    if ("version".equals(c.type)) {
                        MinecraftVersionService.MinecraftVersion v2 = MinecraftVersionService.find(c.id);
                        if (v2 != null) downloadGameVersion(v2);
                    } else {
                        int tab = currentTab;
                        searchBySlug(c.id, tab);
                    }
                });
            } catch (Throwable ignored) {}
        }

        @Override
        public int getItemCount() { return data == null ? 0 : data.size(); }

        class VH extends RecyclerView.ViewHolder {
            MaterialTextView title;
            MaterialTextView subtitle;
            MaterialButton btn;
            VH(View v) {
                super(v);
                title = v.findViewById(R.id.card_title);
                subtitle = v.findViewById(R.id.card_subtitle);
                btn = v.findViewById(R.id.card_btn);
            }
        }
    }

    private class CompletedAdapter extends RecyclerView.Adapter<CompletedAdapter.VH> {
        private List<DownloadService.Task> data = new ArrayList<>();
        void setData(List<DownloadService.Task> data) { this.data = data; }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_active_download, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            try {
                DownloadService.Task t = data.get(pos);
                h.title.setText(t.name);
                h.bar.setProgressCompat(100, true);
                h.speed.setText("已完成");
                h.size.setText(formatSize(t.totalBytes) + " · " + t.targetVersion);
                h.cancel.setText("删除");
                h.cancel.setOnClickListener(v -> {
                    if (t.destination != null && t.destination.exists()) {
                        try { t.destination.delete(); } catch (Throwable ignored) {}
                    }
                    completedTasks.remove(t);
                    notifyDataSetChanged();
                    updateEmpty();
                });
            } catch (Throwable ignored) {}
        }

        @Override
        public int getItemCount() { return data == null ? 0 : data.size(); }

        class VH extends RecyclerView.ViewHolder {
            MaterialTextView title;
            MaterialTextView speed;
            MaterialTextView size;
            LinearProgressIndicator bar;
            MaterialButton cancel;
            VH(View v) {
                super(v);
                title = v.findViewById(R.id.active_title);
                speed = v.findViewById(R.id.active_speed);
                size = v.findViewById(R.id.active_size);
                bar = v.findViewById(R.id.active_progress);
                cancel = v.findViewById(R.id.active_btn_cancel);
            }
        }
    }

    private static String formatSize(long b) {
        if (b <= 0) return "0 B";
        if (b < 1024) return b + " B";
        if (b < 1024 * 1024) return String.format("%.1f KB", b / 1024.0);
        if (b < 1024 * 1024 * 1024) return String.format("%.1f MB", b / 1024.0 / 1024.0);
        return String.format("%.2f GB", b / 1024.0 / 1024.0 / 1024.0);
    }
}
