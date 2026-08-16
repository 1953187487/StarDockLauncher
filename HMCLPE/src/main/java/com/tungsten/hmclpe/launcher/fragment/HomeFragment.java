package com.tungsten.hmclpe.launcher.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textview.MaterialTextView;
import com.tungsten.hmclpe.R;
import com.tungsten.hmclpe.launcher.HomeActivity;
import com.tungsten.hmclpe.launcher.setting.AppPrefs;
import com.tungsten.hmclpe.launcher.setting.AuthManager;
import com.tungsten.hmclpe.launcher.setting.SettingNavigation;
import com.tungsten.hmclpe.launcher.setting.VersionManager;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class HomeFragment extends Fragment {

    private RecyclerView versionList;
    private ShapeableImageView avatar;
    private MaterialTextView name;
    private MaterialTextView mode;
    private MaterialButton loginBtn;
    private View accountCard;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        try {
            avatar = view.findViewById(R.id.home_account_avatar);
            name = view.findViewById(R.id.home_account_name);
            mode = view.findViewById(R.id.home_account_mode);
            loginBtn = view.findViewById(R.id.home_account_login_btn);
            accountCard = view.findViewById(R.id.home_account_card);
            versionList = view.findViewById(R.id.home_version_list);

            View downloadCard = view.findViewById(R.id.home_btn_download_card);
            View manageCard = view.findViewById(R.id.home_manage_versions_card);
            View toolsCard = view.findViewById(R.id.home_btn_tools_card);

            if (downloadCard != null) downloadCard.setOnClickListener(v -> SettingNavigation.openDownloadTab(requireActivity()));
            if (manageCard != null) manageCard.setOnClickListener(v -> openManageVersions());
            if (toolsCard != null) toolsCard.setOnClickListener(v -> SettingNavigation.openToolsTab(requireActivity()));
            if (accountCard != null) accountCard.setOnClickListener(v -> openLogin());
            if (loginBtn != null) loginBtn.setOnClickListener(v -> openLogin());

            if (versionList != null) versionList.setLayoutManager(new LinearLayoutManager(requireContext()));
            bindAccount();
            bindVersions();
        } catch (Throwable t) {
            android.util.Log.e("HomeFragment", "init failed", t);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        bindAccount();
        bindVersions();
    }

    private void openLogin() {
        try {
            android.content.Intent i = new android.content.Intent(requireContext(), com.tungsten.hmclpe.launcher.uis.login.AccountActivity.class);
            startActivity(i);
        } catch (Throwable t) {
            android.widget.Toast.makeText(requireContext(), "登录面板未就绪", android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    private void openManageVersions() {
        try {
            android.content.Intent i = new android.content.Intent(requireContext(), com.tungsten.hmclpe.launcher.uis.versions.VersionsActivity.class);
            startActivity(i);
        } catch (Throwable t) {
            android.widget.Toast.makeText(requireContext(), "管理版本页未就绪", android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    private void bindAccount() {
        try {
            int m = AuthManager.currentMode();
            String n = AuthManager.currentNickname();
            boolean offline = (n == null || n.isEmpty());
            if (name != null) name.setText(offline ? "未登录" : n);
            if (mode != null) mode.setText(offline ? "点击登录账号，享受云端皮肤" : AuthManager.name(m));
            if (loginBtn != null) loginBtn.setText(offline ? "登录" : "切换");
            if (avatar != null) {
                avatar.setImageResource(R.drawable.ic_account_placeholder);
                String skin = AppPrefs.getString(requireContext(), AppPrefs.KEY_USER_SKIN_PATH, "");
                if (skin != null && !skin.isEmpty()) {
                    File f = new File(skin);
                    if (f.exists()) {
                        try { avatar.setImageURI(android.net.Uri.fromFile(f)); } catch (Throwable ignored) {}
                    }
                }
            }
        } catch (Throwable ignored) {}
    }

    private void bindVersions() {
        try {
            if (versionList == null) return;
            File gamesDir = VersionManager.gamesDir();
            List<File> files = new ArrayList<>();
            if (gamesDir.exists()) {
                File[] arr = gamesDir.listFiles();
                if (arr != null) {
                    Arrays.sort(arr, new Comparator<File>() {
                        @Override public int compare(File a, File b) {
                            return a.getName().compareToIgnoreCase(b.getName());
                        }
                    });
                    for (File f : arr) if (f.isDirectory()) files.add(f);
                }
            }
            versionList.setAdapter(new VersionAdapter(files));
        } catch (Throwable ignored) {}
    }

    class VersionAdapter extends RecyclerView.Adapter<VersionAdapter.VH> {
        private final List<File> data;
        VersionAdapter(List<File> data) { this.data = data; }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_home_version, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            try {
                File f = data.get(pos);
                String n = f.getName();
                h.title.setText(n);
                File jar = new File(f, "client.jar");
                File json = new File(f, n + ".json");
                File libs = new File(f, "libraries");
                StringBuilder meta = new StringBuilder();
                meta.append(jar.exists() ? "客户端 ✓" : "客户端 ✗").append("  ");
                meta.append(json.exists() ? "配置 ✓" : "配置 ✗").append("  ");
                meta.append(libs.exists() ? "依赖 ✓" : "依赖 ✗");
                h.meta.setText(meta);
                h.itemView.setOnClickListener(v -> {
                    AppPrefs.setString(requireContext(), AppPrefs.KEY_LAST_GAME_VERSION, n);
                    AppPrefs.setString(requireContext(), AppPrefs.KEY_LAST_PROFILE, n);
                    android.widget.Toast.makeText(requireContext(), "已选择版本：" + n + "，点击右下角开始游戏", android.widget.Toast.LENGTH_SHORT).show();
                    if (getActivity() instanceof HomeActivity) {
                        ((HomeActivity) getActivity()).refreshStartFab();
                    }
                });
            } catch (Throwable ignored) {}
        }

        @Override
        public int getItemCount() { return data == null ? 0 : data.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView title;
            TextView meta;
            VH(View v) {
                super(v);
                title = v.findViewById(R.id.item_version_title);
                meta = v.findViewById(R.id.item_version_meta);
            }
        }
    }
}
