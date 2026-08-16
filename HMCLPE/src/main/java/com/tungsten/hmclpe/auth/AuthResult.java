package com.tungsten.hmclpe.auth;

public class AuthResult {

    public final boolean success;
    public final AccountInfo account;
    public final String error;

    private AuthResult(boolean success, AccountInfo account, String error) {
        this.success = success;
        this.account = account;
        this.error = error;
    }

    public static AuthResult ok(AccountInfo account) {
        return new AuthResult(true, account, null);
    }

    public static AuthResult fail(String error) {
        return new AuthResult(false, null, error);
    }
}
