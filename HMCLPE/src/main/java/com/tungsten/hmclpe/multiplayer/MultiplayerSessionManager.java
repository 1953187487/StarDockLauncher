package com.tungsten.hmclpe.multiplayer;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class MultiplayerSessionManager {

    private static final String TAG = "MultiplayerSess";

    public interface Listener {
        void onStateChanged(@NonNull Hin2nRoom room);

        void onPeerConnected(@NonNull Hin2nRoom room);

        void onError(@NonNull Hin2nRoom room, @NonNull String message);
    }

    private static volatile MultiplayerSessionManager INSTANCE;

    public static MultiplayerSessionManager get() {
        MultiplayerSessionManager local = INSTANCE;
        if (local == null) {
            synchronized (MultiplayerSessionManager.class) {
                local = INSTANCE;
                if (local == null) {
                    local = new MultiplayerSessionManager();
                    INSTANCE = local;
                }
            }
        }
        return local;
    }

    private Hin2nRoom currentRoom;
    private Listener listener;
    private DatagramSocket punchSocket;
    private Socket relaySocket;
    private PrintWriter relayWriter;
    private BufferedReader relayReader;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private ScheduledFuture<?> heartbeatTask;
    private InetAddress peerInet;
    private int peerPort;

    private MultiplayerSessionManager() {
    }

    @Nullable
    public Hin2nRoom getCurrentRoom() {
        return currentRoom;
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public synchronized void createRoom(@NonNull Context ctx, int gamePort, Listener oneShotListener) {
        if (running.get()) {
            stop();
        }
        String virtualNet = Hin2nConfig.getVirtualNet(ctx);
        Hin2nRoom room = new Hin2nRoom(Hin2nRoom.Role.HOST, virtualNet + ".1", gamePort);
        room.setState(Hin2nRoom.State.CREATING);
        currentRoom = room;
        this.listener = oneShotListener;
        notifyState(room);
        startPunchSocket();
        startRelayClient(ctx, room, null);
        startHeartbeat();
        room.setState(Hin2nRoom.State.WAITING_PEER);
        notifyState(room);
    }

    public synchronized void joinRoom(@NonNull Context ctx, String roomCode, int gamePort, Listener oneShotListener) {
        if (running.get()) {
            stop();
        }
        String virtualNet = Hin2nConfig.getVirtualNet(ctx);
        Hin2nRoom room = new Hin2nRoom(Hin2nRoom.Role.CLIENT, virtualNet + ".2", gamePort);
        room.setState(Hin2nRoom.State.CREATING);
        currentRoom = room;
        this.listener = oneShotListener;
        notifyState(room);
        startPunchSocket();
        startRelayClient(ctx, room, roomCode);
        startHeartbeat();
        room.setState(Hin2nRoom.State.WAITING_PEER);
        notifyState(room);
    }

    private void startPunchSocket() {
        try {
            punchSocket = new DatagramSocket(Hin2nConfig.PUNCH_PORT);
            punchSocket.setBroadcast(true);
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    byte[] buf = new byte[2048];
                    while (running.get() && punchSocket != null && !punchSocket.isClosed()) {
                        try {
                            DatagramPacket pkt = new DatagramPacket(buf, buf.length);
                            punchSocket.receive(pkt);
                            String msg = new String(pkt.getData(), 0, pkt.getLength(), StandardCharsets.UTF_8).trim();
                            handlePunchPacket(msg, pkt.getAddress(), pkt.getPort());
                        } catch (IOException e) {
                            if (running.get()) {
                                Log.w(TAG, "punch recv error", e);
                            }
                        }
                    }
                }
            }, "stardock-punch-recv");
            t.setDaemon(true);
            t.start();
        } catch (IOException e) {
            Log.e(TAG, "Failed to bind punch socket on " + Hin2nConfig.PUNCH_PORT, e);
            if (currentRoom != null) {
                currentRoom.setLastError("无法绑定 UDP : " + Hin2nConfig.PUNCH_PORT);
                currentRoom.setState(Hin2nRoom.State.ERROR);
                notifyError(currentRoom, "无法绑定 UDP 端口");
            }
        }
    }

    private void handlePunchPacket(String msg, InetAddress from, int port) {
        if (currentRoom == null) {
            return;
        }
        if (msg.startsWith("HELLO ")) {
            peerInet = from;
            this.peerPort = port;
            currentRoom.setPeerEndpoint(from.getHostAddress() + ":" + port);
            sendPunch("READY " + currentRoom.getRoomCode());
            markConnected();
        } else if (msg.startsWith("READY ") && currentRoom.getRole() == Hin2nRoom.Role.HOST) {
            String code = msg.substring("READY ".length()).trim();
            if (code.equalsIgnoreCase(currentRoom.getRoomCode())) {
                peerInet = from;
                this.peerPort = port;
                currentRoom.setPeerEndpoint(from.getHostAddress() + ":" + port);
                markConnected();
            }
        }
    }

    private void markConnected() {
        if (currentRoom == null) {
            return;
        }
        currentRoom.setState(Hin2nRoom.State.CONNECTED);
        String base = currentRoom.getVirtualIp().substring(0, currentRoom.getVirtualIp().lastIndexOf('.'));
        currentRoom.setPeerVirtualIp(currentRoom.getRole() == Hin2nRoom.Role.HOST
                ? base + ".2"
                : base + ".1");
        notifyPeerConnected(currentRoom);
    }

    public void sendPunch(String msg) {
        if (punchSocket == null || punchSocket.isClosed()) {
            return;
        }
        try {
            byte[] data = msg.getBytes(StandardCharsets.UTF_8);
            if (peerInet != null) {
                DatagramPacket pkt = new DatagramPacket(data, data.length, peerInet, peerPort);
                punchSocket.send(pkt);
            }
            DatagramPacket bcast = new DatagramPacket(data, data.length,
                    InetAddress.getByName("255.255.255.255"), Hin2nConfig.PUNCH_PORT);
            punchSocket.send(bcast);
        } catch (IOException e) {
            Log.w(TAG, "punch send error", e);
        }
    }

    private void startRelayClient(Context ctx, Hin2nRoom room, @Nullable String joinCode) {
        String host = Hin2nConfig.getRelayHost(ctx);
        int port = Hin2nConfig.getRelayPort(ctx);
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    relaySocket = new Socket();
                    relaySocket.connect(new InetSocketAddress(host, port), 5000);
                    relayWriter = new PrintWriter(relaySocket.getOutputStream(), true);
                    relayReader = new BufferedReader(new InputStreamReader(relaySocket.getInputStream(), StandardCharsets.UTF_8));
                    String announce = (room.getRole() == Hin2nRoom.Role.HOST ? "HOST " : "JOIN ")
                            + room.getRoomCode() + " "
                            + (joinCode == null ? "" : joinCode) + "\n";
                    relayWriter.print(announce);
                    relayWriter.flush();
                    String line;
                    while (running.get() && (line = relayReader.readLine()) != null) {
                        Log.d(TAG, "relay: " + line);
                        if (line.startsWith("PEER ")) {
                            String[] parts = line.substring(5).split(":");
                            if (parts.length == 2) {
                                try {
                                    peerInet = InetAddress.getByName(parts[0]);
                                    peerPort = Integer.parseInt(parts[1]);
                                    currentRoom.setPeerEndpoint(parts[0] + ":" + parts[1]);
                                    sendPunch("HELLO " + currentRoom.getRoomCode());
                                    if (currentRoom.getRole() == Hin2nRoom.Role.CLIENT) {
                                        markConnected();
                                    }
                                } catch (Exception e) {
                                    Log.w(TAG, "bad PEER line: " + line, e);
                                }
                            }
                        }
                    }
                } catch (IOException e) {
                    Log.i(TAG, "relay unreachable, will rely on LAN-only mode: " + e.getMessage());
                }
            }
        }, "stardock-relay");
        t.setDaemon(true);
        t.start();
    }

    private void startHeartbeat() {
        if (heartbeatTask != null) {
            heartbeatTask.cancel(false);
        }
        heartbeatTask = scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                if (!running.get() || currentRoom == null) {
                    return;
                }
                sendPunch("PING " + currentRoom.getRoomCode());
                if (relayWriter != null) {
                    relayWriter.print("PING\n");
                    relayWriter.flush();
                }
            }
        }, 0, Hin2nConfig.HEARTBEAT_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    public synchronized void stop() {
        running.set(false);
        if (heartbeatTask != null) {
            heartbeatTask.cancel(false);
            heartbeatTask = null;
        }
        closeQuietly(punchSocket);
        punchSocket = null;
        closeQuietly(relaySocket);
        relaySocket = null;
        relayWriter = null;
        relayReader = null;
        peerInet = null;
        if (currentRoom != null) {
            currentRoom.setState(Hin2nRoom.State.IDLE);
        }
    }

    public void bindForForeground() {
        running.set(true);
    }

    private void closeQuietly(@Nullable java.net.Socket s) {
        if (s != null) {
            try {
                s.close();
            } catch (IOException ignored) {
            }
        }
    }

    private void closeQuietly(@Nullable DatagramSocket s) {
        if (s != null) {
            try {
                s.close();
            } catch (Exception ignored) {
            }
        }
    }

    private void notifyState(Hin2nRoom room) {
        if (listener != null) {
            listener.onStateChanged(room);
        }
    }

    private void notifyPeerConnected(Hin2nRoom room) {
        if (listener != null) {
            listener.onPeerConnected(room);
        }
    }

    private void notifyError(Hin2nRoom room, String msg) {
        if (listener != null) {
            listener.onError(room, msg);
        }
    }

    @SuppressWarnings("unused")
    private OutputStream noop() {
        return null;
    }
}
