package net.kdt.pojavlaunch.fragments;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.stardock.ui.MainFragmentV3;

/**
 * v0.0.5 登录方式选择：离线 / 正版 (Microsoft)
 * 第三方登录入口已移除
 */
public class SelectAuthFragment extends Fragment {
    public static final String TAG = "AUTH_SELECT_FRAGMENT";

    public SelectAuthFragment(){
        super(R.layout.fragment_select_auth_method);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Button mMicrosoftButton = view.findViewById(R.id.button_microsoft_authentication);
        Button mLocalButton = view.findViewById(R.id.button_local_authentication);
        View mThirdPartyButton = view.findViewById(R.id.button_third_party_authentication);

        if (mMicrosoftButton == null || mLocalButton == null) return;

        mMicrosoftButton.setOnClickListener(v ->
                Tools.swapFragment(requireActivity(), MicrosoftLoginFragment.class, MicrosoftLoginFragment.TAG, null));
        mLocalButton.setOnClickListener(v ->
                Tools.swapFragment(requireActivity(), LocalLoginFragment.class, LocalLoginFragment.TAG, null));

        if (mThirdPartyButton != null) {
            mThirdPartyButton.setOnClickListener(v ->
                    Tools.swapFragment(requireActivity(), MainFragmentV3.class, MainFragmentV3.TAG, null));
        }
    }
}
