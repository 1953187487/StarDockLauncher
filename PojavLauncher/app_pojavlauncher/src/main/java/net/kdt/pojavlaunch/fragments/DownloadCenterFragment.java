package net.kdt.pojavlaunch.fragments;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;
import net.kdt.pojavlaunch.value.launcherprofiles.LauncherProfiles;
import net.kdt.pojavlaunch.value.launcherprofiles.MinecraftProfile;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class DownloadCenterFragment extends Fragment {

    private static final String TAG = "DownloadCenterFragment";
    private static final String MODRINTH_API = "https://api.modrinth.com/v2";

    private final Handler mHandler = new Handler(Looper.getMainLooper());

    private EditText mSearchInput;
    private ListView mResultList;
    private TextView mStatusText;
    private Button mTypeModButton, mTypeResourceButton, mTypeShaderButton;

    private String mCurrentType = "mod";
    private final List<ModrinthProject> mProjects = new ArrayList<>();
    private ProjectAdapter mAdapter;

    public DownloadCenterFragment() {
        super(R.layout.fragment_download_center);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mSearchInput = view.findViewById(R.id.download_search_input);
        mResultList = view.findViewById(R.id.download_result_list);
        mStatusText = view.findViewById(R.id.download_status_text);
        mTypeModButton = view.findViewById(R.id.download_type_mod);
        mTypeResourceButton = view.findViewById(R.id.download_type_resourcepack);
        mTypeShaderButton = view.findViewById(R.id.download_type_shader);

        Button searchButton = view.findViewById(R.id.download_search_button);
        searchButton.setOnClickListener(v -> doSearch());

        mTypeModButton.setOnClickListener(v -> setType("mod"));
        mTypeResourceButton.setOnClickListener(v -> setType("resourcepack"));
        mTypeShaderButton.setOnClickListener(v -> setType("shaderpack"));

        mAdapter = new ProjectAdapter(requireContext(), mProjects);
        mResultList.setAdapter(mAdapter);
        mResultList.setOnItemClickListener((parent, view1, position, id) -> showProjectOptions(mProjects.get(position)));

        setType(mCurrentType);
        mStatusText.setText(R.string.download_empty);
    }

    private void setType(String type) {
        mCurrentType = type;
        boolean mod = "mod".equals(type);
        boolean resource = "resourcepack".equals(type);
        mTypeModButton.setSelected(mod);
        mTypeResourceButton.setSelected(resource);
        mTypeShaderButton.setSelected(!mod && !resource);
    }

    private void doSearch() {
        final String query = mSearchInput.getText().toString().trim();
        if (query.isEmpty()) {
            Toast.makeText(requireContext(), R.string.download_empty, Toast.LENGTH_SHORT).show();
            return;
        }
        setStatus("正在搜索“" + query + "”…");
        new Thread(() -> {
            try {
                String facets = "[[" + "\"project_type:" + mCurrentType + "\"" + "]]";
                URL url = new URL(MODRINTH_API + "/search?query="
                        + URLEncoder.encode(query, StandardCharsets.UTF_8.name())
                        + "&facets=" + URLEncoder.encode(facets, StandardCharsets.UTF_8.name())
                        + "&limit=25");
                String body = httpGet(url);
                JsonObject root = Tools.GLOBAL_GSON.fromJson(body, JsonObject.class);
                JsonArray hits = root.getAsJsonArray("hits");
                List<ModrinthProject> projects = new ArrayList<>();
                if (hits != null) {
                    for (JsonElement hit : hits) {
                        JsonObject obj = hit.getAsJsonObject();
                        ModrinthProject p = new ModrinthProject();
                        p.id = getString(obj, "project_id");
                        p.title = getString(obj, "title");
                        p.description = getString(obj, "description");
                        projects.add(p);
                    }
                }
                List<ModrinthProject> finalProjects = projects;
                mHandler.post(() -> {
                    mProjects.clear();
                    mProjects.addAll(finalProjects);
                    mAdapter.notifyDataSetChanged();
                    setStatus(finalProjects.isEmpty() ? "未找到相关内容" : "共 " + finalProjects.size() + " 条结果");
                });
            } catch (Exception e) {
                mHandler.post(() -> setStatus("搜索失败：" + e.getMessage()));
            }
        }).start();
    }

    private void showProjectOptions(ModrinthProject project) {
        setStatus("正在获取 " + project.title + " 的版本…");
        new Thread(() -> {
            try {
                URL url = new URL(MODRINTH_API + "/project/" + project.id + "/version");
                String body = httpGet(url);
                JsonArray versions = Tools.GLOBAL_GSON.fromJson(body, JsonArray.class);
                List<ModrinthVersion> versionList = new ArrayList<>();
                for (JsonElement v : versions) {
                    JsonObject obj = v.getAsJsonObject();
                    ModrinthVersion mv = new ModrinthVersion();
                    mv.name = getString(obj, "name");
                    if (obj.getAsJsonArray("files") != null && obj.getAsJsonArray("files").size() > 0) {
                        JsonObject file = obj.getAsJsonArray("files").get(0).getAsJsonObject();
                        mv.fileUrl = getString(file, "url");
                        mv.fileName = getString(file, "filename");
                    }
                    if (obj.getAsJsonArray("dependencies") != null) {
                        for (JsonElement dep : obj.getAsJsonArray("dependencies")) {
                            JsonObject d = dep.getAsJsonObject();
                            if ("required".equals(getString(d, "dependency_type")) && !getString(d, "project_id").isEmpty()) {
                                mv.requiredDeps.add(getString(d, "project_id"));
                            }
                        }
                    }
                    if (mv.fileUrl != null) versionList.add(mv);
                }

                if (versionList.isEmpty()) {
                    mHandler.post(() -> Toast.makeText(requireContext(), "该项目没有可下载的文件", Toast.LENGTH_SHORT).show());
                    return;
                }

                String[] names = new String[versionList.size()];
                for (int i = 0; i < names.length; i++) names[i] = versionList.get(i).name;

                mHandler.post(() -> new AlertDialog.Builder(requireContext())
                        .setTitle(project.title)
                        .setItems(names, (dialog, which) -> downloadVersion(project, versionList.get(which)))
                        .setNegativeButton(android.R.string.cancel, null)
                        .show());
            } catch (Exception e) {
                mHandler.post(() -> Toast.makeText(requireContext(), "获取版本失败：" + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void downloadVersion(ModrinthProject project, ModrinthVersion version) {
        setStatus("正在下载 " + version.fileName + " …");
        new Thread(() -> {
            try {
                File targetDir = getTargetDir(project);
                if (targetDir == null) {
                    mHandler.post(() -> Toast.makeText(requireContext(), "未找到游戏目录，请先创建游戏版本", Toast.LENGTH_SHORT).show());
                    return;
                }
                if (!targetDir.exists()) targetDir.mkdirs();
                Tools.downloadFile(version.fileUrl, new File(targetDir, version.fileName).getAbsolutePath());
                mHandler.post(() -> {
                    setStatus("下载完成：" + version.fileName);
                    Toast.makeText(requireContext(), R.string.download_done, Toast.LENGTH_SHORT).show();
                });

                // One-click required dependencies
                for (String depId : version.requiredDeps) {
                    try {
                        downloadDependency(depId, targetDir);
                    } catch (Exception ignored) {
                    }
                }
            } catch (Exception e) {
                mHandler.post(() -> Toast.makeText(requireContext(), "下载失败：" + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void downloadDependency(String projectId, File targetDir) throws Exception {
        URL url = new URL(MODRINTH_API + "/project/" + projectId + "/version");
        JsonArray versions = Tools.GLOBAL_GSON.fromJson(httpGet(url), JsonArray.class);
        if (versions == null || versions.size() == 0) return;
        JsonObject file = versions.get(0).getAsJsonObject().getAsJsonArray("files").get(0).getAsJsonObject();
        String fileUrl = getString(file, "url");
        String fileName = getString(file, "filename");
        if (fileUrl.isEmpty()) return;
        Tools.downloadFile(fileUrl, new File(targetDir, fileName).getAbsolutePath());
    }

    private File getTargetDir(ModrinthProject project) {
        File gameDir = getCurrentGameDir();
        if (gameDir == null) return null;
        switch (mCurrentType) {
            case "mod": return new File(gameDir, "mods");
            case "resourcepack": return new File(gameDir, "resourcepacks");
            case "shaderpack": return new File(gameDir, "shaderpacks");
            default: return new File(gameDir, "mods");
        }
    }

    private File getCurrentGameDir() {
        String currentProfile = LauncherPreferences.DEFAULT_PREF.getString(LauncherPreferences.PREF_KEY_CURRENT_PROFILE, null);
        if (!Tools.isValidString(currentProfile)) return new File(Tools.DIR_GAME_NEW);
        LauncherProfiles.load();
        MinecraftProfile profileObject = LauncherProfiles.mainProfileJson.profiles.get(currentProfile);
        if (profileObject == null) return new File(Tools.DIR_GAME_NEW);
        return Tools.getGameDirPath(profileObject);
    }

    private void setStatus(String text) {
        mStatusText.setText(text);
    }

    private static String httpGet(URL url) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(15000);
        conn.setRequestProperty("User-Agent", "StarDockLauncher/0.0.1");
        try (InputStream in = conn.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line).append('\n');
            return sb.toString();
        } finally {
            conn.disconnect();
        }
    }

    private static String getString(JsonObject obj, String key) {
        if (obj == null || obj.get(key) == null) return "";
        return obj.get(key).getAsString();
    }

    private static class ModrinthProject {
        String id, title, description;
    }

    private static class ModrinthVersion {
        String name, fileUrl, fileName;
        final List<String> requiredDeps = new ArrayList<>();
    }

    private static class ProjectAdapter extends BaseAdapter {
        private final Context mContext;
        private final List<ModrinthProject> mItems;

        ProjectAdapter(Context context, List<ModrinthProject> items) {
            mContext = context;
            mItems = items;
        }

        @Override public int getCount() { return mItems.size(); }

        @Override public ModrinthProject getItem(int position) { return mItems.get(position); }

        @Override public long getItemId(int position) { return position; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(mContext).inflate(R.layout.item_mc_search, parent, false);
            }
            ModrinthProject item = getItem(position);
            TextView titleView = convertView.findViewById(R.id.item_title);
            TextView descView = convertView.findViewById(R.id.item_description);
            titleView.setText(item.title);
            descView.setText(item.description == null || item.description.isEmpty() ? mContext.getString(R.string.download_deps) : item.description);
            return convertView;
        }
    }
}
