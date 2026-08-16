package com.tungsten.hmclpe.ai;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.tungsten.hmclpe.R;
import com.tungsten.hmclpe.launcher.multiplayer.TaowaPrefs;
import com.tungsten.hmclpe.launcher.uis.multiplayer.MultiplayerActivity;

public class AiOverlayService extends Service {

    public static final String ACTION_START = "com.tungsten.hmclpe.ai.action.START";
    public static final String ACTION_STOP = "com.tungsten.hmclpe.ai.action.STOP";

    private WindowManager windowManager;
    private View floatBall;
    private WindowManager.LayoutParams params;

    private float touchStartX;
    private float touchStartY;
    private float initialX;
    private float initialY;
    private long touchDownTime;
    private boolean isDragging;

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (canDrawOverlay()) {
            addBall();
        }
        return START_STICKY;
    }

    private boolean canDrawOverlay() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void addBall() {
        if (floatBall != null) return;
        floatBall = LayoutInflater.from(this).inflate(R.layout.view_ai_float_ball, null);
        int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;
        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 0;
        params.y = 300;

        floatBall.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    isDragging = false;
                    touchDownTime = System.currentTimeMillis();
                    initialX = params.x;
                    initialY = params.y;
                    touchStartX = event.getRawX();
                    touchStartY = event.getRawY();
                    return true;
                case MotionEvent.ACTION_MOVE:
                    if (Math.abs(event.getRawX() - touchStartX) > 10 || Math.abs(event.getRawY() - touchStartY) > 10) {
                        isDragging = true;
                        params.x = (int) initialX + (int) (event.getRawX() - touchStartX);
                        params.y = (int) initialY + (int) (event.getRawY() - touchStartY);
                        try { windowManager.updateViewLayout(floatBall, params); } catch (Exception ignored) {}
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                    if (!isDragging) {
                        long duration = System.currentTimeMillis() - touchDownTime;
                        if (duration < 400) showMenu();
                    }
                    return true;
                default:
                    return false;
            }
        });
        try { windowManager.addView(floatBall, params); } catch (Exception e) { floatBall = null; }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void showMenu() {
        try {
            LinearLayout menu = new LinearLayout(this);
            menu.setOrientation(LinearLayout.VERTICAL);
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(Color.parseColor("#E61A1F2A"));
            bg.setCornerRadius(20f);
            menu.setBackground(bg);
            menu.setPadding(20, 20, 20, 20);

            addMenuRow(menu, "🤖 AI 助手", () -> openChat());
            if (TaowaPrefs.isEnabled(this)) {
                addMenuRow(menu, "🛰 淘瓦联机", () -> openTaowa());
            }
            addMenuRow(menu, "❌ 关闭悬浮窗", () -> { removeBall(); stopSelf(); });

            int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                    ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                    : WindowManager.LayoutParams.TYPE_PHONE;
            WindowManager.LayoutParams p = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    type,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_DIM_BEHIND,
                    PixelFormat.TRANSLUCENT);
            p.gravity = Gravity.TOP | Gravity.START;
            p.x = Math.max(params.x, 0);
            p.y = Math.max(params.y - 240, 0);
            p.dimAmount = 0.4f;
            menu.setOnTouchListener((v, event) -> {
                if (event.getAction() == MotionEvent.ACTION_OUTSIDE) {
                    try { windowManager.removeView(menu); } catch (Exception ignored) {}
                    return true;
                }
                return false;
            });
            windowManager.addView(menu, p);
            menu.setTag("menu");
        } catch (Throwable t) {
            Toast.makeText(this, "菜单打开失败：" + t.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void addMenuRow(LinearLayout parent, String text, Runnable onClick) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(15);
        tv.setTextColor(Color.parseColor("#FFB454"));
        tv.setPadding(20, 18, 20, 18);
        tv.setOnClickListener(v -> {
            try {
                if (parent.getParent() instanceof android.view.ViewParent) {
                    try { windowManager.removeView(parent); } catch (Throwable ignored) {}
                }
            } catch (Throwable ignored) {}
            onClick.run();
        });
        parent.addView(tv);
    }

    private void openChat() {
        try {
            Intent i = new Intent(this, AiChatActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(i);
        } catch (Throwable t) {
            Toast.makeText(this, "AI 启动失败：" + t.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void openTaowa() {
        try {
            Intent i = new Intent(this, MultiplayerActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(i);
        } catch (Throwable t) {
            Toast.makeText(this, "淘瓦启动失败：" + t.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void removeBall() {
        if (floatBall != null) {
            try { windowManager.removeView(floatBall); } catch (Exception ignored) {}
            floatBall = null;
        }
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onDestroy() {
        super.onDestroy();
        removeBall();
    }
}
