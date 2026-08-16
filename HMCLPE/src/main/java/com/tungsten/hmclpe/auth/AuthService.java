package com.tungsten.hmclpe.auth;

public class AuthService {

    public AuthResult loginOffline(String username) {
        if (username == null || username.isEmpty()) {
            return AuthResult.fail("用户名不能为空");
        }
        AccountInfo info = new AccountInfo(username);
        info.type = AccountInfo.TYPE_OFFLINE;
        info.lastLoginAt = System.currentTimeMillis();
        return AuthResult.ok(info);
    }

    public AuthResult loginMojang(String email, String password) {
        if (email == null || email.isEmpty()) {
            return AuthResult.fail("邮箱不能为空");
        }
        if (password == null || password.isEmpty()) {
            return AuthResult.fail("密码不能为空");
        }
        AccountInfo info = new AccountInfo(email);
        info.type = AccountInfo.TYPE_MOJANG;
        info.lastLoginAt = System.currentTimeMillis();
        return AuthResult.ok(info);
    }

    public AuthResult loginMicrosoft() {
        AccountInfo info = new AccountInfo("Microsoft User");
        info.type = AccountInfo.TYPE_MICROSOFT;
        info.userType = "msa";
        info.uuid = "00000000-0000-0000-0000-000000000000";
        info.accessToken = "stub-msa-token";
        info.lastLoginAt = System.currentTimeMillis();
        return AuthResult.ok(info);
    }
}
