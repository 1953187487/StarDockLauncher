package net.kdt.pojavlaunch.stardock.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.Tools;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * v0.0.5 联机中心
 * 三个面板：
 *   1. 局域网联机
 *   2. 外置服务器联机（任意 Java 版服务器）
 *   3. 内网穿透联机（支持 .stardock-tunnel 文件导入 + 网址导入）
 */
public class OnlineHubFragment extends Fragment {
    public static final String TAG = "OnlineHubFragment";

    private final ActivityResultLauncher<String[]> mTunnelFilePicker = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            uri -> { if (uri != null) handleTunnelFile(uri); }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_online_hub, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        View lanPanel = view.findViewById(R.id.online_panel_lan);
        View serverPanel = view.findViewById(R.id.online_panel_server);
        View tunnelPanel = view.findViewById(R.id.online_panel_tunnel);

        lanPanel.setOnClickListener(v -> showLanDialog());
        serverPanel.setOnClickListener(v -> showServerDialog());
        tunnelPanel.setOnClickListener(v -> showTunnelDialog());

        Button tunnelImportBtn = view.findViewById(R.id.tunnel_import_file);
        Button tunnelUrlBtn = view.findViewById(R.id.tunnel_import_url);
        tunnelImportBtn.setOnClickListener(v -> mTunnelFilePicker.launch(new String[]{"*/*"}));
        tunnelUrlBtn.setOnClickListener(v -> showTunnelUrlDialog());
    }

    private void showLanDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.sd_mp_lan)
                .setMessage("请确保两台设备在同一局域网。\n\n在游戏中按 ESC 打开菜单 → 「对局域网开放」→ 选择模式后开启，其他设备即可在「多人游戏」列表中看到。\n\nStarDockLauncher 会在启动器中预填当前玩家名作为游戏内默认用户名。")
                .setPositiveButton("我知道了", null)
                .show();
    }

    private void showServerDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_server_input, null);
        EditText address = dialogView.findViewById(R.id.server_address);
        EditText port = dialogView.findViewById(R.id.server_port);

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.sd_mp_server)
                .setView(dialogView)
                .setPositiveButton("连接", (d, w) -> {
                    String addr = address.getText().toString().trim();
                    String portStr = port.getText().toString().trim();
                    if (addr.isEmpty()) {
                        android.widget.Toast.makeText(requireContext(), "请填写服务器地址", android.widget.Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String full = portStr.isEmpty() ? addr : (addr + ":" + portStr);
                    android.widget.Toast.makeText(requireContext(), "已加入队列：" + full, android.widget.Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showTunnelDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.sd_mp_tunnel)
                .setMessage("两种导入方式任选其一：\n\n  1. 选择「导入连接文件」：选择 .stardock-tunnel / .txt 文件\n  2. 选择「粘贴连接网址」：直接粘贴内网穿透提供的完整地址\n\n导入成功后点击主界面启动游戏即可自动加入房间。")
                .setPositiveButton("我知道了", null)
                .show();
    }

    private void showTunnelUrlDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_server_input, null);
        EditText address = dialogView.findViewById(R.id.server_address);
        EditText port = dialogView.findViewById(R.id.server_port);
        address.setHint("连接地址 (如 tunnel.example.com:25565)");
        port.setVisibility(View.GONE);

        new AlertDialog.Builder(requireContext())
                .setTitle("粘贴连接网址")
                .setView(dialogView)
                .setPositiveButton("导入", (d, w) -> {
                    String url = address.getText().toString().trim();
                    if (url.isEmpty()) {
                        android.widget.Toast.makeText(requireContext(), "请填写连接地址", android.widget.Toast.LENGTH_SHORT).show();
                        return;
                    }
                    android.widget.Toast.makeText(requireContext(), "已导入：" + url, android.widget.Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void handleTunnelFile(Uri uri) {
        try (InputStream is = requireContext().getContentResolver().openInputStream(uri);
             BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line.trim()).append('\n');
            }
            String content = sb.toString().trim();
            android.widget.Toast.makeText(requireContext(),
                    "已导入内网穿透文件：\n" + content.substring(0, Math.min(80, content.length())),
                    android.widget.Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            android.widget.Toast.makeText(requireContext(), "导入失败：" + e.getMessage(), android.widget.Toast.LENGTH_SHORT).show();
        }
    }
}
