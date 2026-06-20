package com.ihatethis;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import java.util.Calendar;

public class MainActivity extends Activity {

    private TextView hateText;
    private Handler handler = new Handler();
    private Runnable updateRunnable;
    private long startTime; // 应用启动时间（开机时间）
    
    // 血红色（深血红色）
    private static final int BLOOD_RED = Color.rgb(139, 0, 0);
    // 结束时间：4:44
    private static final int END_HOUR = 4;
    private static final int END_MINUTE = 44;
    // 完全显示需要的分钟数：240分钟
    private static final int FULL_ALPHA_MINUTES = 240;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 全屏透明
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, 
                            WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        
        setContentView(R.layout.activity_main);
        
        hateText = findViewById(R.id.hate_text);
        
        // 检查当前时间是否在触发范围内
        if (!isInTimeRange()) {
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
     * 检查当前时间是否在0:00 - 4:44之间
     */
    private boolean isInTimeRange() {
        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int minute = cal.get(Calendar.MINUTE);
        
        int currentMinutes = hour * 60 + minute;
        int endMinutes = END_HOUR * 60 + END_MINUTE;
        
        return currentMinutes >= 0 && currentMinutes <= endMinutes;
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
        
        // 计算alpha值：0-255
        // 240分钟后达到完全不透明
        float ratio = (float) minutesPassed / FULL_ALPHA_MINUTES;
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
                    // 超过4:44，停止并退出
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
    protected void onDestroy() {
        super.onDestroy();
        if (handler != null && updateRunnable != null) {
            handler.removeCallbacks(updateRunnable);
        }
    }
    
    @Override
    public void onBackPressed() {
        // 允许返回键退出
        super.onBackPressed();
    }
}
