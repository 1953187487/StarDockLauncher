package net.kdt.pojavlaunch.stardock.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import net.kdt.pojavlaunch.JMinecraftVersionList;
import net.kdt.pojavlaunch.LauncherActivity;
import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.lifecycle.ContextAwareDoneListener;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;
import net.kdt.pojavlaunch.progresskeeper.ProgressKeeper;
import net.kdt.pojavlaunch.tasks.AsyncMinecraftDownloader;
import net.kdt.pojavlaunch.tasks.MinecraftDownloader;
import net.kdt.pojavlaunch.value.launcherprofiles.LauncherProfiles;
import net.kdt.pojavlaunch.value.launcherprofiles.MinecraftProfile;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * v0.0.8 版本下载中心（重写 UI 引擎）
 * - 自绘 LinearLayout 卡片，替代 ListView（避免 ScrollView 嵌套问题）
 * - BMCLAPI 镜像 + Mojang 兜底
 * - 实时检查应用更新
 * - 点击版本：下载 / 设为当前 / 删除
 */
public class VersionDownloadFragment extends Fragment {
    public static final String TAG = "VersionDownloadFragment";

    private EditText mSearchBox;
    private Button mSearchBtn;
    private Button mRefreshBtn;
    private LinearLayout mSearchResultsContainer;
    private LinearLayout mInstalledContainer;
    private TextView mStatusText;
    private TextView mInstalledEmpty;
    private TextView mSearchEmpty;
    private Button mCheckUpdateBtn;

    private final List<JMinecraftVersionList.Version> mAllVersions = new ArrayList<>();
    private final List<JMinecraftVersionList.Version> mFilteredVersions = new ArrayList<>();
    private final List<JMinecraftVersionList.Version> mInstalledVersions = new ArrayList<>();

    private static final String GITHUB_LATEST_API = "https://api.github.com/repos/1953187487/StarDockLauncher/releases/latest";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_version_download, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mSearchBox = view.findViewById(R.id.vd_search_box);
        mSearchBtn = view.findViewById(R.id.vd_search_btn);
        mRefreshBtn = view.findViewById(R.id.vd_refresh_btn);
        mSearchResultsContainer = view.findViewById(R.id.vd_search_results);
        mInstalledContainer = view.findViewById(R.id.vd_installed_list);
        mStatusText = view.findViewById(R.id.vd_status);
        mInstalledEmpty = view.findViewById(R.id.vd_installed_empty);
        mSearchEmpty = view.findViewById(R.id.vd_search_empty);
        mCheckUpdateBtn = view.findViewById(R.id.vd_check_update_btn);

        mSearchBtn.setOnClickListener(v -> doSearch());
        mRefreshBtn.setOnClickListener(v -> refreshVersionList());
        mCheckUpdateBtn.setOnClickListener(v -> checkAppUpdate());

        mSearchBox.setOnEditorActionListener((v, actionId, event) -> {
            doSearch();
            return true;
        });

