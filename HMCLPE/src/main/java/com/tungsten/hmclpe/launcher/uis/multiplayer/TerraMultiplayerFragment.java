package com.tungsten.hmclpe.launcher.uis.multiplayer;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.tungsten.hmclpe.R;

import java.util.UUID;

public class TerraMultiplayerFragment extends Fragment {

    private EditText inviteCodeInput;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_multiplayer_terra, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Button btnCreate = view.findViewById(R.id.btn_terra_create);
        Button btnJoin = view.findViewById(R.id.btn_terra_join);
        inviteCodeInput = view.findViewById(R.id.et_terra_invite_code);

        btnCreate.setOnClickListener(v -> {
            String code = generateTerraInviteCode();
            copyToClipboard(requireContext(), code);
            Toast.makeText(requireContext(),
                    getString(R.string.multiplayer_terra_code_copied) + "\n邀请码：" + code,
                    Toast.LENGTH_LONG).show();
        });

        btnJoin.setOnClickListener(v -> {
            String code = inviteCodeInput.getText().toString().trim();
            if (code.isEmpty()) {
                Toast.makeText(requireContext(), "请粘贴邀请码", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!isValidTerraCode(code)) {
                Toast.makeText(requireContext(), "邀请码格式无效", Toast.LENGTH_SHORT).show();
                return;
            }
            Toast.makeText(requireContext(),
                    "已尝试加入房间：" + code + "\n请启动游戏，在多人游戏中查看",
                    Toast.LENGTH_LONG).show();
        });
    }

    private String generateTerraInviteCode() {
        return "TERRA-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private boolean isValidTerraCode(String code) {
        return code.startsWith("TERRA-") && code.length() == 14;
    }

    private void copyToClipboard(Context ctx, String text) {
        ClipboardManager cm = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null) {
            cm.setPrimaryClip(ClipData.newPlainText("StarDockLauncher 邀请码", text));
        }
    }
}
