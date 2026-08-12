package net.kdt.pojavlaunch.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.kdt.pojavlaunch.JMinecraftVersionList;
import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.lifecycle.ContextAwareDoneListener;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;
import net.kdt.pojavlaunch.tasks.AsyncMinecraftDownloader;
import net.kdt.pojavlaunch.tasks.AsyncVersionList;
import net.kdt.pojavlaunch.tasks.MinecraftDownloader;
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

    private static final String MODRINTH_API = "https://api.modrinth.com/v2";
    private static final String MOJANG_VERSION_MANIFEST = "https://launchermeta.mojang.com/mc/game/version_manifest.json";

    private enum Mode { MOD, RESOURCEPACK, SHADER, VERSION }

    private final Handler mHandler = new Handler(Looper.getMainLooper());

    private EditText mSearchInput;
    private ListView mResultList;
    private TextView mStatusText;
    private Button mTypeModButton, mTypeResourceButton, mTypeShaderButton, mTypeVersionButton;

    private Mode mCurrentMode = Mode.MOD;
    private final List<Object> mItems = new ArrayList<>();
    private MixAdapter mAdapter;
    private JMinecraftVersionList.Version[] mMojangVersions;

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
        mTypeVersionButton = view.findViewById(R.id.download_type_version);

        Button searchButton = view.findViewById(R.id.download_search_button);
        searchButton.setOnClickListener(v -> onSearch());

        mTypeModButton.setOnClickListener(v -> switchMode(Mode.MOD));
        mTypeResourceButton.setOnClickListener(v -> switchMode(Mode.RESOURCEPACK));
        mTypeShaderButton.setOnClickListener(v -> switchMode(Mode.SHADER));
        mTypeVersionButton.setOnClickListener(v -> switchMode(Mode.VERSION));

        mAdapter = new MixAdapter(requireContext(), mItems);
        mResultList.setAdapter(mAdapter);
        mResultList.setOnItemClickListener((parent, view1, position, id) -> onItemClick(position));

        switchMode(Mode.MOD);
    }

    private void switchMode(Mode mode) {
        mCurrentMode = mode;
        mTypeModButton.setSelected(mode == Mode.MOD);
        mTypeResourceButton.setSelected(mode == Mode.RESOURCEPACK);
        mTypeShaderButton.setSelected(mode == Mode.SHADER);
        mTypeVersionButton.setSelected(mode == Mode.VERSION);

        mItems.clear();
        mAdapter.notifyDataSetChanged();

        if (mode == Mode.VERSION) {
            mSearchInput.setVisibility(View.GONE);
            ((View) mSearchInput.getParent()).findViewById(R.id.download_search_button).setVisibility(View.GONE);
            loadMojangVersions();
        } else {
            mSearchInput.setVisibility(View.VISIBLE);
            ((View) mSearchInput.getParent()).findViewById(R.id.download_search_button).setVisibility(View.VISIBLE);
            setStatus(getString(R.string.download_empty));
        }
    }

    /* ============================= Mojang versions ============================= */

    private void loadMojangVersions() {
        setStatus("正在加载游戏版本列表…");
        new AsyncVersionList().getVersionList(versions -> {
            if (versions == null || versions.versions == null) {
                setStatus("无法加载版本列表，请检查网络");
                return;
            }
            mMojangVersions = versions.versions;
            mItems.clear();
            for (JMinecraftVersionList.Version v : versions.versions) {
                mItems.add(v);
            }
            mAdapter.notifyDataSetChanged();
            setStatus("共 " + versions.versions.length + " 个版本。点击版本一键下载（含依赖）。");
        }, false);
    }

    private void onMojangVersionClick(int position) {
        JMinecraftVersionList.Version version = (JMinecraftVersionList.Version) mItems.get(position);
        new AlertDialog.Builder(requireContext())
                .setTitle(version.id)
                .setMessage("类型：" + version.type
                        + "\n发布时间：" + version.releaseTime
                        + "\n将自动下载游戏本体、依赖库与资源。\n游戏数据大小约 100-300MB，请保持网络通畅。")
                .setPositiveButton("一键下载（含依赖）", (d, w) -> startGameDownload(version))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void startGameDownload(JMinecraftVersionList.Version version) {
        ensureProfileThen(() -> new MinecraftDownloader().start(
                requireActivity(),
                version,
                version.id,
                new ContextAwareDoneListener(requireActivity(), version.id)
        ));
    }

    private void ensureProfileThen(Runnable then) {
        // Make sure at least one profile exists before launching the downloader.
        LauncherProfiles.load();
        if (LauncherProfiles.mainProfileJson == null || LauncherProfiles.mainProfileJson.profiles == null
                || LauncherProfiles.mainProfileJson.profiles.isEmpty()) {
            MinecraftProfile profile = new MinecraftProfile();
            profile.lastVersionId = "1.20.4";
            profile.name = "Default";
            profile.icon = "TNT";
            LauncherProfiles.mainProfileJson.profiles.put(profile.name, profile);
            LauncherProfiles.write();
        }
        LauncherPreferences.DEFAULT_PREF.edit()
                .putString(LauncherPreferences.PREF_KEY_CURRENT_PROFILE, "Default")
                .apply();
        then.run();
    }

    /* ============================= Modrinth ============================= */

    private void onSearch() {
        final String query = mSearchInput.getText().toString().trim();
        if (query.isEmpty()) {
            Toast.makeText(requireContext(), R.string.download_empty, Toast.LENGTH_SHORT).show();
            return;
        }
        setStatus("正在搜索“" + query + "”…");
        new Thread(() -> {
            try {
                String projectType;
                switch (mCurrentMode) {
                    case RESOURCEPACK: projectType = "resourcepack"; break;
                    case SHADER: projectType = "shaderpack"; break;
                    default: projectType = "mod";
                }
                String facets = "[[" + "\"project_type:" + projectType + "\"" + "]]";
                URL url = new URL(MODRINTH_API + "/search?query="
                        + URLEncoder.encode(query, StandardCharsets.UTF_8.name())
                        + "&facets=" + URLEncoder.encode(facets, StandardCharsets.UTF_8.name())
                        + "&limit=25");
                String body = httpGet(url);
                JsonObject root = Tools.GLOBAL_GSON.fromJson(body, JsonObject.class);
                JsonArray hits = root.getAsJsonArray("hits");
                List<Object> result = new ArrayList<>();
                if (hits != null) {
                    for (JsonElement hit : hits) {
                        JsonObject obj = hit.getAsJsonObject();
                        ModrinthProject p = new ModrinthProject();
                        p.id = getString(obj, "project_id");
                        p.title = getString(obj, "title");
                        p.description = getString(obj, "description");
                        result.add(p);
                    }
                }
                mHandler.post(() -> {
                    mItems.clear();
                    mItems.addAll(result);
                    mAdapter.notifyDataSetChanged();
                    setStatus(result.isEmpty() ? "未找到相关内容" : "共 " + result.size() + " 条结果");
                });
            } catch (Exception e) {
                mHandler.post(() -> setStatus("搜索失败：" + e.getMessage()));
            }
        }).start();
    }

    private void onItemClick(int position) {
        Object item = mItems.get(position);
        if (item instanceof ModrinthProject) showProjectOptions((ModrinthProject) item);
        else if (item instanceof JMinecraftVersionList.Version) onMojangVersionClick(position);
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
                File targetDir = getTargetDir();
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
                for (String depId : version.requiredDeps) {
                    try { downloadDependency(depId, targetDir); } catch (Exception ignored) {}
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

    private File getTargetDir() {
        File gameDir = getCurrentGameDir();
        if (gameDir == null) return null;
        switch (mCurrentMode) {
            case RESOURCEPACK: return new File(gameDir, "resourcepacks");
            case SHADER: return new File(gameDir, "shaderpacks");
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
        conn.setRequestProperty("User-Agent", "StarDockLauncher/0.0.3");
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

    /** Adapter that renders either a Modrinth search hit or a Mojang version row. */
    private static class MixAdapter extends BaseAdapter {
        private final android.content.Context mContext;
        private final List<Object> mItems;

        MixAdapter(android.content.Context context, List<Object> items) {
            mContext = context;
            mItems = items;
        }

        @Override public int getCount() { return mItems.size(); }
        @Override public Object getItem(int position) { return mItems.get(position); }
        @Override public long getItemId(int position) { return position; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (mItems.get(position) instanceof JMinecraftVersionList.Version) {
                if (convertView == null || !convertView.getTag().equals("version")) {
                    convertView = LayoutInflater.from(mContext).inflate(R.layout.item_mc_search, parent, false);
                    convertView.setTag("version");
                }
                JMinecraftVersionList.Version v = (JMinecraftVersionList.Version) mItems.get(position);
                TextView title = convertView.findViewById(R.id.item_title);
                TextView desc = convertView.findViewById(R.id.item_description);
                title.setText(v.id);
                desc.setText(v.type + " · " + v.releaseTime);
            } else {
                if (convertView == null || !convertView.getTag().equals("mod")) {
                    convertView = LayoutInflater.from(mContext).inflate(R.layout.item_mc_search, parent, false);
                    convertView.setTag("mod");
                }
                ModrinthProject item = (ModrinthProject) mItems.get(position);
                TextView title = convertView.findViewById(R.id.item_title);
                TextView desc = convertView.findViewById(R.id.item_description);
                title.setText(item.title);
                desc.setText(item.description == null || item.description.isEmpty() ? "点击查看版本" : item.description);
            }
            return convertView;
        }
    }
}
