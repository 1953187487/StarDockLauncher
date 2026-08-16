package com.tungsten.hmclpe.launcher.uis.runtime;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.stardock.launcher.R;
import com.tungsten.hmclpe.launcher.launch.boat.BoatMinecraftActivity;
import com.tungsten.hmclpe.launcher.launch.pojav.PojavMinecraftActivity;
import com.tungsten.hmclpe.launcher.runtime.RuntimePrefs;
import com.tungsten.hmclpe.launcher.version.VersionInfo;
import com.tungsten.hmclpe.launcher.version.VersionManager;
import com.tungsten.hmclpe.runtime.RuntimeInfo;
import com.tungsten.hmclpe.utils.Prefs;

import java.io.File;
import java.util.List;

public class RuntimeActivity extends AppCompatActivity {

    private MaterialCardView cardBoat, cardPojav;
    private TextView boatChecked, pojavChecked;
    private TextView java8, java17;
    private String engine = RuntimePrefs.ENGINE_BOAT;
    private VersionInfo selected;
    private RuntimeVersionAdapter runtimeAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_runtime);
        try {
            MaterialToolbar toolbar = findViewById(R.id.runtime_toolbar);
            toolbar.setNavigationOnClickListener(v -> finish());
            cardBoat = findViewById(R.id.runtime_card_boat);
            cardPojav = findViewById(R.id.runtime_card_pojav);
            boatChecked = findViewById(R.id.runtime_boat_checked);
            pojavChecked = findViewById(R.id.runtime_pojav_checked);
            java8 = findViewById(R.id.runtime_java8);
            java17 = findViewById(R.id.runtime_java17);
            RecyclerView list = findViewById(R.id.runtime_version_list);
            list.setLayoutManager(new LinearLayoutManager(this));
            VersionManager vm = new VersionManager(this);
            List<VersionInfo> installed = vm.installed();
            RuntimeVersionAdapter adapter = new RuntimeVersionAdapter(this, installed, info -> {
                selected = info;
                if (runtimeAdapter != null) {
                    runtimeAdapter.setSelected(info.id);
                }
            });
            runtimeAdapter = adapter;
            list.setAdapter(adapter);
            if (!installed.isEmpty()) {
                selected = installed.get(0);
                adapter.setSelected(selected.id);
            }

            engine = RuntimePrefs.getEngine(this);
            renderEngine();
            cardBoat.setOnClickListener(v -> {
                engine = RuntimePrefs.ENGINE_BOAT;
                RuntimePrefs.setEngine(this, engine);
                renderEngine();
            });
            cardPojav.setOnClickListener(v -> {
                engine = RuntimePrefs.ENGINE_POJAV;
                RuntimePrefs.setEngine(this, engine);
                renderEngine();
            });
            findViewById(R.id.runtime_btn_play).setOnClickListener(v -> startGame());
            showRuntimeInfo();
        } catch (Throwable t) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.settings_runtime_title)
                    .setMessage("初始化失败：" + t.getMessage())
                    .setPositiveButton(R.string.dialog_ok, (d, w) -> finish())
                    .show();
        }
    }

    private void renderEngine() {
        try {
            boolean isBoat = RuntimePrefs.ENGINE_BOAT.equals(engine);
            cardBoat.setStrokeColor(Color.parseColor(isBoat ? "#7C4DFF" : "#33000000"));
            cardBoat.setStrokeWidth(isBoat ? 4 : 1);
            boatChecked.setText(isBoat ? "✓ 当前" : "");
            cardPojav.setStrokeColor(Color.parseColor(!isBoat ? "#7C4DFF" : "#33000000"));
            cardPojav.setStrokeWidth(!isBoat ? 4 : 1);
            pojavChecked.setText(!isBoat ? "✓ 当前" : "");
        } catch (Throwable ignored) {
        }
    }

    private void showRuntimeInfo() {
        try {
            RuntimeInfo info = RuntimeInfo.from(this);
            File j8 = info.java8();
            File j17 = info.java17();
            java8.setText("Java 8: " + (j8 != null ? j8.getAbsolutePath() : "未就绪"));
            java17.setText("Java 17: " + (j17 != null ? j17.getAbsolutePath() : "未就绪"));
        } catch (Throwable t) {
            java8.setText("Java 8: 读取失败");
            java17.setText("Java 17: 读取失败");
        }
    }

    private void startGame() {
        try {
            if (selected == null) {
                new AlertDialog.Builder(this)
                        .setMessage("请先选择游戏版本")
                        .setPositiveButton(R.string.dialog_ok, null)
                        .show();
                return;
            }
            int ram = Prefs.get(this).getInt("launcher_max_ram", 1024);
            File gameDir = RuntimeInfo.gameDir(this);
            if (RuntimePrefs.ENGINE_BOAT.equals(engine)) {
                Intent i = BoatMinecraftActivity.createIntent(this, selected.id, gameDir.getAbsolutePath(), "", "GL4ES", ram);
                startActivity(i);
            } else {
                Intent i = PojavMinecraftActivity.createIntent(this, selected.id, gameDir.getAbsolutePath(), "", "GL4ES", ram);
                startActivity(i);
            }
        } catch (Throwable t) {
            new AlertDialog.Builder(this)
                    .setMessage("启动失败：" + t.getMessage())
                    .setPositiveButton(R.string.dialog_ok, null)
                    .show();
        }
    }
}
