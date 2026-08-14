package com.tungsten.hmclpe.launcher.uis.game.download.right.game;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.tungsten.hmclpe.R;
import com.tungsten.hmclpe.launcher.MainActivity;
import com.tungsten.hmclpe.launcher.download.quilt.QuiltGameVersion;
import com.tungsten.hmclpe.launcher.download.quilt.QuiltLoaderVersion;
import com.tungsten.hmclpe.launcher.list.download.minecraft.DownloadQuiltListAdapter;
import com.tungsten.hmclpe.launcher.uis.game.download.DownloadUrlSource;
import com.tungsten.hmclpe.launcher.uis.tools.BaseUI;
import com.tungsten.hmclpe.utils.animation.CustomAnimationUtils;
import com.tungsten.hmclpe.utils.io.NetworkUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class DownloadQuiltUI extends BaseUI implements View.OnClickListener {

    public LinearLayout downloadQuiltUI;

    public String version;
    public boolean install;

    private LinearLayout hintLayout;

    private ListView quiltListView;
    private ProgressBar progressBar;
    private TextView back;

    private static final String OFFICIAL_LOADER_META_URL = "https://meta.quiltmc.org/v3/versions/loader";
    private static final String OFFICIAL_GAME_META_URL = "https://meta.quiltmc.org/v3/versions/game";

    private static final String BMCLAPI_LOADER_META_URL = "https://bmclapi2.bangbang93.com/quilt-meta/v3/versions/loader";
    private static final String BMCLAPI_GAME_META_URL = "https://bmclapi2.bangbang93.com/quilt-meta/v3/versions/game";

    public DownloadQuiltUI(Context context, MainActivity activity) {
        super(context, activity);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        downloadQuiltUI = activity.findViewById(R.id.ui_install_quilt_list);

        View hintView = activity.findViewById(R.id.download_quilt_hint_layout);
        if (hintView instanceof android.widget.LinearLayout) {
            hintLayout = (android.widget.LinearLayout) hintView;
            hintLayout.setOnClickListener(this);
        }

        quiltListView = activity.findViewById(R.id.quilt_version_list);
        progressBar = activity.findViewById(R.id.loading_quilt_list_progress);
        back = activity.findViewById(R.id.back_to_install_ui_quilt);

        back.setOnClickListener(this);
    }

    @Override
    public void onStart() {
        super.onStart();
        activity.showBarTitle(context.getResources().getString(R.string.quilt_list_ui_title),false,true);
        CustomAnimationUtils.showViewFromLeft(downloadQuiltUI,activity,context,true);
        init();
    }

    @Override
    public void onStop() {
        super.onStop();
        CustomAnimationUtils.hideViewToLeft(downloadQuiltUI,activity,context,true);
    }

    private void init(){
        new Thread(() -> {
            loadingHandler.sendEmptyMessage(0);
            String loaderUrl;
            String gameUrl;
            if (DownloadUrlSource.getSource(activity.launcherSetting.downloadUrlSource) == 0) {
                loaderUrl = OFFICIAL_LOADER_META_URL;
                gameUrl = OFFICIAL_GAME_META_URL;
            }
            else {
                loaderUrl = BMCLAPI_LOADER_META_URL;
                gameUrl = BMCLAPI_GAME_META_URL;
            }
            ArrayList<QuiltGameVersion> gameVersions = new ArrayList<>();
            ArrayList<QuiltLoaderVersion> loaderVersions = new ArrayList<>();
            final boolean[] success = {false};
            try {
                String gameResponse = NetworkUtils.doGet(NetworkUtils.toURL(gameUrl));
                Gson gson = new Gson();
                QuiltGameVersion[] quiltGameVersions = gson.fromJson(gameResponse, QuiltGameVersion[].class);
                gameVersions.addAll(Arrays.asList(quiltGameVersions));
                ArrayList<String> mcVersions = new ArrayList<>();
                for (QuiltGameVersion version : gameVersions){
                    mcVersions.add(version.version);
                }
                String loaderResponse = NetworkUtils.doGet(NetworkUtils.toURL(loaderUrl));
                QuiltLoaderVersion[] quiltLoaderVersions = gson.fromJson(loaderResponse, QuiltLoaderVersion[].class);
                loaderVersions.addAll(Arrays.asList(quiltLoaderVersions));
                if (!mcVersions.contains(version)){
                    loadingHandler.sendEmptyMessage(2);
                }
                else {
                    DownloadQuiltListAdapter downloadQuiltListAdapter = new DownloadQuiltListAdapter(context,activity,version,loaderVersions,install);
                    activity.runOnUiThread(() -> quiltListView.setAdapter(downloadQuiltListAdapter));
                    loadingHandler.sendEmptyMessage(1);
                    success[0] = true;
                }
            } catch (IOException e) {
                e.printStackTrace();
                activity.runOnUiThread(() -> Toast.makeText(context,
                        "Quilt 版本列表加载失败：" + e.getMessage()
                                + "\n请检查网络或切换下载源（设置 → 启动器 → 下载设置）",
                        Toast.LENGTH_LONG).show());
            }
            if (!success[0]) {
                loadingHandler.sendEmptyMessage(2);
            }
        }).start();
    }

    @Override
    public void onClick(View view) {
        if (view == hintLayout){
            Uri uri = Uri.parse("https://afdian.net/@bangbang93");
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            context.startActivity(intent);
        }
        if (view == back){
            activity.backToLastUI();
        }
    }

    @SuppressLint("HandlerLeak")
    private final Handler loadingHandler = new Handler(){
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            if (msg.what == 0){
                quiltListView.setVisibility(View.GONE);
                progressBar.setVisibility(View.VISIBLE);
                back.setVisibility(View.GONE);
            }
            if (msg.what == 1){
                quiltListView.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.GONE);
                back.setVisibility(View.GONE);
            }
            if (msg.what == 2){
                quiltListView.setVisibility(View.GONE);
                progressBar.setVisibility(View.GONE);
                back.setVisibility(View.VISIBLE);
            }
        }
    };
}
