package com.tungsten.hmclpe.launcher.uis.multiplayer;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;
import com.tungsten.hmclpe.R;

public class MultiplayerActivity extends AppCompatActivity {

    public static final String EXTRA_TUNNEL_FILE_URI = "tunnel_file_uri";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multiplayer);

        TabLayout tabs = findViewById(R.id.multiplayer_tabs);
        ViewPager pager = findViewById(R.id.multiplayer_pager);

        pager.setAdapter(new MultiplayerPagerAdapter(getSupportFragmentManager()));
        tabs.setupWithViewPager(pager);
    }

    private static class MultiplayerPagerAdapter extends FragmentPagerAdapter {

        MultiplayerPagerAdapter(FragmentManager fm) {
            super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        }

        @Override
        public Fragment getItem(int position) {
            return position == 0
                    ? new TerraMultiplayerFragment()
                    : new TunnelMultiplayerFragment();
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return position == 0
                    ? "淘瓦联机"
                    : "内网穿透联机";
        }
    }
}
