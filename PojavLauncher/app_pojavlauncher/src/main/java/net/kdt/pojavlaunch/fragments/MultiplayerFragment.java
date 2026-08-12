package net.kdt.pojavlaunch.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.Tools;

public class MultiplayerFragment extends Fragment {

    private static final String PREF_NAME = "network_prefs";
    private static final String KEY_HOST = "tunnel_host";
    private static final String KEY_PORT = "tunnel_port";
    private static final String KEY_TOKEN = "tunnel_token";

    private EditText mHostInput, mPortInput, mTokenInput;

    public MultiplayerFragment() {
        super(R.layout.fragment_multiplayer);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mHostInput = view.findViewById(R.id.mp_custom_host_input);
        mPortInput = view.findViewById(R.id.mp_custom_port_input);
        mTokenInput = view.findViewById(R.id.mp_custom_token_input);

        Button tailscaleButton = view.findViewById(R.id.mp_tunnel_tailscale_button);
        Button zerotierButton = view.findViewById(R.id.mp_tunnel_zerotier_button);
        Button saveButton = view.findViewById(R.id.mp_custom_save_button);

        tailscaleButton.setOnClickListener(v -> openAppOrSite("com.tailscale.ipn", "https://tailscale.com"));
        zerotierButton.setOnClickListener(v -> openAppOrSite("com.zerotier.one", "https://www.zerotier.com"));
        saveButton.setOnClickListener(v -> saveConfig());

        loadConfig();
    }

    private void openAppOrSite(String packageName, String fallbackUrl) {
        try {
            Intent intent = requireContext().getPackageManager().getLaunchIntentForPackage(packageName);
            if (intent != null) {
                startActivity(intent);
                return;
            }
        } catch (Exception ignored) {
        }
        Tools.openURL(requireActivity(), fallbackUrl);
    }

    private void loadConfig() {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        mHostInput.setText(prefs.getString(KEY_HOST, ""));
        mPortInput.setText(prefs.getString(KEY_PORT, ""));
        mTokenInput.setText(prefs.getString(KEY_TOKEN, ""));
    }

    private void saveConfig() {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putString(KEY_HOST, mHostInput.getText().toString().trim())
                .putString(KEY_PORT, mPortInput.getText().toString().trim())
                .putString(KEY_TOKEN, mTokenInput.getText().toString().trim())
                .apply();
        Toast.makeText(requireContext(), R.string.mp_custom_saved, Toast.LENGTH_SHORT).show();
    }
}
