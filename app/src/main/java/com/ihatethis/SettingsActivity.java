package com.ihatethis;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class SettingsActivity extends Activity {

    private SettingsManager settingsManager;
    
    // 时间选择器
    private NumberPicker npStartHour, npStartMinute;
    private NumberPicker npEndHour, npEndMinute;
    
    // 渐变时长
    private SeekBar sbFadeMinutes;
    private TextView tvFadeValue;
    
    // 预览相关
    private TextView previewText;
    private SeekBar sbPreviewProgress;
    private TextView tvPreviewProgress;
    private Button btnPlayAnimation, btnStopAnimation;
    
    // 悬浮窗控制
    private Button btnStartService, btnStopService, btnPreviewFloat;
    private TextView tvPermissionStatus;
    
    // 按钮
    private Button btnSave, btnReset, btnBack;
    
    // 动画相关
    private Handler animationHandler = new Handler();
    private Runnable animationRunnable;
    private boolean isAnimating = false;
    private int animationProgress = 0;
    
    // 血红色
    private static final int BLOOD_RED = Color.rgb(139, 0, 0);
    
    private static final int REQUEST_OVERLAY_PERMISSION = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        
        settingsManager = new SettingsManager(this);
        
        initViews();
        setupNumberPickers();
        setupListeners();
        loadSettings();
        updatePreview(0);
        checkPermissionStatus();
    }
    
    private void initViews() {
        npStartHour = findViewById(R.id.np_start_hour);
        npStartMinute = findViewById(R.id.np_start_minute);
        npEndHour = findViewById(R.id.np_end_hour);
        npEndMinute = findViewById(R.id.np_end_minute);
        
        sbFadeMinutes = findViewById(R.id.sb_fade_minutes);
        tvFadeValue = findViewById(R.id.tv_fade_value);
        
        previewText = findViewById(R.id.preview_text);
        sbPreviewProgress = findViewById(R.id.sb_preview_progress);
        tvPreviewProgress = findViewById(R.id.tv_preview_progress);
        btnPlayAnimation = findViewById(R.id.btn_play_animation);
        btnStopAnimation = findViewById(R.id.btn_stop_animation);
        
        btnStartService = findViewById(R.id.btn_start_service);
        btnStopService = findViewById(R.id.btn_stop_service);
        btnPreviewFloat = findViewById(R.id.btn_preview_float);
        tvPermissionStatus = findViewById(R.id.tv_permission_status);
        
        btnSave = findViewById(R.id.btn_save);
        btnReset = findViewById(R.id.btn_reset);
        btnBack = findViewById(R.id.btn_back);
    }
    
    private void setupNumberPickers() {
        // 小时：0-23
        npStartHour.setMinValue(0);
        npStartHour.setMaxValue(23);
        npEndHour.setMinValue(0);
        npEndHour.setMaxValue(23);
        
        // 分钟：0-59
        npStartMinute.setMinValue(0);
        npStartMinute.setMaxValue(59);
        npEndMinute.setMinValue(0);
        npEndMinute.setMaxValue(59);
        
        // 设置格式化器，显示两位数
        NumberPicker.Formatter formatter = new NumberPicker.Formatter() {
            @Override
            public String format(int value) {
                return String.format("%02d", value);
            }
        };
        npStartHour.setFormatter(formatter);
        npStartMinute.setFormatter(formatter);
        npEndHour.setFormatter(formatter);
        npEndMinute.setFormatter(formatter);
    }
    
    private void setupListeners() {
        // 渐变时长拖动
        sbFadeMinutes.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int minutes = Math.max(1, progress);
                tvFadeValue.setText(minutes + "分钟");
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        
        // 预览进度拖动
        sbPreviewProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    stopAnimation();
                    updatePreview(progress);
                }
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        
        // 播放动画
        btnPlayAnimation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startAnimation();
            }
        });
        
        // 停止动画
        btnStopAnimation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopAnimation();
            }
        });
        
        // 启动悬浮服务
        btnStartService.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!checkOverlayPermission()) {
                    requestOverlayPermission();
                    return;
                }
                saveSettings();
                startFloatingService("START");
                Toast.makeText(SettingsActivity.this, "悬浮服务已启动", Toast.LENGTH_SHORT).show();
            }
        });
        
        // 停止悬浮服务
        btnStopService.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startFloatingService("STOP");
                Toast.makeText(SettingsActivity.this, "悬浮服务已停止", Toast.LENGTH_SHORT).show();
            }
        });
        
        // 预览悬浮效果
        btnPreviewFloat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!checkOverlayPermission()) {
                    requestOverlayPermission();
                    return;
                }
                saveSettings();
                startFloatingService("PREVIEW");
                Toast.makeText(SettingsActivity.this, "悬浮预览已启动，3秒后自动关闭", Toast.LENGTH_SHORT).show();
                
                // 3秒后自动停止预览
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        startFloatingService("STOP");
                    }
                }, 3000);
            }
        });
        
        // 权限状态点击
        tvPermissionStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestOverlayPermission();
            }
        });
        
        // 保存
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveSettings();
                Toast.makeText(SettingsActivity.this, "设置已保存", Toast.LENGTH_SHORT).show();
            }
        });
        
        // 恢复默认
        btnReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                settingsManager.resetToDefault();
                loadSettings();
                updatePreview(0);
                Toast.makeText(SettingsActivity.this, "已恢复默认设置", Toast.LENGTH_SHORT).show();
            }
        });
        
        // 返回
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
    
    private void loadSettings() {
        npStartHour.setValue(settingsManager.getStartHour());
        npStartMinute.setValue(settingsManager.getStartMinute());
        npEndHour.setValue(settingsManager.getEndHour());
        npEndMinute.setValue(settingsManager.getEndMinute());
        
        int fadeMinutes = settingsManager.getFadeMinutes();
        sbFadeMinutes.setProgress(fadeMinutes);
        tvFadeValue.setText(fadeMinutes + "分钟");
    }
    
    private void saveSettings() {
        settingsManager.setStartTime(npStartHour.getValue(), npStartMinute.getValue());
        settingsManager.setEndTime(npEndHour.getValue(), npEndMinute.getValue());
        settingsManager.setFadeMinutes(Math.max(1, sbFadeMinutes.getProgress()));
    }
    
    private void updatePreview(int progressPercent) {
        int fadeMinutes = Math.max(1, sbFadeMinutes.getProgress());
        float ratio = (float) progressPercent / 100.0f;
        ratio = Math.min(ratio, 1.0f);
        
        int alpha = (int) (ratio * 255);
        int color = Color.argb(alpha, Color.red(BLOOD_RED), Color.green(BLOOD_RED), Color.blue(BLOOD_RED));
        previewText.setTextColor(color);
        
        tvPreviewProgress.setText("进度：" + progressPercent + "%");
        sbPreviewProgress.setProgress(progressPercent);
    }
    
    private void startAnimation() {
        if (isAnimating) {
            return;
        }
        
        isAnimating = true;
        animationProgress = 0;
        updatePreview(0);
        
        animationRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isAnimating) {
                    return;
                }
                
                animationProgress += 2;
                if (animationProgress > 100) {
                    animationProgress = 100;
                    isAnimating = false;
                    updatePreview(animationProgress);
                    return;
                }
                
                updatePreview(animationProgress);
                animationHandler.postDelayed(this, 50); // 50ms更新一次，约2.5秒播完
            }
        };
        
        animationHandler.postDelayed(animationRunnable, 50);
    }
    
    private void stopAnimation() {
        isAnimating = false;
        if (animationHandler != null && animationRunnable != null) {
            animationHandler.removeCallbacks(animationRunnable);
        }
    }
    
    /**
     * 检查悬浮窗权限
     */
    private boolean checkOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.canDrawOverlays(this);
        }
        return true;
    }
    
    /**
     * 申请悬浮窗权限
     */
    private void requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION);
        }
    }
    
    /**
     * 检查并更新权限状态显示
     */
    private void checkPermissionStatus() {
        if (checkOverlayPermission()) {
            tvPermissionStatus.setText("✅ 悬浮窗权限已开启");
            tvPermissionStatus.setTextColor(Color.parseColor("#4CAF50"));
        } else {
            tvPermissionStatus.setText("⚠️ 点击开启悬浮窗权限");
            tvPermissionStatus.setTextColor(Color.parseColor("#FF9800"));
        }
    }
    
    /**
     * 启动悬浮服务
     */
    private void startFloatingService(String action) {
        Intent intent = new Intent(this, FloatingTextService.class);
        intent.setAction(action);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_OVERLAY_PERMISSION) {
            checkPermissionStatus();
            if (checkOverlayPermission()) {
                Toast.makeText(this, "悬浮窗权限已开启", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopAnimation();
    }
}
