package net.kdt.pojavlaunch;

import static android.content.res.Configuration.ORIENTATION_PORTRAIT;

import android.Manifest;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
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
import net.kdt.pojavlaunch.lifecycle.ContextAwareDoneListener;
import net.kdt.pojavlaunch.lifecycle.ContextExecutor;
import net.kdt.pojavlaunch.modloaders.modpacks.ModloaderInstallTracker;
import net.kdt.pojavlaunch.modloaders.modpacks.imagecache.IconCacheJanitor;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;
import net.kdt.pojavlaunch.progresskeeper.ProgressKeeper;
import net.kdt.pojavlaunch.progresskeeper.TaskCountListener;
import net.kdt.pojavlaunch.services.ProgressServiceKeeper;
import net.kdt.pojavlaunch.stardock.ui.DownloadHubFragment;
import net.kdt.pojavlaunch.stardock.ui.KeyBindingFragment;
import net.kdt.pojavlaunch.stardock.ui.MainFragmentV2;
import net.kdt.pojavlaunch.stardock.ui.OnlineHubFragment;
import net.kdt.pojavlaunch.stardock.ui.SettingsHubFragment;
import net.kdt.pojavlaunch.tasks.AsyncMinecraftDownloader;
import net.kdt.pojavlaunch.tasks.AsyncVersionList;
import net.kdt.pojavlaunch.tasks.MinecraftDownloader;
import net.kdt.pojavlaunch.update.UpdateManager;
import net.kdt.pojavlaunch.utils.DateUtils;
import net.kdt.pojavlaunch.utils.NotificationUtils;
import net.kdt.pojavlaunch.value.launcherprofiles.LauncherProfiles;
import net.kdt.pojavlaunch.value.launcherprofiles.MinecraftProfile;

import java.lang.ref.WeakReference;
import java.text.ParseException;

/**
 * v0.0.5 StarDockLauncher 主 Activity
 *
 * 保留 PojavLauncher 的引擎调用（启动 Minecraft、读取版本列表、订阅账户、显示进度）
 * UI 层全部由 stardock 包下的 fragment 提供。
 */
public class LauncherActivity extends BaseActivity {
    public static final String NAV_LAUNCH = "nav_launch";
    public static final String NAV_DOWNLOAD = "nav_download";
    public static final String NAV_MULTIPLAYER = "nav_multiplayer";
    public static final String NAV_KEYBINDING = "nav_keybinding";
    public static final String NAV_SETTINGS = "nav_settings";

    public final ActivityResultLauncher<Object> modInstallerLauncher =
            registerForActivityResult(new OpenDocumentWithExtension("jar"), (data) -> {
                if (data != null) Tools.launchModInstaller(this, data);
            });

    private mcAccountSpinner mAccountSpinner;
    private FragmentContainerView mFragmentView;
    private ProgressLayout mProgressLayout;
    private ProgressServiceKeeper mProgressServiceKeeper;
    private ModloaderInstallTracker mInstallTracker;
    private NotificationManager mNotificationManager;

    private ActivityResultLauncher<String> mRequestNotificationPermissionLauncher;
    private WeakReference<Runnable> mRequestNotificationPermissionRunnable;
    private UpdateManager mUpdateManager;

    private final TaskCountListener mDoubleLaunchPreventionListener = taskCount -> {
        if (taskCount > 0) {
            Tools.runOnUiThread(() ->
                    mNotificationManager.cancel(NotificationUtils.NOTIFICATION_ID_GAME_START)
            );
        }
    };

    private final ExtraListener<Boolean> mLaunchGameListener = (key, value) -> {
        launchGame();
        return false;
    };

    private final FragmentManager.FragmentLifecycleCallbacks mFragmentCallbackListener = new FragmentManager.FragmentLifecycleCallbacks() {
        @Override
        public void onFragmentResumed(@NonNull FragmentManager fm, @NonNull Fragment f) {
            syncNavWithFragment(f);
            if (f instanceof MainFragmentV2) {
                ((MainFragmentV2) f).refreshVersions();
                ((MainFragmentV2) f).refreshAccount();
            }
        }
    };

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
        if (fragmentManager.getBackStackEntryCount() < 1) {
            fragmentManager.beginTransaction()
                    .setReorderingAllowed(true)
                    .addToBackStack("ROOT")
                    .add(R.id.container_fragment, MainFragmentV2.class, null, NAV_LAUNCH).commit();
        }

