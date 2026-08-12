package net.kdt.pojavlaunch.stardock.auth;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.kdt.mcgui.MineButton;

import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.value.MinecraftAccount;

import java.util.UUID;

/**
 * Step 2: enter the third-party server credentials and log in.
 */
public class ThirdPartyCredentialsFragment extends Fragment {

    public static final String TAG = "THIRD_PARTY_STEP2";

    private String mAuthEndpoint;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mAuthEndpoint = getArguments().getString(ThirdPartyServerFragment.ARG_AUTH_ENDPOINT);
        }
    }

    public ThirdPartyCredentialsFragment() {
        super(R.layout.fragment_third_party_credentials);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        EditText userInput = view.findViewById(R.id.third_party_user_input);
        EditText passInput = view.findViewById(R.id.third_party_pass_input);
        MineButton loginButton = view.findViewById(R.id.third_party_login_button);
        ImageButton backButton = view.findViewById(R.id.third_party_back_button);
        TextView serverText = view.findViewById(R.id.third_party_server_label);
        TextView statusText = view.findViewById(R.id.third_party_status);

        if (mAuthEndpoint != null) serverText.setText(mAuthEndpoint);
        backButton.setOnClickListener(v -> Tools.backToMainMenu(requireActivity()));

        loginButton.setOnClickListener(v -> {
            String username = userInput.getText().toString().trim();
            String password = passInput.getText().toString();
            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), "请输入用户名和密码", Toast.LENGTH_SHORT).show();
                return;
            }
            loginButton.setEnabled(false);
            statusText.setText(R.string.login_loading);
            new Thread(() -> {
                String clientToken = UUID.randomUUID().toString().replace("-", "");
                ThirdPartyAuthService.AccountProfile profile = null;
                String error = null;
                try {
                    profile = ThirdPartyAuthService.authenticate(mAuthEndpoint, username, password, clientToken);
                } catch (Exception e) {
                    error = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
                }
                final ThirdPartyAuthService.AccountProfile finalProfile = profile;
                final String finalError = error;
                if (getActivity() != null) getActivity().runOnUiThread(() -> {
                    loginButton.setEnabled(true);
                    if (finalProfile != null) {
                        saveAccount(finalProfile);
                        statusText.setText(getString(R.string.login_success, finalProfile.name));
                        Toast.makeText(requireContext(),
                                getString(R.string.login_success, finalProfile.name),
                                Toast.LENGTH_LONG).show();
                        Tools.backToMainMenu(requireActivity());
                    } else {
                        statusText.setText(getString(R.string.login_failed,
                                finalError == null ? "未知错误" : finalError));
                    }
                });
            }).start();
        });
    }

    private void saveAccount(ThirdPartyAuthService.AccountProfile profile) {
        try {
            MinecraftAccount account = new MinecraftAccount();
            account.accessToken = profile.accessToken;
            account.clientToken = profile.clientToken;
            account.profileId = profile.uuid;
            account.username = profile.name;
            account.isMicrosoft = false;
            account.save();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
