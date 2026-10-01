package com.rsm.overlay;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.Switch;
import androidx.annotation.Nullable;

public class FloatingService extends Service {

    private WindowManager wm;
    private View overlay;
    private WindowManager.LayoutParams params;
    private float touchX, touchY;
    private int startX, startY;

    @Nullable @Override
    public IBinder onBind(Intent i) { return null; }

    @Override
    public void onCreate() {
        super.onCreate();
        startForeground(1, buildNotification());
    }

    @Override
    public int onStartCommand(Intent i, int flags, int startId) {
        if (overlay != null) return START_STICKY;
        showOverlay();
        return START_STICKY;
    }

    private void showOverlay() {
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);

        int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 60;
        params.y = 200;

        overlay = LayoutInflater.from(this).inflate(R.layout.overlay, null);

        View header = overlay.findViewById(R.id.header);
        ImageButton close = overlay.findViewById(R.id.btn_close);

        close.setOnClickListener(v -> {
            if (overlay != null) {
                wm.removeView(overlay);
                overlay = null;
            }
            stopSelf();
        });

        header.setOnTouchListener((v, e) -> {
            switch (e.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    touchX = e.getRawX();
                    touchY = e.getRawY();
                    startX = params.x;
                    startY = params.y;
                    return true;
                case MotionEvent.ACTION_MOVE:
                    params.x = startX + (int)(e.getRawX() - touchX);
                    params.y = startY + (int)(e.getRawY() - touchY);
                    wm.updateViewLayout(overlay, params);
                    return true;
            }
            return false;
        });

        int[] switchIds = {
                R.id.sw_box, R.id.sw_filled, R.id.sw_health, R.id.sw_armor,
                R.id.sw_skeleton, R.id.sw_chams, R.id.sw_ragdoll
        };
        for (int id : switchIds) {
            Switch sw = overlay.findViewById(id);
            sw.setOnCheckedChangeListener((v, checked) -> { });
        }

        wm.addView(overlay, params);
    }

    private Notification buildNotification() {
        String ch = "rsm_overlay";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel c = new NotificationChannel(
                    ch, "RSM Overlay", NotificationManager.IMPORTANCE_LOW);
            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE))
                    .createNotificationChannel(c);
        }
        Notification.Builder b = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(this, ch)
                : new Notification.Builder(this);
        return b.setContentTitle("RSM Overlay")
                .setContentText("Меню активно")
                .setSmallIcon(android.R.drawable.ic_menu_view)
                .build();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (overlay != null && wm != null) {
            wm.removeView(overlay);
            overlay = null;
        }
    }
}