        IconCacheJanitor.runJanitor();
        mRequestNotificationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isAllowed -> {
                    if (!isAllowed) handleNoNotificationPermission();
                    else {
                        Runnable runnable = Tools.getWeakReference(mRequestNotificationPermissionRunnable);
                        if (runnable != null) runnable.run();
                    }
                }
        );
        getWindow().setBackgroundDrawable(null);
        bindViews();
        setupLeftNavigation();
        checkNotificationPermission();
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        ProgressKeeper.addTaskCountListener(mDoubleLaunchPreventionListener);
        ProgressKeeper.addTaskCountListener((mProgressServiceKeeper = new ProgressServiceKeeper(this)));
        ProgressKeeper.addTaskCountListener(mProgressLayout);
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
        if (mAccountSpinner == null) {
            View root = findViewById(android.R.id.content);
            mAccountSpinner = root.findViewById(R.id.account_spinner);
        }
    }

    private void setupLeftNavigation() {
        View navLaunch = findViewById(R.id.nav_launch);
        View navDownload = findViewById(R.id.nav_download);
        View navMultiplayer = findViewById(R.id.nav_multiplayer);
        View navKeybinding = findViewById(R.id.nav_keybinding);
        View navSettings = findViewById(R.id.nav_settings);

        navLaunch.setOnClickListener(v -> selectNav(NAV_LAUNCH));
        navDownload.setOnClickListener(v -> selectNav(NAV_DOWNLOAD));
        navMultiplayer.setOnClickListener(v -> selectNav(NAV_MULTIPLAYER));
        navKeybinding.setOnClickListener(v -> selectNav(NAV_KEYBINDING));
        navSettings.setOnClickListener(v -> selectNav(NAV_SETTINGS));
    }

    private void selectNav(String tag) {
        Class<? extends Fragment> target;
        switch (tag) {
            case NAV_DOWNLOAD:
                target = DownloadHubFragment.class;
                break;
            case NAV_MULTIPLAYER:
                target = OnlineHubFragment.class;
                break;
            case NAV_KEYBINDING:
                target = KeyBindingFragment.class;
                break;
            case NAV_SETTINGS:
                target = SettingsHubFragment.class;
                break;
            default:
                target = MainFragmentV2.class;
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
                    .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left)
                    .replace(mFragmentView.getId(), target, null, tag)
                    .commit();
        } catch (Exception e) {
            Toast.makeText(this, "无法切换：" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        highlightNavButton(tag);
    }

    private void highlightNavButton(String tag) {
        int[] ids = {R.id.nav_launch, R.id.nav_download, R.id.nav_multiplayer, R.id.nav_keybinding, R.id.nav_settings};
        String[] tags = {NAV_LAUNCH, NAV_DOWNLOAD, NAV_MULTIPLAYER, NAV_KEYBINDING, NAV_SETTINGS};
        for (int i = 0; i < ids.length; i++) {
            View v = findViewById(ids[i]);
            if (v != null) v.setSelected(tags[i].equals(tag));
        }
    }

    private void syncNavWithFragment(Fragment f) {
        if (f == null) return;
        Class<? extends Fragment> cls = f.getClass();
        if (cls == MainFragmentV2.class) highlightNavButton(NAV_LAUNCH);
        else if (cls == DownloadHubFragment.class) highlightNavButton(NAV_DOWNLOAD);
        else if (cls == OnlineHubFragment.class) highlightNavButton(NAV_MULTIPLAYER);
        else if (cls == KeyBindingFragment.class) highlightNavButton(NAV_KEYBINDING);
        else if (cls == SettingsHubFragment.class) highlightNavButton(NAV_SETTINGS);
    }

    @Override
    public void onResume() {
        super.onResume();
        ContextExecutor.setActivity(this);
        mInstallTracker.attach();
        Fragment current = getSupportFragmentManager().findFragmentById(mFragmentView.getId());
        if (current instanceof MainFragmentV2) {
            ((MainFragmentV2) current).refreshVersions();
            ((MainFragmentV2) current).refreshAccount();
        }
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
        if (mProgressLayout != null) mProgressLayout.cleanUpObservers();
        ProgressKeeper.removeTaskCountListener(mProgressLayout);
        ProgressKeeper.removeTaskCountListener(mProgressServiceKeeper);
        ExtraCore.removeExtraListenerFromValue(ExtraConstants.LAUNCH_GAME, mLaunchGameListener);
        getSupportFragmentManager().unregisterFragmentLifecycleCallbacks(mFragmentCallbackListener);
    }

    @Override
    public void onBackPressed() {
        Fragment current = getSupportFragmentManager().findFragmentById(mFragmentView.getId());
        if (current != null && !(current instanceof MainFragmentV2)) {
            selectNav(NAV_LAUNCH);
            return;
        }
        finish();
    }

    @Override
    public void onAttachedToWindow() {
        LauncherPreferences.computeNotchSize(this);
    }

    /* ----------- v0.0.5 公开方法：MainFragmentV2 调用 ----------- */

    /** 启动游戏（核心引擎调用） */
    public void launchGame() {
        if (mProgressLayout.hasProcesses()) {
            Toast.makeText(this, R.string.tasks_ongoing, Toast.LENGTH_LONG).show();
            return;
        }

        String selectedProfile = LauncherPreferences.DEFAULT_PREF.getString(LauncherPreferences.PREF_KEY_CURRENT_PROFILE, "");
        if (LauncherProfiles.mainProfileJson == null || !LauncherProfiles.mainProfileJson.profiles.containsKey(selectedProfile)) {
            Toast.makeText(this, R.string.error_no_version, Toast.LENGTH_LONG).show();
            return;
        }
        MinecraftProfile prof = LauncherProfiles.mainProfileJson.profiles.get(selectedProfile);
        if (prof == null || prof.lastVersionId == null || "Unknown".equals(prof.lastVersionId)) {
            Toast.makeText(this, R.string.error_no_version, Toast.LENGTH_LONG).show();
            return;
        }

        if (mAccountSpinner == null || mAccountSpinner.getSelectedAccount() == null) {
            Toast.makeText(this, R.string.no_saved_accounts, Toast.LENGTH_LONG).show();
            openAccountPicker();
            return;
        }

        String normalizedVersionId = AsyncMinecraftDownloader.normalizeVersionId(prof.lastVersionId);
        JMinecraftVersionList.Version mcVersion = AsyncMinecraftDownloader.getListedVersion(normalizedVersionId);

        if (mAccountSpinner.getSelectedAccount().isDemo()) {
            boolean isOlderThan13 = true;
            if (mcVersion != null) {
                try {
                    isOlderThan13 = DateUtils.dateBefore(DateUtils.parseReleaseDate(mcVersion.releaseTime), 2012, 6, 22);
                } catch (ParseException ignored) {
                }
            }
            if (isOlderThan13) {
                Toast.makeText(this, R.string.toast_not_available_demo, Toast.LENGTH_LONG).show();
                return;
            }
        }

        new MinecraftDownloader().start(
                this,
                mcVersion,
                normalizedVersionId,
                new ContextAwareDoneListener(this, normalizedVersionId)
        );
    }

    /** 打开版本管理（跳到下载中心） */
    public void openManageVersions() {
        selectNav(NAV_DOWNLOAD);
    }

    /** 打开账户选择（弹账户下拉） */
    public void openAccountPicker() {
        if (mAccountSpinner != null) {
            try {
                mAccountSpinner.performClick();
            } catch (Exception ignored) {
                Toast.makeText(this, "请前往设置登录账户", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "账户系统未初始化", Toast.LENGTH_SHORT).show();
        }
    }

    /** 打开版本编辑器 */
    public void openVersionEditor(int position) {
        Toast.makeText(this, "打开版本编辑器 #" + position, Toast.LENGTH_SHORT).show();
    }

    /** 触发更新检查 */
    public void triggerUpdateCheck() {
        if (mUpdateManager == null) mUpdateManager = new UpdateManager(this);
        mUpdateManager.checkForUpdates();
    }

    public mcAccountSpinner getAccountSpinner() {
        return mAccountSpinner;
    }

    /* ----------- 权限 ----------- */

    private void checkNotificationPermission() {
        if (LauncherPreferences.PREF_SKIP_NOTIFICATION_PERMISSION_CHECK ||
                checkForNotificationPermission()) {
            return;
        }
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this, Manifest.permission.POST_NOTIFICATIONS)) {
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
                .setNegativeButton(android.R.string.cancel, (d, w) -> handleNoNotificationPermission())
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
        if (Build.VERSION.SDK_INT < 33) return;
        if (onSuccessRunnable != null) {
            mRequestNotificationPermissionRunnable = new WeakReference<>(onSuccessRunnable);
        }
        mRequestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
    }
}
