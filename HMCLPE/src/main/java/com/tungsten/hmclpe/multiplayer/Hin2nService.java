package com.tungsten.hmclpe.multiplayer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.net.VpnService;
import android.os.Build;
import android.os.ParcelFileDescriptor;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

public class Hin2nService extends VpnService {

    private static final String TAG = "Hin2nService";

    private static final String CHANNEL_ID = "stardock_hin2n";
    private static final int NOTIFICATION_ID = 0x1010;
    private static final String ACTION_START = "com.tungsten.hmclpe.multiplayer.START";
    private static final String ACTION_STOP = "com.tungsten.hmclpe.multiplayer.STOP";

    public static final String EXTRA_ROLE = "role";
    public static final String EXTRA_GAME_PORT = "game_port";
    public static final String EXTRA_VIRTUAL_IP = "virtual_ip";

    private ParcelFileDescriptor tunFd;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread tunReadThread;
    private Thread tunWriteThread;
    private DatagramSocket upstreamSocket;
    private InetAddress peerAddr;
    private int peerPort;

    public static Intent prepareStart(@NonNull android.content.Context ctx, @NonNull String role, int gamePort, @NonNull String virtualIp) {
        Intent intent = new Intent(ctx, Hin2nService.class);
        intent.setAction(ACTION_START);
        intent.putExtra(EXTRA_ROLE, role);
        intent.putExtra(EXTRA_GAME_PORT, gamePort);
        intent.putExtra(EXTRA_VIRTUAL_IP, virtualIp);
        return intent;
    }

    public static Intent prepareStop(@NonNull android.content.Context ctx) {
        Intent intent = new Intent(ctx, Hin2nService.class);
        intent.setAction(ACTION_STOP);
        return intent;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            stopSelf();
            return START_NOT_STICKY;
        }
        String action = intent.getAction();
        if (ACTION_STOP.equals(action)) {
            shutdown();
            stopForeground(true);
            stopSelf();
            return START_NOT_STICKY;
        }
        if (!ACTION_START.equals(action)) {
            stopSelf();
            return START_NOT_STICKY;
        }
        String role = intent.getStringExtra(EXTRA_ROLE);
        int gamePort = intent.getIntExtra(EXTRA_GAME_PORT, Hin2nConfig.DEFAULT_GAME_PORT);
        String virtualIp = intent.getStringExtra(EXTRA_VIRTUAL_IP);
        if (virtualIp == null || virtualIp.isEmpty()) {
            virtualIp = Hin2nConfig.getVirtualNet(this) + ".1";
        }
        startForeground(NOTIFICATION_ID, buildNotification(role, virtualIp));
        establishTunnel(role, virtualIp, gamePort);
        return START_STICKY;
    }

    @Override
    public void onRevoke() {
        shutdown();
        super.onRevoke();
    }

    @Override
    public void onDestroy() {
        shutdown();
        super.onDestroy();
    }

    private void establishTunnel(String role, String virtualIp, int gamePort) {
        if (running.get()) {
            return;
        }
        Hin2nRoom room = MultiplayerSessionManager.get().getCurrentRoom();
        if (room == null || !room.isActive()) {
            stopSelf();
            return;
        }
        try {
            upstreamSocket = new DatagramSocket(0);
            Builder b = new Builder()
                    .setSession("StarDockLauncher-" + (role == null ? "host" : role))
                    .addAddress(virtualIp, 24)
                    .addRoute("10.10.0.0", 24)
                    .addDnsServer("1.1.1.1")
                    .setMtu(1400);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                b = b.setBlocking(true);
            }
            tunFd = b.establish();
            if (tunFd == null) {
                stopSelf();
                return;
            }
            running.set(true);
            if (room.getPeerEndpoint() != null && room.getPeerEndpoint().contains(":")) {
                String[] parts = room.getPeerEndpoint().split(":");
                peerAddr = InetAddress.getByName(parts[0]);
                peerPort = Integer.parseInt(parts[1]);
            }
            tunReadThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    readTunLoop();
                }
            }, "stardock-tun-read");
            tunReadThread.start();
            tunWriteThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    writeTunLoop();
                }
            }, "stardock-tun-write");
            tunWriteThread.start();
        } catch (Exception e) {
            android.util.Log.e(TAG, "Failed to establish tunnel", e);
            shutdown();
            stopSelf();
        }
    }

    private void readTunLoop() {
        FileInputStream in = new FileInputStream(tunFd.getFileDescriptor());
        byte[] buf = new byte[32767];
        ByteBuffer packet = ByteBuffer.allocate(buf.length);
        while (running.get()) {
            try {
                int n = in.read(buf);
                if (n <= 0) {
                    continue;
                }
                packet.clear();
                packet.put(buf, 0, n);
                packet.flip();
                if (peerAddr != null && upstreamSocket != null && !upstreamSocket.isClosed()) {
                    byte[] payload = new byte[n];
                    System.arraycopy(buf, 0, payload, 0, n);
                    DatagramPacket pkt = new DatagramPacket(payload, payload.length,
                            peerAddr, peerPort);
                    upstreamSocket.send(pkt);
                }
            } catch (IOException e) {
                if (running.get()) {
                    android.util.Log.w(TAG, "tun read error", e);
                }
                break;
            }
        }
    }

    private void writeTunLoop() {
        if (upstreamSocket == null) {
            return;
        }
        byte[] buf = new byte[32767];
        DatagramPacket pkt = new DatagramPacket(buf, buf.length);
        while (running.get()) {
            try {
                upstreamSocket.receive(pkt);
                if (tunFd == null) {
                    break;
                }
                FileOutputStream out = new FileOutputStream(tunFd.getFileDescriptor());
                out.write(pkt.getData(), 0, pkt.getLength());
                out.flush();
            } catch (IOException e) {
                if (running.get()) {
                    android.util.Log.w(TAG, "upstream recv error", e);
                }
                break;
            }
        }
    }

    private synchronized void shutdown() {
        if (!running.compareAndSet(true, false)) {
            return;
        }
        if (tunFd != null) {
            try {
                tunFd.close();
            } catch (IOException ignored) {
            }
            tunFd = null;
        }
        if (upstreamSocket != null && !upstreamSocket.isClosed()) {
            upstreamSocket.close();
        }
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) {
                NotificationChannel ch = new NotificationChannel(CHANNEL_ID,
                        getString(com.tungsten.hmclpe.R.string.multiplayer_floating_channel),
                        NotificationManager.IMPORTANCE_LOW);
                nm.createNotificationChannel(ch);
            }
        }
    }

    private Notification buildNotification(String role, String virtualIp) {
        Intent openIntent = getPackageManager().getLaunchIntentForPackage(getPackageName());
        PendingIntent pi = openIntent == null ? null
                : PendingIntent.getActivity(this, 0, openIntent,
                        PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        String title = getString(com.tungsten.hmclpe.R.string.multiplayer_floating_service_started);
        String body = (role == null ? "host" : role) + " · " + virtualIp;
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentTitle(title)
                .setContentText(body)
                .setContentIntent(pi)
                .setOngoing(true)
                .build();
    }

    @SuppressWarnings("unused")
    private InetSocketAddress noop() {
        return null;
    }
}
