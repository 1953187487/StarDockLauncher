package com.tungsten.hmclpe.auth;

import java.io.Serializable;

public class AccountInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final int TYPE_OFFLINE = 0;
    public static final int TYPE_MOJANG = 1;
    public static final int TYPE_MICROSOFT = 2;
    public static final int TYPE_AUTHLIB_INJECTOR = 3;

    public String username;
    public String uuid;
    public String accessToken;
    public String userType = "msa";
    public String skinUrl;
    public String capeUrl;
    public int type = TYPE_OFFLINE;
    public long lastLoginAt;

    public AccountInfo() {
    }

    public AccountInfo(String username) {
        this.username = username;
        this.type = TYPE_OFFLINE;
        if (username != null) {
            this.uuid = com.tungsten.hmclpe.utils.DigestUtils.md5("OfflinePlayer:" + username);
        }
        this.userType = "legacy";
    }

    public boolean isOffline() {
        return type == TYPE_OFFLINE;
    }

    public boolean hasSkin() {
        return skinUrl != null && !skinUrl.isEmpty();
    }
}
