package net.kdt.pojavlaunch;

import static android.content.res.Configuration.ORIENTATION_PORTRAIT;

import android.Manifest;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentContainerView;
import androidx.fragment.app.FragmentManager;

import com.kdt.mcgui.ProgressLayout;
import com.kdt.mcgui.mcAccountSpinner;

import net.kdt.pojavlaunch.agreement.AgreementDialog;
import net.kdt.pojavlaunch.contracts.OpenDocumentWithExtension;
import net.kdt.pojavlaunch.extra.ExtraConstants;
import net.kdt.pojavlaunch.extra.ExtraCore;
import net.kdt.pojavlaunch.extra.ExtraListener;
import net.kdt.pojavlaunch.fragments.AiAssistantFragment;
import net.kdt.pojavlaunch.fragments.DownloadCenterFragment;
import net.kdt.pojavlaunch.fragments.MainMenuFragment;
import net.kdt.pojavlaunch.fragments.MicrosoftLoginFragment;
import net.kdt.pojavlaunch.fragments.MusicFragment;
import net.kdt.pojavlaunch.fragments.MultiplayerFragment;
import net.kdt.pojavlaunch.fragments.SelectAuthFragment;
import net.kdt.pojavlaunch.lifecycle.ContextAwareDoneListener;
import net.kdt.pojavlaunch.lifecycle.ContextExecutor;
import net.kdt.pojavlaunch.modloaders.modpacks.ModloaderInstallTracker;
import net.kdt.pojavlaunch.modloaders.modpacks.imagecache.IconCacheJanitor;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;
import net.kdt.pojavlaunch.stardock.ui.SettingsFragment;
import net.kdt.pojavlaunch.update.UpdateManager;
import net.kdt.pojavlaunch.progresskeeper.ProgressKeeper;
import net.kdt.pojavlaunch.progresskeeper.TaskCountListener;
import net.kdt.pojavlaunch.services.ProgressServiceKeeper;
import net.kdt.pojavlaunch.tasks.AsyncMinecraftDownloader;
import net.kdt.pojavlaunch.tasks.AsyncVersionList;
import net.kdt.pojavlaunch.tasks.MinecraftDownloader;
import net.kdt.pojavlaunch.utils.DateUtils;
import net.kdt.pojavlaunch.utils.NotificationUtils;
import net.kdt.pojavlaunch.value.launcherprofiles.LauncherProfiles;
import net.kdt.pojavlaunch.value.launcherprofiles.MinecraftProfile;

import java.lang.ref.WeakReference;
import java.text.ParseException;

public class LauncherActivity extends BaseActivity {
    public static final String SETTING_FRAGMENT_TAG = "SETTINGS_FRAGMENT";
    public static final String NAV_LAUNCH = "nav_launch";
    public static final String NAV_DOWNLOAD = "nav_download";
    public static final String NAV_MULTIPLAYER = "nav_multiplayer";
    public static final String NAV_AI = "nav_ai";
    public static final String NAV_MUSIC = "nav_music";
    public static final String NAV_SETTINGS = "nav_settings";

    public final ActivityResultLauncher<Object> modInstallerLauncher =
            registerForActivityResult(new OpenDocumentWithExtension("jar"), (data)->{
                if(data != null) Tools.launchModInstaller(this, data);
            });

    private mcAccountSpinner mAccountSpinner;
    private FragmentContainerView mFragmentView;
    private ProgressLayout mProgressLayout;
    private ProgressServiceKeeper mProgressServiceKeeper;
    private ModloaderInstallTracker mInstallTracker;
    private NotificationManager mNotificationManager;

    private final FragmentManager.FragmentLifecycleCallbacks mFragmentCallbackListener = new FragmentManager.FragmentLifecycleCallbacks() {
        @Override
        public void onFragmentResumed(@NonNull FragmentManager fm, @NonNull Fragment f) {
            syncNavWithFragment(f);
        }
    };

    /* Listener for the back button in settings */
    private final ExtraListener<String> mBackPreferenceListener = (key, value) -> {
        if(value.equals("true")) onBackPressed();
        return false;
    };

    /* Listener for the auth method selection screen */
    private final ExtraListener<Boolean> mSelectAuthMethod = (key, value) -> {
        Fragment fragment = getSupportFragmentManager().findFragmentById(mFragmentView.getId());
        if(!(fragment instanceof MainMenuFragment)) return false;

        Tools.swapFragment(this, SelectAuthFragment.class, SelectAuthFragment.TAG, null);
        return false;
    };