        refreshVersionList();
        refreshInstalled();
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshInstalled();
    }

    private void doSearch() {
        String q = mSearchBox.getText().toString().trim().toLowerCase();
        mFilteredVersions.clear();
        if (q.isEmpty()) {
            mFilteredVersions.addAll(mAllVersions);
        } else {
            for (JMinecraftVersionList.Version v : mAllVersions) {
                if (v.id != null && v.id.toLowerCase().contains(q)) {
                    mFilteredVersions.add(v);
                }
            }
        }
        renderSearchResults();
        mStatusText.setText("搜索结果：" + mFilteredVersions.size() + " 个（共 " + mAllVersions.size() + " 个）");
    }

    private void refreshVersionList() {
        mStatusText.setText("正在从 BMCLAPI 加载版本列表…");
        new AsyncTask<Void, Void, JMinecraftVersionList.Version[]>() {
            @Override protected JMinecraftVersionList.Version[] doInBackground(Void... voids) {
                String[] mirrors = {
                        LauncherPreferences.PREF_VERSION_REPOS,
                        "https://bmclapi2.bangbang93.com/mc/game/version_manifest_v2.json",
                        "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"
                };
                for (String mirror : mirrors) {
                    try {
                        URL url = new URL(mirror);
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        conn.setConnectTimeout(15000);
                        conn.setReadTimeout(20000);
                        conn.connect();
                        if (conn.getResponseCode() != 200) continue;
                        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = br.readLine()) != null) sb.append(line);
                        br.close();
                        JSONObject root = new JSONObject(sb.toString());
                        JSONArray arr = root.getJSONArray("versions");
                        JMinecraftVersionList.Version[] result = new JMinecraftVersionList.Version[arr.length()];
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject obj = arr.getJSONObject(i);
                            JMinecraftVersionList.Version v = new JMinecraftVersionList.Version();
                            v.id = obj.optString("id");
                            v.type = obj.optString("type");
                            v.url = obj.optString("url");
                            v.releaseTime = obj.optString("releaseTime");
                            result[i] = v;
                        }
                        return result;
                    } catch (Exception ignored) {
                    }
                }
                return null;
            }
            @Override protected void onPostExecute(JMinecraftVersionList.Version[] result) {
                if (result == null) {
                    mStatusText.setText("加载失败，请检查网络或更换下载源");
                    return;
                }
                mAllVersions.clear();
                Arrays.sort(result, new Comparator<JMinecraftVersionList.Version>() {
                    @Override
                    public int compare(JMinecraftVersionList.Version a, JMinecraftVersionList.Version b) {
                        if (a.releaseTime == null) return 1;
                        if (b.releaseTime == null) return -1;
                        return b.releaseTime.compareTo(a.releaseTime);
                    }
                });
                Collections.addAll(mAllVersions, result);
                doSearch();
            }
        }.execute();
    }

    private void refreshInstalled() {
        mInstalledVersions.clear();
        File versionsDir = new File(Tools.DIR_HOME_VERSION);
        if (versionsDir.isDirectory()) {
            for (File f : versionsDir.listFiles()) {
                if (f.isDirectory()) {
                    JMinecraftVersionList.Version ver = new JMinecraftVersionList.Version();
                    ver.id = f.getName();
                    ver.type = "local";
                    mInstalledVersions.add(ver);
                }
            }
        }
        renderInstalled();
    }

    private void renderInstalled() {
        mInstalledContainer.removeAllViews();
        mInstalledEmpty.setVisibility(mInstalledVersions.isEmpty() ? View.VISIBLE : View.GONE);
        for (JMinecraftVersionList.Version v : mInstalledVersions) {
            mInstalledContainer.addView(buildVersionCard(v, true));
        }
    }

    private void renderSearchResults() {
        mSearchResultsContainer.removeAllViews();
        mSearchEmpty.setVisibility(mFilteredVersions.isEmpty() ? View.VISIBLE : View.GONE);
        for (JMinecraftVersionList.Version v : mFilteredVersions) {
            mSearchResultsContainer.addView(buildVersionCard(v, false));
        }
    }

    /** 自绘版本卡（替代 ListView+Adapter） */
    private View buildVersionCard(JMinecraftVersionList.Version v, boolean installed) {
        LinearLayout card = new LinearLayout(requireContext());
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setBackgroundResource(R.drawable.background_item);
        card.setPadding(dp(20), dp(16), dp(20), dp(16));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(8);
        card.setLayoutParams(lp);
        card.setGravity(android.view.Gravity.CENTER_VERTICAL);

        LinearLayout info = new LinearLayout(requireContext());
        info.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams infoLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        info.setLayoutParams(infoLp);

        TextView title = new TextView(requireContext());
        title.setText(v.id);
        title.setTextColor(getResources().getColor(R.color.text_primary, requireContext().getTheme()));
        title.setTextSize(15);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        info.addView(title);

        TextView sub = new TextView(requireContext());
        String date = v.releaseTime != null ? v.releaseTime.substring(0, Math.min(10, v.releaseTime.length())) : "";
        String type = v.type != null ? v.type : "release";
        sub.setText(installed ? "已下载" : (type + " · " + date));
        sub.setTextColor(getResources().getColor(R.color.text_secondary, requireContext().getTheme()));
        sub.setTextSize(12);
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subLp.topMargin = dp(2);
        info.addView(sub, subLp);
        card.addView(info);

        TextView arrow = new TextView(requireContext());
        arrow.setText("›");
        arrow.setTextSize(24);
        arrow.setTextColor(getResources().getColor(R.color.brand_primary, requireContext().getTheme()));
        card.addView(arrow);

        card.setOnClickListener(view -> showVersionActionDialog(v, installed));
        return card;
    }

    private int dp(int v) {
        float d = getResources().getDisplayMetrics().density;
        return (int) (v * d);
    }

    private void showVersionActionDialog(JMinecraftVersionList.Version ver, boolean installed) {
        String type = ver.type != null ? ver.type : "release";
        String releaseTime = ver.releaseTime != null
                ? new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(parseIso(ver.releaseTime))
                : "未知";

        String[] opts;
        if (installed) {
            opts = new String[]{"设为当前版本", "删除版本", "打开版本目录"};
        } else {
            opts = new String[]{"下载此版本", "查看版本详情"};
        }
        new AlertDialog.Builder(requireContext())
                .setTitle(ver.id + " · " + type)
                .setMessage("发布时间：" + releaseTime + "\n类型：" + type)
                .setItems(opts, (d, w) -> {
                    if (installed) {
                        switch (w) {
                            case 0: setCurrentVersion(ver.id); break;
                            case 1: confirmDeleteVersion(ver.id); break;
                            case 2: openVersionDir(ver.id); break;
                        }
                    } else {
                        switch (w) {
                            case 0: downloadVersion(ver); break;
                            case 1: showVersionDetail(ver); break;
                        }
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showVersionDetail(JMinecraftVersionList.Version ver) {
        String type = ver.type != null ? ver.type : "release";
        String releaseTime = ver.releaseTime != null
                ? new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(parseIso(ver.releaseTime))
                : "未知";
        String content = "版本 ID: " + ver.id + "\n类型: " + type + "\n发布时间: " + releaseTime + "\nURL: " + ver.url;
        new AlertDialog.Builder(requireContext())
                .setTitle(ver.id)
                .setMessage(content)
                .setPositiveButton("下载", (d, w) -> downloadVersion(ver))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void downloadVersion(JMinecraftVersionList.Version ver) {
        if (ProgressKeeper.getTaskCount() > 0) {
            Toast.makeText(requireContext(), "有任务进行中", Toast.LENGTH_SHORT).show();
            return;
        }
        String versionId = AsyncMinecraftDownloader.normalizeVersionId(ver.id);
        Toast.makeText(requireContext(), "开始下载：" + versionId, Toast.LENGTH_SHORT).show();
        new MinecraftDownloader().start(
                requireActivity(),
                ver,
                versionId,
                new ContextAwareDoneListener(requireActivity(), versionId)
        );
    }

    private void setCurrentVersion(String id) {
        String currentProfile = LauncherPreferences.DEFAULT_PREF.getString(LauncherPreferences.PREF_KEY_CURRENT_PROFILE, "");
        LauncherProfiles.load();
        MinecraftProfile prof = LauncherProfiles.mainProfileJson.profiles.get(currentProfile);
        if (prof == null) {
            prof = new MinecraftProfile();
            LauncherProfiles.mainProfileJson.profiles.put(currentProfile, prof);
        }
        prof.lastVersionId = id;
        LauncherProfiles.write();
        Toast.makeText(requireContext(), "已切换到 " + id, Toast.LENGTH_SHORT).show();
        if (getActivity() instanceof LauncherActivity) {
            androidx.fragment.app.Fragment current = getActivity().getSupportFragmentManager().findFragmentById(R.id.container_fragment);
            if (current instanceof MainFragmentV4) {
                ((MainFragmentV4) current).refreshVersion();
            }
        }
    }

    private void confirmDeleteVersion(String id) {
        new AlertDialog.Builder(requireContext())
                .setTitle("删除版本")
                .setMessage("确认删除 " + id + "？\n相关存档与模组不会被删除。")
                .setPositiveButton("删除", (d, w) -> {
                    File dir = new File(Tools.DIR_HOME_VERSION, id);
                    deleteRecursive(dir);
                    refreshInstalled();
                    Toast.makeText(requireContext(), "已删除：" + id, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private static void deleteRecursive(File f) {
        if (f.isDirectory()) {
            File[] children = f.listFiles();
            if (children != null) for (File c : children) deleteRecursive(c);
        }
        f.delete();
    }

    private void openVersionDir(String id) {
        File dir = new File(Tools.DIR_HOME_VERSION, id);
        Tools.openPath(requireContext(), dir, false);
    }

    private void checkAppUpdate() {
        mCheckUpdateBtn.setEnabled(false);
        mCheckUpdateBtn.setText("检查中…");
        new AsyncTask<Void, Void, String>() {
            @Override protected String doInBackground(Void... voids) {
                try {
                    URL url = new URL(GITHUB_LATEST_API);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(10000);
                    conn.setReadTimeout(15000);
                    conn.setRequestProperty("Accept", "application/vnd.github+json");
                    conn.connect();
                    if (conn.getResponseCode() != 200) return null;
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) sb.append(line);
                    JSONObject root = new JSONObject(sb.toString());
                    return root.optString("tag_name", "");
                } catch (Exception e) {
                    return null;
                }
            }
            @Override protected void onPostExecute(String latestTag) {
                mCheckUpdateBtn.setEnabled(true);
                mCheckUpdateBtn.setText("检查更新");
                if (latestTag == null || latestTag.isEmpty()) {
                    Toast.makeText(requireContext(), "检查更新失败", Toast.LENGTH_SHORT).show();
                    return;
                }
                String current;
                try {
                    current = "v" + requireContext().getPackageManager().getPackageInfo(requireContext().getPackageName(), 0).versionName;
                } catch (Exception e) { current = ""; }
                if (latestTag.equals(current)) {
                    Toast.makeText(requireContext(), "已是最新版本：" + current, Toast.LENGTH_SHORT).show();
                } else {
                    new AlertDialog.Builder(requireContext())
                            .setTitle("发现新版本")
                            .setMessage("当前：" + current + "\n最新：" + latestTag + "\n\n前往 GitHub Releases 下载？")
                            .setPositiveButton("前往", (d, w) ->
                                    Tools.openURL(requireActivity(),
                                            "https://github.com/1953187487/StarDockLauncher/releases/tag/" + latestTag))
                            .setNegativeButton(android.R.string.cancel, null)
                            .show();
                }
            }
        }.execute();
    }

    private static Date parseIso(String s) {
        try { return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).parse(s); }
        catch (Exception e) { return null; }
    }
}
