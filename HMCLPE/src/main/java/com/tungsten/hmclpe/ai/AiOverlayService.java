package com.tungsten.hmclpe.ai;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import com.tungsten.hmclpe.R;

public class AiOverlayService extends Service {

    public static final String ACTION_START = "com.tungsten.hmclpe.ai.START";
    public static final String ACTION_STOP = "com.tungsten.hmclpe.ai.STOP";

    private WindowManager windowManager;
    private View floatBall;
    private WindowManager.LayoutParams params;

    private int initialX;
    private int initialY;
    private float touchStartX;
    private float touchStartY;
    private boolean isDragging = false;
    private long touchDownTime = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            removeBall();
            stopSelf();
            return START_NOT_STICKY;
        }
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
        if (floatBall != null) {
            return;
        }
        floatBall = LayoutInflater.from(this).inflate(R.layout.view_ai_float_ball, null);
        int type;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            type = WindowManager.LayoutParams.TYPE_PHONE;
        }
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
                        params.x = initialX + (int) (event.getRawX() - touchStartX);
                        params.y = initialY + (int) (event.getRawY() - touchStartY);
                        try {
                            windowManager.updateViewLayout(floatBall, params);
                        } catch (Exception ignored) {
                        }
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                    if (!isDragging) {
                        long duration = System.currentTimeMillis() - touchDownTime;
                        if (duration < 400) {
                            openChat();
                        }
                    }
                    return true;
                default:
                    return false;
            }
        });

        try {
            windowManager.addView(floatBall, params);
        } catch (Exception e) {
            floatBall = null;
        }
    }

    private void removeBall() {
        if (floatBall != null) {
            try {
                windowManager.removeView(floatBall);
            } catch (Exception ignored) {
            }
            floatBall = null;
        }
    }

    private void openChat() {
        Intent intent = new Intent(this, AiChatActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        removeBall();
    }
}