    private final ExtraListener<Boolean> mLaunchGameListener = (key, value) -> {
        if(mProgressLayout.hasProcesses()){
            Toast.makeText(this, R.string.tasks_ongoing, Toast.LENGTH_LONG).show();
            return false;
        }

        String selectedProfile = LauncherPreferences.DEFAULT_PREF.getString(LauncherPreferences.PREF_KEY_CURRENT_PROFILE,"");
        if (LauncherProfiles.mainProfileJson == null || !LauncherProfiles.mainProfileJson.profiles.containsKey(selectedProfile)){
            Toast.makeText(this, R.string.error_no_version, Toast.LENGTH_LONG).show();
            return false;
        }
        MinecraftProfile prof = LauncherProfiles.mainProfileJson.profiles.get(selectedProfile);
        if (prof == null || prof.lastVersionId == null || "Unknown".equals(prof.lastVersionId)){
            Toast.makeText(this, R.string.error_no_version, Toast.LENGTH_LONG).show();
            return false;
        }

        if(mAccountSpinner.getSelectedAccount() == null){
            Toast.makeText(this, R.string.no_saved_accounts, Toast.LENGTH_LONG).show();
            ExtraCore.setValue(ExtraConstants.SELECT_AUTH_METHOD, true);
            return false;
        }
        String normalizedVersionId = AsyncMinecraftDownloader.normalizeVersionId(prof.lastVersionId);
        JMinecraftVersionList.Version mcVersion = AsyncMinecraftDownloader.getListedVersion(normalizedVersionId);

        if (mAccountSpinner.getSelectedAccount().isDemo()) {
            boolean isOlderThan13 = true;
            if (mcVersion != null) {
                try {
                    isOlderThan13 = DateUtils.dateBefore(DateUtils.parseReleaseDate(mcVersion.releaseTime), 2012, 6, 22);
                } catch (ParseException ignored) {}
            }
            if (isOlderThan13) {
                Toast.makeText(this, R.string.toast_not_available_demo, Toast.LENGTH_LONG).show();
                return false;
            }
        }

        new MinecraftDownloader().start(
                this,
                mcVersion,
                normalizedVersionId,
                new ContextAwareDoneListener(this, normalizedVersionId)
        );
        return false;
    };

    private final TaskCountListener mDoubleLaunchPreventionListener = taskCount -> {
        if(taskCount > 0) {
            Tools.runOnUiThread(() ->
                    mNotificationManager.cancel(NotificationUtils.NOTIFICATION_ID_GAME_START)
            );
        }
    };

    private ActivityResultLauncher<String> mRequestNotificationPermissionLauncher;
    private WeakReference<Runnable> mRequestNotificationPermissionRunnable;

    private UpdateManager mUpdateManager;

    public void triggerUpdateCheck() {
        if (mUpdateManager == null) mUpdateManager = new UpdateManager(this);
        mUpdateManager.checkForUpdates();
    }

    @Override
    protected boolean shouldIgnoreNotch() {
        return getResources().getConfiguration().orientation == ORIENTATION_PORTRAIT;
    }

    @Override
    public boolean setFullscreen() {
        return false;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pojav_launcher);

        FragmentManager fragmentManager = getSupportFragmentManager();
        if(fragmentManager.getBackStackEntryCount() < 1) {
            fragmentManager.beginTransaction()
                    .setReorderingAllowed(true)
                    .addToBackStack("ROOT")
                    .add(R.id.container_fragment, MainMenuFragment.class, null, NAV_LAUNCH).commit();
        }

