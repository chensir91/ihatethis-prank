package com.ihatethis;

import android.content.Context;
import android.content.SharedPreferences;

public class SettingsManager {
    private static final String PREFS_NAME = "ihatethis_settings";
    
    // 默认值
    private static final int DEFAULT_START_HOUR = 0;
    private static final int DEFAULT_START_MINUTE = 0;
    private static final int DEFAULT_END_HOUR = 4;
    private static final int DEFAULT_END_MINUTE = 44;
    private static final int DEFAULT_FADE_MINUTES = 240; // 4小时
    
    private SharedPreferences prefs;
    
    public SettingsManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
    
    // 开始时间
    public int getStartHour() {
        return prefs.getInt("start_hour", DEFAULT_START_HOUR);
    }
    
    public int getStartMinute() {
        return prefs.getInt("start_minute", DEFAULT_START_MINUTE);
    }
    
    public void setStartTime(int hour, int minute) {
        prefs.edit()
            .putInt("start_hour", hour)
            .putInt("start_minute", minute)
            .apply();
    }
    
    // 结束时间
    public int getEndHour() {
        return prefs.getInt("end_hour", DEFAULT_END_HOUR);
    }
    
    public int getEndMinute() {
        return prefs.getInt("end_minute", DEFAULT_END_MINUTE);
    }
    
    public void setEndTime(int hour, int minute) {
        prefs.edit()
            .putInt("end_hour", hour)
            .putInt("end_minute", minute)
            .apply();
    }
    
    // 渐变时长（分钟）
    public int getFadeMinutes() {
        return prefs.getInt("fade_minutes", DEFAULT_FADE_MINUTES);
    }
    
    public void setFadeMinutes(int minutes) {
        prefs.edit()
            .putInt("fade_minutes", minutes)
            .apply();
    }
    
    // 获取开始时间的总分钟数
    public int getStartTotalMinutes() {
        return getStartHour() * 60 + getStartMinute();
    }
    
    // 获取结束时间的总分钟数
    public int getEndTotalMinutes() {
        return getEndHour() * 60 + getEndMinute();
    }
    
    // 重置为默认设置
    public void resetToDefault() {
        prefs.edit().clear().apply();
    }
}
