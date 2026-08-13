package net.kdt.pojavlaunch.stardock.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
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
import net.kdt.pojavlaunch.extra.ExtraConstants;
import net.kdt.pojavlaunch.extra.ExtraCore;
import net.kdt.pojavlaunch.extra.ExtraListener;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;
import net.kdt.pojavlaunch.progresskeeper.ProgressKeeper;
import net.kdt.pojavlaunch.tasks.AsyncMinecraftDownloader;
import net.kdt.pojavlaunch.tasks.AsyncVersionList;
import net.kdt.pojavlaunch.tasks.MinecraftDownloader;
import net.kdt.pojavlaunch.lifecycle.ContextAwareDoneListener;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * v0.0.6 版本下载中心
 * - 顶部搜索框 + 状态显示
 * - 已下载版本列表（点击查看详情）
 * - 搜索结果列表（点击下载）
 * - 下载源选择（BMCLAPI / Mojang / 自定义）
 */
public class VersionDownloadFragment extends Fragment {
    public static final String TAG = "VersionDownloadFragment";

    private EditText mSearchBox;
    private Button mSearchBtn;
    private Button mRefreshBtn;
    private ListView mSearchResults;
    private ListView mInstalledListView;
    private TextView mStatusText;
    private TextView mInstalledEmpty;
    private TextView mSearchEmpty;

    private SearchAdapter mSearchAdapter;
    private InstalledAdapter mInstalledAdapter;
    private final List<JMinecraftVersionList.Version> mSearchResultsList = new ArrayList<>();
    private final List<JMinecraftVersionList.Version> mInstalledVersions = new ArrayList<>();

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
        mSearchResults = view.findViewById(R.id.vd_search_results);
        mInstalledListView = view.findViewById(R.id.vd_installed_list);
        mStatusText = view.findViewById(R.id.vd_status);
        mInstalledEmpty = view.findViewById(R.id.vd_installed_empty);
        mSearchEmpty = view.findViewById(R.id.vd_search_empty);

        mSearchAdapter = new SearchAdapter();
        mInstalledAdapter = new InstalledAdapter();
        mSearchResults.setAdapter(mSearchAdapter);
        mInstalledListView.setAdapter(mInstalledAdapter);

        mSearchBtn.setOnClickListener(v -> doSearch());
        mRefreshBtn.setOnClickListener(v -> refreshVersionList());

        mSearchResults.setOnItemClickListener((parent, v, position, id) -> {
            JMinecraftVersionList.Version ver = mSearchResultsList.get(position);
            showVersionActionDialog(ver, false);
        });

        mInstalledListView.setOnItemClickListener((parent, v, position, id) -> {
            JMinecraftVersionList.Version ver = mInstalledVersions.get(position);
            showVersionActionDialog(ver, true);
        });

        ExtraCore.addExtraListener(ExtraConstants.RELEASE_TABLE, mReleaseListener);

