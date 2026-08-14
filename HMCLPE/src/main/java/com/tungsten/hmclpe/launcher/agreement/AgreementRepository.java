package com.tungsten.hmclpe.launcher.agreement;

import android.content.Context;
import android.content.SharedPreferences;

import com.tungsten.hmclpe.BuildConfig;

/**
 * Stores the last accepted agreement version and JAVA runtime version.
 *
 * Stored values:
 *   - key "agreement_accepted_version" : int  (the agreementVersion the user last accepted)
 *   - key "eula_accepted"               : bool (whether EULA was ever accepted)
 *   - key "gpl_accepted"                : bool (whether GPL was ever accepted)
 *   - key "java_version_accepted"       : int  (the javaVersion the user last accepted)
 *   - key "agreement_decision_time"     : long (epoch ms of last accept)
 */
public final class AgreementRepository {

    private static final String PREFS = "stardock_agreement";

    private final SharedPreferences sp;

    public AgreementRepository(Context ctx) {
        this.sp = ctx.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public int currentAgreementVersion() {
        return sp.getInt("agreement_accepted_version", 0);
    }

    public int currentJavaVersion() {
        return sp.getInt("java_version_accepted", 0);
    }

    public boolean isEulaAccepted() {
        return sp.getBoolean("eula_accepted", false);
    }

    public boolean isGplAccepted() {
        return sp.getBoolean("gpl_accepted", false);
    }

    public long lastDecisionTime() {
        return sp.getLong("agreement_decision_time", 0L);
    }

    public boolean needsFirstRunAgreement() {
        return !isEulaAccepted() || !isGplAccepted();
    }

    public boolean needsUpdateAgreement(int targetAgreementVersion) {
        return isEulaAccepted() && isGplAccepted() && currentAgreementVersion() < targetAgreementVersion;
    }

    public boolean needsJavaUpdateAgreement(int targetJavaVersion) {
        return isEulaAccepted() && currentJavaVersion() < targetJavaVersion;
    }

    public void accept(int agreementVersion, int javaVersion, boolean eula, boolean gpl) {
        sp.edit()
                .putInt("agreement_accepted_version", agreementVersion)
                .putInt("java_version_accepted", javaVersion)
                .putBoolean("eula_accepted", eula)
                .putBoolean("gpl_accepted", gpl)
                .putLong("agreement_decision_time", System.currentTimeMillis())
                .putString("app_version_at_accept", BuildConfig.VERSION_NAME)
                .apply();
    }

    public void reset() {
        sp.edit().clear().apply();
    }
}
