package com.ihatethis;

import android.app.Notification;
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
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;

public class FloatingTextService extends Service {

    private static final String CHANNEL_ID = "floating_text_v4";
    private static final int NOTIFY_ID = 1;

    private WindowManager wm;
    private SettingsManager sm;
    private Handler mainHandler;
    private Random rng = new Random();

    private boolean running = false;
    private final List<FloatingText> activeTexts = new ArrayList<>();

    @Override
    public void onCreate() {
        super.onCreate();
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        sm = new SettingsManager(this);
        mainHandler = new Handler(Looper.getMainLooper());
        createChannel();
        startForeground(NOTIFY_ID, buildNotification());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if ("START".equals(action)) { startLoop(); }
            else if ("STOP".equals(action)) { stopLoop(); stopSelf(); }
            else if ("PREVIEW".equals(action)) { doPreview(); }
        }
        return START_NOT_STICKY;
    }

    // ==================== Main Loop ====================

    private void startLoop() {
        if (running) return;
        running = true;
        scheduleNext();
    }

    private void stopLoop() {
        running = false;
        mainHandler.removeCallbacksAndMessages(null);
        clearAllTexts();
    }

    private void scheduleNext() {
        if (!running) return;

        Calendar cal = Calendar.getInstance();
        int nowMin = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE);
        float progress = sm.getTimeProgress(nowMin);

        if (progress < 0) {
            // Not in range, check again in 30s
            mainHandler.postDelayed(this::scheduleNext, 30000);
            return;
        }

        // Spawn text
        spawnText(progress);

        // Schedule next based on frequency
        int freq = sm.getInterpolatedFrequency(progress);
        if (freq <= 0) freq = 1;
        long interval = 60000L / freq;
        // Add slight randomness ±20%
        interval = (long)(interval * (0.8 + rng.nextFloat() * 0.4));
        mainHandler.postDelayed(this::scheduleNext, interval);
    }

    // ==================== Spawn & Animate ====================

    private void spawnText(float progress) {
        String[] pool = sm.getTexts();
        List<String> valid = new ArrayList<>();
        for (String s : pool) if (s != null && !s.isEmpty()) valid.add(s);
        if (valid.isEmpty()) return;

        String text = valid.get(rng.nextInt(valid.size()));

        // Font size: random between min and max
        int minF = sm.getInterpolatedMinFont(progress);
        int maxF = sm.getInterpolatedMaxFont(progress);
        if (maxF < minF) maxF = minF;
        int fontSize = minF + rng.nextInt(maxF - minF + 1);

        int color = sm.getFontColor();
        int displayMs = sm.getDisplayDurationMs();
        int typeMs = sm.getTypingSpeedMs();
        float dirDeg = sm.getInterpolatedDirection(progress);
        float shake = sm.getInterpolatedShake(progress);

        // Random rotation direction (±)
        if (rng.nextBoolean()) dirDeg = -dirDeg;

        FloatingText ft = new FloatingText(text, fontSize, color, typeMs, displayMs, dirDeg, shake);
        activeTexts.add(ft);
        ft.show();
    }

    private void clearAllTexts() {
        for (FloatingText ft : new ArrayList<>(activeTexts)) {
            ft.destroy();
        }
        activeTexts.clear();
    }

    // ==================== Preview ====================
    private void doPreview() {
        Calendar cal = Calendar.getInstance();
        int nowMin = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE);
        float p = sm.getTimeProgress(nowMin);
        if (p < 0) p = 0.5f;
        spawnText(p);

        // Auto clear after display duration
        int dur = sm.getDisplayDurationMs();
        mainHandler.postDelayed(() -> {
            if (!activeTexts.isEmpty()) {
                activeTexts.get(0).destroy();
                activeTexts.remove(0);
            }
            stopSelf();
        }, dur + 2000);
    }

    // ==================== FloatingText (inner class) ====================

    private class FloatingText {
        private final String fullText;
        private final int fontSize;
        private final int color;
        private final int typeMs;
        private final int displayMs;
        private final float dirDeg;
        private final float shakeIntensity;

        private View view;
        private TextView tv;
        private WindowManager.LayoutParams params;
        private Handler h = new Handler(Looper.getMainLooper());
        private boolean destroyed = false;

        // Shake state
        private float baseX, baseY;
        private Runnable shakeRunnable;

        FloatingText(String text, int fs, int c, int tms, int dms, float dir, float shk) {
            this.fullText = text;
            this.fontSize = fs;
            this.color = c;
            this.typeMs = tms;
            this.displayMs = dms;
            this.dirDeg = dir;
            this.shakeIntensity = shk;
        }

        void show() {
            view = LayoutInflater.from(FloatingTextService.this).inflate(R.layout.floating_text, null);
            tv = view.findViewById(R.id.floating_hate_text);
            tv.setTextSize(fontSize);
            tv.setTextColor(color);
            tv.setText(""); // start empty

            // Layout params
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
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                            | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    PixelFormat.TRANSLUCENT
            );
            params.gravity = Gravity.TOP | Gravity.LEFT;

            // Random position (avoid edges)
            int maxW = wm.getDefaultDisplay().getWidth();
            int maxH = wm.getDefaultDisplay().getHeight();
            baseX = rng.nextInt(Math.max(10, maxW - 100));
            baseY = rng.nextInt(Math.max(50, maxH - 200));
            params.x = (int) baseX;
            params.y = (int) baseY;

            // Apply rotation
            view.setRotation(dirDeg);

            wm.addView(view, params);

            // Start typewriter
            startTypewriter(0);

            // Start shake
            if (shakeIntensity > 0) startShake();

            // Schedule fade-out and removal
            h.postDelayed(this::fadeOut, displayMs);
        }

        private void startTypewriter(int index) {
            if (destroyed) return;
            if (index < fullText.length()) {
                tv.setText(fullText.substring(0, index + 1));
                h.postDelayed(() -> startTypewriter(index + 1), typeMs);
            }
        }

        private void startShake() {
            shakeRunnable = new Runnable() {
                @Override
                public void run() {
                    if (destroyed || view == null) return;
                    float dx = (rng.nextFloat() - 0.5f) * 2 * shakeIntensity;
                    float dy = (rng.nextFloat() - 0.5f) * 2 * shakeIntensity;
                    params.x = (int)(baseX + dx);
                    params.y = (int)(baseY + dy);
                    try { wm.updateViewLayout(view, params); } catch (Exception ignored) {}
                    h.postDelayed(this, 40 + rng.nextInt(30));
                }
            };
            h.post(shakeRunnable);
        }

        private void fadeOut() {
            if (destroyed || view == null) return;
            // Simple alpha fade
            view.animate().alpha(0f).setDuration(800).withEndAction(this::destroy).start();
        }

        void destroy() {
            if (destroyed) return;
            destroyed = true;
            if (shakeRunnable != null) h.removeCallbacks(shakeRunnable);
            h.removeCallbacksAndMessages(null);
            if (view != null) {
                try { wm.removeView(view); } catch (Exception ignored) {}
                view = null;
            }
            activeTexts.remove(this);
        }
    }

    // ==================== Notification ====================

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(CHANNEL_ID, "悬浮文字服务",
                    NotificationManager.IMPORTANCE_MIN);
            ch.setDescription("后台悬浮文字");
            ch.setShowBadge(false);
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(ch);
        }
    }

    private Notification buildNotification() {
        Notification.Builder b;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            b = new Notification.Builder(this, CHANNEL_ID);
        } else {
            b = new Notification.Builder(this);
        }
        return b.setContentTitle("系统服务")
                .setContentText("正在运行")
                .setSmallIcon(android.R.drawable.ic_menu_info_details)
                .setPriority(Notification.PRIORITY_MIN)
                .setOngoing(true)
                .build();
    }

    @Override
    public void onDestroy() {
        stopLoop();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