        refreshVersionList();
        refreshInstalled();
        mStatusText.setText("点击「刷新列表」获取最新版本");
    }

    private final ExtraListener<JMinecraftVersionList.Version[]> mReleaseListener = (key, value) -> {
        if (value == null) return false;
        Tools.runOnUiThread(() -> {
            List<JMinecraftVersionList.Version> all = Arrays.asList(value);
            mSearchResultsList.clear();
            mSearchResultsList.addAll(all);
            mSearchAdapter.notifyDataSetChanged();
            mSearchEmpty.setVisibility(mSearchResultsList.isEmpty() ? View.VISIBLE : View.GONE);
            mStatusText.setText("已加载 " + mSearchResultsList.size() + " 个版本（共" + all.size() + " 个）");
        });
        return false;
    };

    @Override
    public void onDestroyView() {
        ExtraCore.removeExtraListenerFromValue(ExtraConstants.RELEASE_TABLE, mReleaseListener);
        super.onDestroyView();
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshInstalled();
    }

    private void doSearch() {
        String q = mSearchBox.getText().toString().trim().toLowerCase();
        if (q.isEmpty()) {
            mSearchResultsList.clear();
            mSearchAdapter.notifyDataSetChanged();
            mSearchEmpty.setVisibility(View.VISIBLE);
            return;
        }
        if (mSearchResultsList.isEmpty()) {
            refreshVersionList();
        }
        List<JMinecraftVersionList.Version> all = new ArrayList<>(mSearchResultsList);
        mSearchResultsList.clear();
        for (JMinecraftVersionList.Version v : all) {
            if (v.id.toLowerCase().contains(q)) {
                mSearchResultsList.add(v);
            }
        }
        mSearchAdapter.notifyDataSetChanged();
        mSearchEmpty.setVisibility(mSearchResultsList.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void refreshVersionList() {
        mStatusText.setText("正在加载版本列表...");
        new AsyncVersionList().getVersionList(versions -> {
            ExtraCore.setValue(ExtraConstants.RELEASE_TABLE, versions);
        }, false);
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
        mInstalledAdapter.notifyDataSetChanged();
        mInstalledEmpty.setVisibility(mInstalledVersions.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void showVersionActionDialog(JMinecraftVersionList.Version ver, boolean installed) {
        String[] opts;
        if (installed) {
            opts = new String[]{"设为当前版本", "删除版本", "打开版本目录"};
        } else {
            opts = new String[]{"下载此版本", "查看版本详情"};
        }
        new AlertDialog.Builder(requireContext())
                .setTitle(ver.id)
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
        String releaseTime = ver.releaseTime != null ? ver.releaseTime : "未知";
        String content = "版本 ID: " + ver.id + "\n类型: " + type + "\n发布时间: " + releaseTime;
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
        net.kdt.pojavlaunch.value.launcherprofiles.LauncherProfiles.load();
        net.kdt.pojavlaunch.value.launcherprofiles.MinecraftProfile prof =
                net.kdt.pojavlaunch.value.launcherprofiles.LauncherProfiles.mainProfileJson.profiles.get(currentProfile);
        if (prof == null) {
            prof = new net.kdt.pojavlaunch.value.launcherprofiles.MinecraftProfile();
            net.kdt.pojavlaunch.value.launcherprofiles.LauncherProfiles.mainProfileJson.profiles.put(currentProfile, prof);
        }
        prof.lastVersionId = id;
        net.kdt.pojavlaunch.value.launcherprofiles.LauncherProfiles.write();
        Toast.makeText(requireContext(), "已切换到 " + id, Toast.LENGTH_SHORT).show();
        if (getActivity() instanceof LauncherActivity) {
            Fragment current = getActivity().getSupportFragmentManager().findFragmentById(R.id.container_fragment);
            if (current instanceof MainFragmentV3) {
                ((MainFragmentV3) current).refreshVersion();
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
            if (children != null) {
                for (File c : children) deleteRecursive(c);
            }
        }
        f.delete();
    }

    private void openVersionDir(String id) {
        File dir = new File(Tools.DIR_HOME_VERSION, id);
        Tools.openPath(requireContext(), dir, false);
    }

    private class SearchAdapter extends BaseAdapter {
        @Override public int getCount() { return mSearchResultsList.size(); }
        @Override public JMinecraftVersionList.Version getItem(int position) { return mSearchResultsList.get(position); }
        @Override public long getItemId(int position) { return position; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(requireContext())
                        .inflate(android.R.layout.simple_list_item_2, parent, false);
            }
            JMinecraftVersionList.Version v = getItem(position);
            ((android.widget.TextView) convertView.findViewById(android.R.id.text1))
                    .setText(v.id);
            ((android.widget.TextView) convertView.findViewById(android.R.id.text2))
                    .setText((v.type != null ? v.type : "release") + " · " + (v.releaseTime != null ? v.releaseTime.substring(0, Math.min(10, v.releaseTime.length())) : ""));
            return convertView;
        }
    }

    private class InstalledAdapter extends BaseAdapter {
        @Override public int getCount() { return mInstalledVersions.size(); }
        @Override public JMinecraftVersionList.Version getItem(int position) { return mInstalledVersions.get(position); }
        @Override public long getItemId(int position) { return position; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(requireContext())
                        .inflate(android.R.layout.simple_list_item_2, parent, false);
            }
            JMinecraftVersionList.Version v = getItem(position);
            ((android.widget.TextView) convertView.findViewById(android.R.id.text1))
                    .setText(v.id);
            ((android.widget.TextView) convertView.findViewById(android.R.id.text2))
                    .setText("已下载");
            return convertView;
        }
    }
}
