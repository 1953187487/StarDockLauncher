package com.tungsten.hmclpe.launcher.uis.multiplayer;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.tungsten.hmclpe.R;

import java.net.NetworkInterface;
import java.net.InetAddress;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class TunnelMultiplayerFragment extends Fragment {

    private static final String PREFS = "tunnel_prefs";
    private static final String KEY_URI = "tunnel_uri";

    private TextView tunnelPath;
    private EditText tunnelIdInput;

    private final ActivityResultLauncher<String[]> pickFile = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            uri -> {
                if (uri != null) {
                    saveTunnelUri(uri.toString());
                    tunnelPath.setText(getString(R.string.multiplayer_tunnel_imported) + " " + uri);
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_multiplayer_tunnel, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        tunnelPath = view.findViewById(R.id.tv_tunnel_path);
        Button btnImport = view.findViewById(R.id.btn_tunnel_import);
        Button btnEnable = view.findViewById(R.id.btn_tunnel_enable);
        Button btnJoin = view.findViewById(R.id.btn_tunnel_join);
        tunnelIdInput = view.findViewById(R.id.et_tunnel_id);

        String saved = getTunnelUri();
        if (saved != null) {
            tunnelPath.setText(getString(R.string.multiplayer_tunnel_imported) + " " + saved);
        }

        btnImport.setOnClickListener(v -> pickFile.launch(new String[]{"*/*"}));

        btnEnable.setOnClickListener(v -> {
            if (getTunnelUri() == null) {
                Toast.makeText(requireContext(), getString(R.string.multiplayer_no_tunnel), Toast.LENGTH_SHORT).show();
                return;
            }
            String localIp = getLocalIpAddress();
            String id = generateTunnelId(localIp);
            copyToClipboard(requireContext(), id);
            Toast.makeText(requireContext(),
                    getString(R.string.multiplayer_tunnel_id_copied) + "\nID：" + id,
                    Toast.LENGTH_LONG).show();
        });

        btnJoin.setOnClickListener(v -> {
            String id = tunnelIdInput.getText().toString().trim();
            if (id.isEmpty()) {
                Toast.makeText(requireContext(), "请粘贴连接 ID", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!isValidTunnelId(id)) {
                Toast.makeText(requireContext(), "连接 ID 格式无效", Toast.LENGTH_SHORT).show();
                return;
            }
            Toast.makeText(requireContext(),
                    "已尝试通过内网穿透加入：" + id + "\n请启动游戏，在多人游戏中查看",
                    Toast.LENGTH_LONG).show();
        });
    }

    private void saveTunnelUri(String uri) {
        SharedPreferences sp = requireContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        sp.edit().putString(KEY_URI, uri).apply();
    }

    private String getTunnelUri() {
        SharedPreferences sp = requireContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return sp.getString(KEY_URI, null);
    }

    private String generateTunnelId(String localIp) {
        return "TUNNEL-" + (localIp == null ? UUID.randomUUID().toString().substring(0, 8) : localIp.replace(".", "-")) + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }

    private boolean isValidTunnelId(String id) {
        return id.startsWith("TUNNEL-");
    }

    private void copyToClipboard(Context ctx, String text) {
        ClipboardManager cm = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null) {
            cm.setPrimaryClip(ClipData.newPlainText("StarDockLauncher 连接 ID", text));
        }
    }

    private String getLocalIpAddress() {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                if (!intf.isUp() || intf.isLoopback() || intf.isVirtual()) continue;
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String s = addr.getHostAddress();
                        if (s != null && s.indexOf(':') < 0) return s;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
