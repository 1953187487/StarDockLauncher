package com.tungsten.hmclpe.multiplayer;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.VpnService;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.tungsten.hmclpe.R;

public final class Hin2nDialogController {

    private static final int VPN_REQUEST_CODE = 9917;

    private Hin2nDialogController() {
    }

    public static void show(@NonNull final Activity activity) {
        final View root = LayoutInflater.from(activity).inflate(R.layout.dialog_hin2n_menu, null);
        final AlertDialog dialog = new AlertDialog.Builder(activity)
                .setView(root)
                .setCancelable(true)
                .create();

        View create = root.findViewById(R.id.create);
        View join = root.findViewById(R.id.join);
        View info = root.findViewById(R.id.info);
        View help = root.findViewById(R.id.help);

        create.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                showCreateDialog(activity);
            }
        });
        join.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                showJoinDialog(activity);
            }
        });
        info.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                showInfoDialog(activity);
            }
        });
        help.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                showHelpDialog(activity);
            }
        });
        dialog.show();
    }

    private static void showCreateDialog(final Activity activity) {
        final EditText portInput = new EditText(activity);
        portInput.setHint(String.valueOf(Hin2nConfig.DEFAULT_GAME_PORT));
        portInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        new AlertDialog.Builder(activity)
                .setTitle(R.string.dialog_hin2n_menu_create)
                .setMessage(R.string.multiplayer_tunnel_intro)
                .setView(portInput)
                .setPositiveButton(android.R.string.ok, new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface d, int which) {
                        int port = parsePort(portInput.getText().toString(), Hin2nConfig.DEFAULT_GAME_PORT);
                        Hin2nConfig.setGamePort(activity, port);
                        MultiplayerSessionManager.get().createRoom(activity, port, new SessionCallback(activity, Hin2nRoom.Role.HOST));
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private static void showJoinDialog(final Activity activity) {
        final EditText codeInput = new EditText(activity);
        codeInput.setHint("ROOM-XXXXXXXX");
        final EditText portInput = new EditText(activity);
        portInput.setHint(String.valueOf(Hin2nConfig.DEFAULT_GAME_PORT));
        portInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);

        android.widget.LinearLayout container = new android.widget.LinearLayout(activity);
        container.setOrientation(android.widget.LinearLayout.VERTICAL);
        container.setPadding(48, 24, 48, 0);
        android.widget.TextView codeLabel = new android.widget.TextView(activity);
        codeLabel.setText("房间号");
        container.addView(codeLabel);
        container.addView(codeInput);
        android.widget.TextView portLabel = new android.widget.TextView(activity);
        portLabel.setText("游戏端口");
        portLabel.setPadding(0, 24, 0, 0);
        container.addView(portLabel);
        container.addView(portInput);

        new AlertDialog.Builder(activity)
                .setTitle(R.string.dialog_hin2n_menu_join)
                .setView(container)
                .setPositiveButton(android.R.string.ok, new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface d, int which) {
                        String code = codeInput.getText().toString().trim().toUpperCase();
                        if (!code.startsWith("ROOM-") || code.length() != 13) {
                            Toast.makeText(activity, R.string.multiplayer_tunnel_id_invalid, Toast.LENGTH_SHORT).show();
                            return;
                        }
                        int port = parsePort(portInput.getText().toString(), Hin2nConfig.DEFAULT_GAME_PORT);
                        Hin2nConfig.setGamePort(activity, port);
                        MultiplayerSessionManager.get().joinRoom(activity, code, port,
                                new SessionCallback(activity, Hin2nRoom.Role.CLIENT));
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private static void showInfoDialog(Activity activity) {
        Hin2nRoom room = MultiplayerSessionManager.get().getCurrentRoom();
        if (room == null || room.getState() == Hin2nRoom.State.IDLE) {
            Toast.makeText(activity, R.string.dialog_hin2n_menu_out, Toast.LENGTH_SHORT).show();
            return;
        }
        final View root = LayoutInflater.from(activity).inflate(R.layout.dialog_hin2n_community, null);
        final AlertDialog dialog = new AlertDialog.Builder(activity)
                .setView(root)
                .setCancelable(true)
                .create();
        root.findViewById(R.id.copy_invite_code).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                copyToClipboard(activity, room.getRoomCode());
                Toast.makeText(activity, R.string.dialog_community_copy_success, Toast.LENGTH_SHORT).show();
            }
        });
        root.findViewById(R.id.copy_ip_port).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String ip = TextUtils.isEmpty(room.getPeerVirtualIp()) ? room.getVirtualIp() : room.getPeerVirtualIp();
                String s = ip + ":" + room.getGamePort();
                copyToClipboard(activity, s);
                Toast.makeText(activity, R.string.dialog_community_copy_success, Toast.LENGTH_SHORT).show();
            }
        });
        ((android.widget.TextView) root.findViewById(R.id.invite_code)).setText(room.getRoomCode());
        ((android.widget.TextView) root.findViewById(R.id.ip_port))
                .setText((TextUtils.isEmpty(room.getPeerVirtualIp()) ? room.getVirtualIp() : room.getPeerVirtualIp())
                        + ":" + room.getGamePort());
        root.findViewById(R.id.exit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MultiplayerSessionManager.get().stop();
                if (activity != null) {
                    activity.startService(Hin2nService.prepareStop(activity));
                }
                dialog.dismiss();
            }
        });
        root.findViewById(R.id.positive).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        dialog.show();
    }

    private static void showHelpDialog(Activity activity) {
        new AlertDialog.Builder(activity)
                .setTitle(R.string.dialog_hin2n_help_title)
                .setMessage(R.string.dialog_hin2n_help_text)
                .setPositiveButton(R.string.dialog_hin2n_help_positive, null)
                .show();
    }

    private static int parsePort(String s, int def) {
        try {
            int p = Integer.parseInt(s.trim());
            if (p < 1024 || p > 65535) {
                return def;
            }
            return p;
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static void copyToClipboard(Context ctx, String text) {
        ClipboardManager cm = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null) {
            cm.setPrimaryClip(ClipData.newPlainText("StarDockLauncher", text));
        }
    }

    private static final class SessionCallback implements MultiplayerSessionManager.Listener {
        private final Activity activity;
        private final Hin2nRoom.Role role;

        SessionCallback(Activity activity, Hin2nRoom.Role role) {
            this.activity = activity;
            this.role = role;
        }

        @Override
        public void onStateChanged(@NonNull Hin2nRoom room) {
        }

        @Override
        public void onPeerConnected(@NonNull Hin2nRoom room) {
            MultiplayerSessionManager.get().bindForForeground();
            Intent prepare = VpnService.prepare(activity);
            Intent svc;
            if (prepare != null) {
                if (activity instanceof android.app.Activity) {
                    ((android.app.Activity) activity).startActivityForResult(prepare, VPN_REQUEST_CODE);
                }
                svc = Hin2nService.prepareStart(activity, role.name().toLowerCase(),
                        room.getGamePort(), room.getVirtualIp());
                activity.startService(svc);
            } else {
                svc = Hin2nService.prepareStart(activity, role.name().toLowerCase(),
                        room.getGamePort(), room.getVirtualIp());
                activity.startService(svc);
            }
            Toast.makeText(activity, R.string.multiplayer_floating_service_started, Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onError(@NonNull Hin2nRoom room, @NonNull String message) {
            Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
        }
    }
}
