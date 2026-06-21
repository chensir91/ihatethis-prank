package com.ihatethis;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import java.util.Calendar;

public class MainActivity extends Activity {
    private TextView hateText;
    private Handler handler = new Handler();
    private Runnable updateRunnable;
    private long startTime; // 应用启动时间（开机时间）
    
    private SettingsManager settingsManager;
    
    // 血红色（深血红色）
    private static final int BLOOD_RED = Color.rgb(139, 0, 0);
    
    // 隐藏设置入口：连续点击计数
    private int tapCount = 0;
    private static final int TAPS_TO_SETTINGS = 5;
    private static final long TAP_TIMEOUT = 2000; // 2秒内连续点击
    private Handler tapHandler = new Handler();
    private Runnable tapResetRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        settingsManager = new SettingsManager(this);
        
        // 全屏透明
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, 
                            WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        
        setContentView(R.layout.activity_main);
        
        hateText = findViewById(R.id.hate_text);
        
        // 设置点击事件（隐藏入口）
        setupTapDetector();
        
        // 检查当前时间是否在触发范围内
        if (!isInTimeRange()) {
            // 不在时间范围内，直接进入设置界面
            openSettings();
            finish();
            return;
        }
        
        // 记录启动时间
        startTime = SystemClock.elapsedRealtime();
        
        // 设置初始颜色深度
        updateTextColor();
        
        // 启动定时更新
        startColorUpdate();
    }
    
    /**
     * 设置连续点击检测器（隐藏入口进入设置）
     */
    private void setupTapDetector() {
        hateText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleTap();
            }
        });
        
        // 也可以点击整个屏幕
        findViewById(android.R.id.content).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleTap();
            }
        });
    }
    
    private void handleTap() {
        tapCount++;
        
        // 重置计时器
        if (tapResetRunnable != null) {
            tapHandler.removeCallbacks(tapResetRunnable);
        }
        
        tapResetRunnable = new Runnable() {
            @Override
            public void run() {
                tapCount = 0;
            }
        };
        tapHandler.postDelayed(tapResetRunnable, TAP_TIMEOUT);
        
        // 检查是否达到进入设置的次数
        if (tapCount >= TAPS_TO_SETTINGS) {
            tapCount = 0;
            openSettings();
        }
    }
    
    /**
     * 打开设置界面
     */
    private void openSettings() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }
    
    /**
     * 检查当前时间是否在触发范围内
     */
    private boolean isInTimeRange() {
        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int minute = cal.get(Calendar.MINUTE);
        
        int currentMinutes = hour * 60 + minute;
        int startMinutes = settingsManager.getStartTotalMinutes();
        int endMinutes = settingsManager.getEndTotalMinutes();
        
        // 处理跨天的情况（比如开始时间22:00，结束时间06:00）
        if (startMinutes <= endMinutes) {
            return currentMinutes >= startMinutes && currentMinutes <= endMinutes;
        } else {
            // 跨天：从开始时间到24:00，或者从0:00到结束时间
            return currentMinutes >= startMinutes || currentMinutes <= endMinutes;
        }
    }
    
    /**
     * 计算从开机（应用启动）到现在经过了多少分钟
     */
    private int getMinutesSinceStart() {
        long elapsed = SystemClock.elapsedRealtime() - startTime;
        return (int) (elapsed / 60000); // 转换成分钟
    }
    
    /**
     * 更新文字颜色深度
     */
    private void updateTextColor() {
        int minutesPassed = getMinutesSinceStart();
        int fadeMinutes = settingsManager.getFadeMinutes();
        
        // 计算alpha值：0-255
        float ratio = (float) minutesPassed / fadeMinutes;
        ratio = Math.min(ratio, 1.0f); // 最大1.0
        
        int alpha = (int) (ratio * 255);
        
        // 设置颜色：血红色 + 计算出的透明度
        int color = Color.argb(alpha, Color.red(BLOOD_RED), Color.green(BLOOD_RED), Color.blue(BLOOD_RED));
        hateText.setTextColor(color);
    }
    
    /**
     * 启动定时更新，每分钟更新一次颜色
     */
    private void startColorUpdate() {
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isInTimeRange()) {
                    // 超过结束时间，停止并退出
                    finish();
                    return;
                }
                
                updateTextColor();
                
                // 下一分钟再更新
                handler.postDelayed(this, 60000); // 60秒
            }
        };
        
        // 先延迟到下一分钟整开始
        Calendar cal = Calendar.getInstance();
        int seconds = cal.get(Calendar.SECOND);
        int delay = (60 - seconds) * 1000;
        
        handler.postDelayed(updateRunnable, delay);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // 从设置返回时，重新检查时间
        if (isInTimeRange() && updateRunnable == null) {
            startTime = SystemClock.elapsedRealtime();
            updateTextColor();
            startColorUpdate();
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handler != null && updateRunnable != null) {
            handler.removeCallbacks(updateRunnable);
        }
        if (tapHandler != null && tapResetRunnable != null) {
            tapHandler.removeCallbacks(tapResetRunnable);
        }
    }
    
    @Override
    public void onBackPressed() {
        // 允许返回键退出
        super.onBackPressed();
    }
}
