package net.kdt.pojavlaunch.stardock.auth;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
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

/**
 * Step 1: enter the third-party server URL and probe it.
 */
public class ThirdPartyServerFragment extends Fragment {

    public static final String TAG = "THIRD_PARTY_STEP1";
    public static final String ARG_AUTH_ENDPOINT = "auth_endpoint";

    private EditText mUrlInput;
    private MineButton mNextButton;
    private TextView mStatusText;

    public ThirdPartyServerFragment() {
        super(R.layout.fragment_third_party_server);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mUrlInput = view.findViewById(R.id.third_party_url_input);
        mNextButton = view.findViewById(R.id.third_party_url_next);
        mStatusText = view.findViewById(R.id.third_party_url_status);
        ImageButton backButton = view.findViewById(R.id.third_party_back_button);

        // Pre-fill with previously used URL if available.
        Context ctx = requireContext();
        String saved = ctx.getSharedPreferences("third_party_prefs", Context.MODE_PRIVATE)
                .getString("last_url", "");
        if (!TextUtils.isEmpty(saved)) mUrlInput.setText(saved);

        mNextButton.setOnClickListener(v -> probeAndAdvance());
        backButton.setOnClickListener(v -> Tools.backToMainMenu(requireActivity()));
    }

    private void probeAndAdvance() {
        String input = mUrlInput.getText().toString().trim();
        if (!ThirdPartyAuthService.isValidUrl(input)) {
            Toast.makeText(requireContext(), R.string.login_url_invalid, Toast.LENGTH_SHORT).show();
            return;
        }
        String endpoint = ThirdPartyAuthService.resolveAuthEndpoint(input);
        mNextButton.setEnabled(false);
        mStatusText.setText(R.string.login_loading);
        new Thread(() -> {
            boolean ok = false;
            String error = "";
            try {
                ok = ThirdPartyAuthService.probe(endpoint);
            } catch (Exception e) {
                error = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            }
            final boolean probed = ok;
            final String err = error;
            if (getActivity() != null) getActivity().runOnUiThread(() -> {
                mNextButton.setEnabled(true);
                if (probed) {
                    requireContext().getSharedPreferences("third_party_prefs", Context.MODE_PRIVATE)
                            .edit().putString("last_url", input).apply();
                    ThirdPartyCredentialsFragment next = new ThirdPartyCredentialsFragment();
                    Bundle args = new Bundle();
                    args.putString(ARG_AUTH_ENDPOINT, endpoint);
                    next.setArguments(args);
                    Tools.swapFragment(requireActivity(), next.getClass(), ThirdPartyCredentialsFragment.TAG, args);
                } else {
                    mStatusText.setText(getString(R.string.login_failed, err.isEmpty() ? "无法连接" : err));
                }
            });
        }).start();
    }
}
