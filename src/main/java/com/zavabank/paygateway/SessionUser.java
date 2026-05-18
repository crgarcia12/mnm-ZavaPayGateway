package com.zavabank.paygateway;

public class SessionUser {
    private final int userId;
    private final String username;

    public SessionUser(int userId, String username) {
        this.userId = userId;
        this.username = username;
    }

    public int getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }
}
