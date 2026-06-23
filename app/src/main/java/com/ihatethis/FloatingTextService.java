package com.ihatethis;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import androidx.core.app.NotificationCompat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;

public class FloatingTextService extends Service {

    private static final String TAG = "FloatingText";
    private static final String CHANNEL_ID = "ihatethis_fg";
    private static final int NOTIF_ID = 1;

    private SettingsManager sm;
    private WindowManager wm;
    private Handler mainHandler;
    private Random rng = new Random();
    private boolean running;
    private List<FloatingText> activeTexts = new ArrayList<>();

    @Override
    public void onCreate() {
        super.onCreate();
        sm = new SettingsManager(this);
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        mainHandler = new Handler(Looper.getMainLooper());
        createNotificationChannel();
        startForeground(NOTIF_ID, new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("我好恨 服务").setContentText("浮动文字服务运行中")
                .setOngoing(true).setPriority(NotificationCompat.PRIORITY_LOW).build());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_NOT_STICKY;
        String action = intent.getAction();
        if (action == null) return START_NOT_STICKY;

        // Verify overlay permission before doing anything
        if (!hasOverlayPerm()) {
            stopSelf();
            return START_NOT_STICKY;
        }

        switch (action) {
            case "START":
                stopLoop();
                startLoop();
                break;
            case "PREVIEW":
                stopLoop();
                doPreview();
                break;
            case "STOP":
                stopLoop();
                break;
        }
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onDestroy() {
        stopLoop();
        super.onDestroy();
    }

    // ==================== Loop ====================

    private void startLoop() {
        if (running) return;
        running = true;
        scheduleNext();
        Log.d(TAG, "Loop started");
    }

    private void stopLoop() {
        running = false;
        mainHandler.removeCallbacksAndMessages(null);
        for (FloatingText ft : activeTexts) ft.destroy();
        activeTexts.clear();
        Log.d(TAG, "Loop stopped");
    }

    private void scheduleNext() {
        if (!running) return;
        Calendar cal = Calendar.getInstance();
        int nowMin = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE);
        float progress = sm.getTimeProgress(nowMin);
        if (progress < 0) {
            // Not in trigger window, check again in 30s
            mainHandler.postDelayed(this::scheduleNext, 30000);
            return;
        }
        spawnText(progress);
        int freq = sm.getInterpolatedFrequency(progress);
        if (freq <= 0) freq = 1;
        long interval = 60000L / freq;
        interval = (long)(interval * (0.8 + rng.nextFloat() * 0.4));
        mainHandler.postDelayed(this::scheduleNext, interval);
    }

