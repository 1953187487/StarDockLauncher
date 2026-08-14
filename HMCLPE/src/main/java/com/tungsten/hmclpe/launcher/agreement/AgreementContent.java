package com.tungsten.hmclpe.launcher.agreement;

import androidx.annotation.NonNull;

import com.tungsten.hmclpe.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Bundled agreement content for the launcher. Provides the user-facing text
 * for first-run EULA + GPL, and per-update changelog + JAVA update notice.
 */
public final class AgreementContent {

    public static final int AGREEMENT_VERSION = 102;
    public static final int JAVA_RUNTIME_VERSION = 102;

    public static final class Section {
        public final int titleResId;
        public final CharSequence body;

        public Section(int titleResId, CharSequence body) {
            this.titleResId = titleResId;
            this.body = body;
        }
    }

    public static final class Snapshot {
        public final boolean isFirstRun;
        public final List<Section> sections;
        public final List<JavaUpdateManager.RuntimeInfo> javaUpdates;

        Snapshot(boolean isFirstRun, List<Section> sections, List<JavaUpdateManager.RuntimeInfo> javaUpdates) {
            this.isFirstRun = isFirstRun;
            this.sections = sections;
            this.javaUpdates = javaUpdates;
        }
    }

    private AgreementContent() {
    }

    @NonNull
    public static Snapshot forFirstRun(@NonNull String eulaText, @NonNull String gplSummaryText) {
        List<Section> sections = new ArrayList<>();
        sections.add(new Section(R.string.agreement_section_eula_title, eulaText));
        sections.add(new Section(R.string.agreement_section_gpl_title, gplSummaryText));
        sections.add(new Section(R.string.agreement_section_privacy_title,
                "我们仅在设备本地保存你的账号、配置、控制方案与游戏文件，不会主动上传到任何服务器。\n" +
                        "联机功能需要使用网络权限与 VPN 权限，用以建立房间和转发游戏流量。"));
        return new Snapshot(true, sections, new ArrayList<JavaUpdateManager.RuntimeInfo>());
    }

    @NonNull
    public static Snapshot forUpdate(@NonNull String changelogText,
                                     @NonNull List<JavaUpdateManager.RuntimeInfo> javaUpdates) {
        List<Section> sections = new ArrayList<>();
        sections.add(new Section(R.string.agreement_section_changelog_title, changelogText));
        if (!javaUpdates.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (JavaUpdateManager.RuntimeInfo ri : javaUpdates) {
                sb.append("• ").append(ri.displayLabel())
                        .append(" : ").append(ri.installedVersion)
                        .append(" → ").append(ri.bundledVersion).append('\n');
            }
            sections.add(new Section(R.string.agreement_section_java_update_title, sb.toString()));
        }
        return new Snapshot(false, sections, javaUpdates);
    }
}
