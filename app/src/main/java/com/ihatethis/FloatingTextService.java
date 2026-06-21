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
import android.os.SystemClock;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import java.util.Calendar;

public class FloatingTextService extends Service {

    private WindowManager windowManager;
    private View floatingView;
    private TextView hateText;
    
    private SettingsManager settingsManager;
    private Handler handler = new Handler();
    private Runnable updateRunnable;
    private long startTime;
    
    // 血红色
    private static final int BLOOD_RED = Color.rgb(139, 0, 0);
    
    // 标记是否正在显示
    private boolean isShowing = false;
    
    private static final String CHANNEL_ID = "floating_service_channel";
    private static final int NOTIFICATION_ID = 1;

    @Override
    public void onCreate() {
        super.onCreate();
        settingsManager = new SettingsManager(this);
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        
        // 创建通知渠道（Android 8.0+）
        createNotificationChannel();
        
        // 启动前台服务
        startForeground(NOTIFICATION_ID, buildNotification());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if ("START".equals(action)) {
                checkAndShow();
            } else if ("STOP".equals(action)) {
                removeFloatingView();
                stopSelf();
            } else if ("PREVIEW".equals(action)) {
                // 预览模式：直接显示并快速播放动画
                showFloatingView();
                startPreviewAnimation();
            }
        }
        return START_STICKY;
    }
    
    /**
     * 创建通知渠道
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "悬浮窗服务",
                    NotificationManager.IMPORTANCE_MIN
            );
            channel.setDescription("后台运行悬浮窗服务");
            channel.setShowBadge(false);
            
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
    
    /**
     * 构建通知
     */
    private Notification buildNotification() {
        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(this);
        }
        
        builder.setContentTitle("系统服务")
                .setContentText("正在运行")
                .setSmallIcon(android.R.drawable.ic_menu_info_details)
                .setPriority(Notification.PRIORITY_MIN)
                .setOngoing(true);
        
        return builder.build();
    }

    /**
     * 检查时间并显示
     */
    private void checkAndShow() {
        if (isInTimeRange()) {
            showFloatingView();
            startTime = SystemClock.elapsedRealtime();
            startColorUpdate();
        } else {
            // 不在时间范围内，定时检查
            scheduleNextCheck();
        }
    }

    /**
     * 显示悬浮窗
     */
    private void showFloatingView() {
        if (isShowing || floatingView != null) {
            return;
        }

        floatingView = LayoutInflater.from(this).inflate(R.layout.floating_text, null);
        hateText = floatingView.findViewById(R.id.floating_hate_text);

        // 设置初始透明
        hateText.setTextColor(Color.argb(0, Color.red(BLOOD_RED), Color.green(BLOOD_RED), Color.blue(BLOOD_RED)));

        // 布局参数
        int layoutType;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutType = WindowManager.LayoutParams.TYPE_PHONE;
        }

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                        | WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                        | WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION,
                PixelFormat.TRANSLUCENT
        );

        params.gravity = Gravity.TOP | Gravity.LEFT;
        params.x = 0;
        params.y = 0;

        windowManager.addView(floatingView, params);
        isShowing = true;
    }

    /**
     * 移除悬浮窗
     */
    private void removeFloatingView() {
        if (floatingView != null && windowManager != null) {
            try {
                windowManager.removeView(floatingView);
            } catch (Exception e) {
                e.printStackTrace();
            }
            floatingView = null;
            hateText = null;
            isShowing = false;
        }
        
        if (handler != null && updateRunnable != null) {
            handler.removeCallbacks(updateRunnable);
            updateRunnable = null;
        }
    }

    /**
     * 检查是否在时间范围内
     */
    private boolean isInTimeRange() {
        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int minute = cal.get(Calendar.MINUTE);

        int currentMinutes = hour * 60 + minute;
        int startMinutes = settingsManager.getStartTotalMinutes();
        int endMinutes = settingsManager.getEndTotalMinutes();

        if (startMinutes <= endMinutes) {
            return currentMinutes >= startMinutes && currentMinutes <= endMinutes;
        } else {
            return currentMinutes >= startMinutes || currentMinutes <= endMinutes;
        }
    }

    /**
     * 更新文字颜色
     */
    private void updateTextColor() {
        if (hateText == null) return;

        long elapsed = SystemClock.elapsedRealtime() - startTime;
        int minutesPassed = (int) (elapsed / 60000);
        int fadeMinutes = settingsManager.getFadeMinutes();

        float ratio = (float) minutesPassed / fadeMinutes;
        ratio = Math.min(ratio, 1.0f);

        int alpha = (int) (ratio * 255);
        int color = Color.argb(alpha, Color.red(BLOOD_RED), Color.green(BLOOD_RED), Color.blue(BLOOD_RED));
        hateText.setTextColor(color);
    }

    /**
     * 启动定时更新颜色
     */
    private void startColorUpdate() {
        if (updateRunnable != null) {
            handler.removeCallbacks(updateRunnable);
        }

        updateRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isInTimeRange()) {
                    removeFloatingView();
                    scheduleNextCheck();
                    return;
                }

                updateTextColor();
                handler.postDelayed(this, 60000); // 每分钟更新
            }
        };

        // 立即更新一次
        updateTextColor();
        
        // 延迟到下一分钟整
        Calendar cal = Calendar.getInstance();
        int seconds = cal.get(Calendar.SECOND);
        int delay = (60 - seconds) * 1000;
        handler.postDelayed(updateRunnable, delay);
    }

    /**
     * 预览模式：快速播放动画
     */
    private void startPreviewAnimation() {
        if (updateRunnable != null) {
            handler.removeCallbacks(updateRunnable);
        }

        final int[] progress = {0};
        
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                if (hateText == null) return;

                progress[0] += 2;
                if (progress[0] > 100) {
                    progress[0] = 100;
                }

                float ratio = (float) progress[0] / 100.0f;
                int alpha = (int) (ratio * 255);
                int color = Color.argb(alpha, Color.red(BLOOD_RED), Color.green(BLOOD_RED), Color.blue(BLOOD_RED));
                hateText.setTextColor(color);

                if (progress[0] < 100) {
                    handler.postDelayed(this, 50);
                }
            }
        };

        handler.post(updateRunnable);
    }

    /**
     * 定时检查是否到了触发时间
     */
    private void scheduleNextCheck() {
        if (updateRunnable != null) {
            handler.removeCallbacks(updateRunnable);
        }

        updateRunnable = new Runnable() {
            @Override
            public void run() {
                if (isInTimeRange()) {
                    showFloatingView();
                    startTime = SystemClock.elapsedRealtime();
                    startColorUpdate();
                } else {
                    // 每分钟检查一次
                    handler.postDelayed(this, 60000);
                }
            }
        };

        handler.postDelayed(updateRunnable, 60000);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        removeFloatingView();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
