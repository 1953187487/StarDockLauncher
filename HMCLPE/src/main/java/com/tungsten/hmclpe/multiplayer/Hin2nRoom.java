package com.tungsten.hmclpe.multiplayer;

import androidx.annotation.NonNull;

import java.util.UUID;

public final class Hin2nRoom {

    public enum Role {
        HOST, CLIENT
    }

    public enum State {
        IDLE, CREATING, WAITING_PEER, CONNECTED, ERROR
    }

    private final String roomCode;
    private final Role role;
    private final String virtualIp;
    private final int gamePort;
    private final long createdAt;

    private volatile State state;
    private volatile String peerVirtualIp;
    private volatile String peerEndpoint;
    private volatile String lastError;

    public Hin2nRoom(Role role, String virtualIp, int gamePort) {
        this.roomCode = "ROOM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        this.role = role;
        this.virtualIp = virtualIp;
        this.gamePort = gamePort;
        this.createdAt = System.currentTimeMillis();
        this.state = State.IDLE;
    }

    @NonNull
    public String getRoomCode() {
        return roomCode;
    }

    public Role getRole() {
        return role;
    }

    public String getVirtualIp() {
        return virtualIp;
    }

    public int getGamePort() {
        return gamePort;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public String getPeerVirtualIp() {
        return peerVirtualIp;
    }

    public void setPeerVirtualIp(String peerVirtualIp) {
        this.peerVirtualIp = peerVirtualIp;
    }

    public String getPeerEndpoint() {
        return peerEndpoint;
    }

    public void setPeerEndpoint(String peerEndpoint) {
        this.peerEndpoint = peerEndpoint;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public boolean isActive() {
        return state == State.CONNECTED || state == State.WAITING_PEER;
    }
}