        IconCacheJanitor.runJanitor();
        mRequestNotificationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isAllowed -> {
                    if(!isAllowed) handleNoNotificationPermission();
                    else {
                        Runnable runnable = Tools.getWeakReference(mRequestNotificationPermissionRunnable);
                        if(runnable != null) runnable.run();
                    }
                }
        );
        getWindow().setBackgroundDrawable(null);
        bindViews();
        setupBottomNavigation();
        checkNotificationPermission();
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        ProgressKeeper.addTaskCountListener(mDoubleLaunchPreventionListener);
        ProgressKeeper.addTaskCountListener((mProgressServiceKeeper = new ProgressServiceKeeper(this)));

        ProgressKeeper.addTaskCountListener(mProgressLayout);
        ExtraCore.addExtraListener(ExtraConstants.BACK_PREFERENCE, mBackPreferenceListener);
        ExtraCore.addExtraListener(ExtraConstants.SELECT_AUTH_METHOD, mSelectAuthMethod);
        ExtraCore.addExtraListener(ExtraConstants.LAUNCH_GAME, mLaunchGameListener);

        new AsyncVersionList().getVersionList(versions -> ExtraCore.setValue(ExtraConstants.RELEASE_TABLE, versions), false);

        mInstallTracker = new ModloaderInstallTracker(this);

        mProgressLayout.observe(ProgressLayout.DOWNLOAD_MINECRAFT);
        mProgressLayout.observe(ProgressLayout.UNPACK_RUNTIME);
        mProgressLayout.observe(ProgressLayout.INSTALL_MODPACK);
        mProgressLayout.observe(ProgressLayout.AUTHENTICATE_MICROSOFT);
        mProgressLayout.observe(ProgressLayout.DOWNLOAD_VERSION_LIST);

        selectNav(NAV_LAUNCH);
        AgreementDialog.show(this, () -> {}, this::finish);

        mUpdateManager = new UpdateManager(this);
        mUpdateManager.checkForUpdates();
    }

    private void bindViews() {
        mFragmentView = findViewById(R.id.container_fragment);
        mAccountSpinner = findViewById(R.id.account_spinner);
        mProgressLayout = findViewById(R.id.progress_layout);
    }

    private void setupBottomNavigation() {
        View navLaunch = findViewById(R.id.nav_launch);
        View navDownload = findViewById(R.id.nav_download);
        View navMultiplayer = findViewById(R.id.nav_multiplayer);
        View navAi = findViewById(R.id.nav_ai);
        View navMusic = findViewById(R.id.nav_music);
        View navSettings = findViewById(R.id.nav_settings);

        navLaunch.setOnClickListener(v -> selectNav(NAV_LAUNCH));
        navDownload.setOnClickListener(v -> selectNav(NAV_DOWNLOAD));
        navMultiplayer.setOnClickListener(v -> selectNav(NAV_MULTIPLAYER));
        navAi.setOnClickListener(v -> selectNav(NAV_AI));
        navMusic.setOnClickListener(v -> selectNav(NAV_MUSIC));
        navSettings.setOnClickListener(v -> selectNav(NAV_SETTINGS));
    }

    private void selectNav(String tag) {
        Class<? extends Fragment> target;
        switch (tag) {
            case NAV_DOWNLOAD:
                target = DownloadCenterFragment.class;
                break;
            case NAV_MULTIPLAYER:
                target = MultiplayerFragment.class;
                break;
            case NAV_AI:
                target = AiAssistantFragment.class;
                break;
            case NAV_MUSIC:
                target = MusicFragment.class;
                break;
            case NAV_SETTINGS:
                target = SettingsFragment.class;
                break;
            default:
                target = MainMenuFragment.class;
                tag = NAV_LAUNCH;
                break;
        }

        Fragment current = getSupportFragmentManager().findFragmentById(mFragmentView.getId());
        if (current != null && current.getClass().equals(target)) {
            highlightNavButton(tag);
            return;
        }

        try {
            getSupportFragmentManager().beginTransaction()
                    .setReorderingAllowed(true)
                    .replace(mFragmentView.getId(), target, null, tag)
                    .commit();
        } catch (Exception e) {
            Toast.makeText(this, "无法切换：" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        highlightNavButton(tag);
    }

    private void highlightNavButton(String tag) {
        int[] ids = {R.id.nav_launch, R.id.nav_download, R.id.nav_multiplayer, R.id.nav_ai, R.id.nav_music, R.id.nav_settings};
        String[] tags = {NAV_LAUNCH, NAV_DOWNLOAD, NAV_MULTIPLAYER, NAV_AI, NAV_MUSIC, NAV_SETTINGS};
        for (int i = 0; i < ids.length; i++) {
            View v = findViewById(ids[i]);
            if (v != null) v.setSelected(tags[i].equals(tag));
        }
    }

    private void syncNavWithFragment(Fragment f) {
        if (f == null) return;
        Class<? extends Fragment> cls = f.getClass();
        if (cls == MainMenuFragment.class) highlightNavButton(NAV_LAUNCH);
        else if (cls == DownloadCenterFragment.class) highlightNavButton(NAV_DOWNLOAD);
        else if (cls == MultiplayerFragment.class) highlightNavButton(NAV_MULTIPLAYER);
        else if (cls == AiAssistantFragment.class) highlightNavButton(NAV_AI);
        else if (cls == MusicFragment.class) highlightNavButton(NAV_MUSIC);
        else if (cls == SettingsFragment.class) highlightNavButton(NAV_SETTINGS);
    }

    @Override
    public void onResume() {
        super.onResume();
        ContextExecutor.setActivity(this);
        mInstallTracker.attach();
    }

    @Override
    public void onPause() {
        super.onPause();
        ContextExecutor.clearActivity();
        mInstallTracker.detach();
    }

    @Override
    public void onStart() {
        super.onStart();
        getSupportFragmentManager().registerFragmentLifecycleCallbacks(mFragmentCallbackListener, true);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mProgressLayout.cleanUpObservers();
        ProgressKeeper.removeTaskCountListener(mProgressLayout);
        ProgressKeeper.removeTaskCountListener(mProgressServiceKeeper);
        ExtraCore.removeExtraListenerFromValue(ExtraConstants.BACK_PREFERENCE, mBackPreferenceListener);
        ExtraCore.removeExtraListenerFromValue(ExtraConstants.SELECT_AUTH_METHOD, mSelectAuthMethod);
        ExtraCore.removeExtraListenerFromValue(ExtraConstants.LAUNCH_GAME, mLaunchGameListener);

        getSupportFragmentManager().unregisterFragmentLifecycleCallbacks(mFragmentCallbackListener);
    }

    @Override
    public void onBackPressed() {
        MicrosoftLoginFragment fragment = (MicrosoftLoginFragment) getVisibleFragmentByTag(MicrosoftLoginFragment.TAG);
        if(fragment != null){
            if(fragment.canGoBack()){
                fragment.goBack();
                return;
            }
        }

        if(getSupportFragmentManager().getBackStackEntryCount() > 1){
            super.onBackPressed();
            return;
        }

        Fragment current = getSupportFragmentManager().findFragmentById(mFragmentView.getId());
        if(current != null && !(current instanceof MainMenuFragment)){
            selectNav(NAV_LAUNCH);
            return;
        }

        finish();
    }

    @Override
    public void onAttachedToWindow() {
        LauncherPreferences.computeNotchSize(this);
    }

    private Fragment getVisibleFragmentByTag(String tag){
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(tag);
        if(fragment != null && fragment.isVisible()) {
            return fragment;
        }
        return null;
    }

    private void checkNotificationPermission() {
        if(LauncherPreferences.PREF_SKIP_NOTIFICATION_PERMISSION_CHECK ||
            checkForNotificationPermission()) {
            return;
        }

        if(ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.POST_NOTIFICATIONS)) {
            showNotificationPermissionReasoning();
            return;
        }
        askForNotificationPermission(null);
    }

    private void showNotificationPermissionReasoning() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.notification_permission_dialog_title)
                .setMessage(R.string.notification_permission_dialog_text)
                .setPositiveButton(android.R.string.ok, (d, w) -> askForNotificationPermission(null))
                .setNegativeButton(android.R.string.cancel, (d, w)-> handleNoNotificationPermission())
                .show();
    }

    private void handleNoNotificationPermission() {
        LauncherPreferences.PREF_SKIP_NOTIFICATION_PERMISSION_CHECK = true;
        LauncherPreferences.DEFAULT_PREF.edit()
                .putBoolean(LauncherPreferences.PREF_KEY_SKIP_NOTIFICATION_CHECK, true)
                .apply();
        Toast.makeText(this, R.string.notification_permission_toast, Toast.LENGTH_LONG).show();
    }

    public boolean checkForNotificationPermission() {
        return Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_DENIED;
    }

    public void askForNotificationPermission(Runnable onSuccessRunnable) {
        if(Build.VERSION.SDK_INT < 33) return;
        if(onSuccessRunnable != null) {
            mRequestNotificationPermissionRunnable = new WeakReference<>(onSuccessRunnable);
        }
        mRequestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
    }
}