    private void doPreview() {
        Calendar cal = Calendar.getInstance();
        int nowMin = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE);
        float p = sm.getTimeProgress(nowMin);
        if (p < 0) p = 0.5f; // fallback to midpoint if outside window
        spawnText(p);
        // Auto-stop after display
        mainHandler.postDelayed(() -> {
            for (FloatingText ft : activeTexts) ft.destroy();
            activeTexts.clear();
            stopSelf();
        }, sm.getDisplayDurationMs() + 2500);
    }

    // ==================== Spawn ====================

    private void spawnText(float progress) {
        String[] pool = sm.getTexts();
        List<String> valid = new ArrayList<>();
        for (String s : pool) if (s != null && !s.isEmpty()) valid.add(s);
        if (valid.isEmpty()) return;

        String text = valid.get(rng.nextInt(valid.size()));
        FloatingText ft = new FloatingText(text, progress);
        try {
            ft.show();
            activeTexts.add(ft);
        } catch (Exception e) {
            Log.e(TAG, "Failed to show floating text: " + e.getMessage());
        }
    }

    // ==================== Floating Text ====================

    private class FloatingText {
        final String message;
        final float progress;
        View view;
        TextView tv;
        boolean destroyed;
        WindowManager.LayoutParams params;

        FloatingText(String msg, float p) {
            message = msg; progress = p;
        }

        void show() {
            if (destroyed) return;
            view = LayoutInflater.from(FloatingTextService.this)
                    .inflate(R.layout.floating_text, null);
            tv = view.findViewById(R.id.tv_text);
            if (tv == null) return;
            tv.setText(message);

            int minF = sm.getInterpolatedMinFont(progress);
            int maxF = sm.getInterpolatedMaxFont(progress);
            int fs = minF + rng.nextInt(Math.max(1, maxF - minF + 1));
            tv.setTextSize(fs);
            tv.setTextColor(sm.getFontColor());
            tv.setShadowLayer(4 + rng.nextFloat() * 4,
                    2 + rng.nextFloat() * 3, 2 + rng.nextFloat() * 3, 0x88000000);

            params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                            ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                            : WindowManager.LayoutParams.TYPE_PHONE,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                            | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    PixelFormat.TRANSLUCENT);

            int screenW = wm.getDefaultDisplay().getWidth();
            int screenH = wm.getDefaultDisplay().getHeight();
            int x = rng.nextInt(Math.max(1, screenW - 100));
            int y = rng.nextInt(Math.max(1, screenH - 100));

            params.gravity = Gravity.TOP | Gravity.LEFT;
            params.x = x; params.y = y;

            // Direction offset
            float dir = sm.getInterpolatedDirection(progress);
            if (dir > 0) {
                float rad = (float) Math.toRadians(dir);
                if (rng.nextBoolean()) rad = -rad;
                view.setRotation(rad * 180f / (float) Math.PI);
            }

            // Shake
            float shakeIntensity = sm.getInterpolatedShake(progress);
            startShake(shakeIntensity);

            wm.addView(view, params);

            // Typewriter
            startTypewriter();

            // Fade out
            int dur = sm.getDisplayDurationMs();
            int speed = sm.getTypingSpeedMs();
            long fadeDelay = dur + speed * message.length();
            mainHandler.postDelayed(() -> {
                if (!destroyed && view != null) {
                    view.animate().alpha(0f).setDuration(400).withEndAction(this::destroy).start();
                }
            }, fadeDelay);

            // Remove from activeTexts after fade
            mainHandler.postDelayed(() -> {
                if (!destroyed) {
                    activeTexts.remove(this);
                    destroy();
                }
            }, fadeDelay + 500);
        }

        void startTypewriter() {
            if (destroyed || tv == null) return;
            tv.setText("");
            int speed = sm.getTypingSpeedMs();
            final int[] idx = {0};
            Runnable type = new Runnable() {
                @Override
                public void run() {
                    if (destroyed || tv == null) return;
                    if (idx[0] < message.length()) {
                        tv.setText(message.substring(0, idx[0] + 1));
                        idx[0]++;
                        mainHandler.postDelayed(this, speed);
                    }
                }
            };
            mainHandler.post(type);
        }

        void startShake(float intensity) {
            if (destroyed || view == null || intensity < 0.5f) return;
            final int origX = params.x, origY = params.y;
            Runnable shake = new Runnable() {
                @Override
                public void run() {
                    if (destroyed || view == null) return;
                    params.x = origX + (int)((rng.nextFloat() - 0.5f) * 2 * intensity);
                    params.y = origY + (int)((rng.nextFloat() - 0.5f) * 2 * intensity);
                    try { wm.updateViewLayout(view, params); } catch (Exception ignored) {}
                    mainHandler.postDelayed(this, 50 + rng.nextInt(30));
                }
            };
            mainHandler.post(shake);
        }

        void destroy() {
            if (destroyed) return;
            destroyed = true;
            if (view != null && view.getWindowToken() != null) {
                try { wm.removeView(view); } catch (Exception ignored) {}
            }
            view = null;
        }
    }

    // ==================== Utils ====================

    private boolean hasOverlayPerm() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            return android.provider.Settings.canDrawOverlays(this);
        return true;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID, "我好恨服务", NotificationManager.IMPORTANCE_LOW);
            ch.setDescription("浮动文字服务通知");
            getSystemService(NotificationManager.class).createNotificationChannel(ch);
        }
    }
}
