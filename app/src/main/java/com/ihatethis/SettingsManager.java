package com.ihatethis;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;

public class SettingsManager {
    private static final String PREFS_NAME = "ihatethis_settings_v4";

    // Time defaults (00:00 - 04:44)
    private static final int DEF_START_HOUR = 0, DEF_START_MIN = 0;
    private static final int DEF_END_HOUR = 4, DEF_END_MIN = 44;

    // Font size: min goes from 24→48sp, max goes from 48→96sp
    private static final int DEF_MIN_FONT_S = 24, DEF_MIN_FONT_E = 48;
    private static final int DEF_MAX_FONT_S = 48, DEF_MAX_FONT_E = 96;

    // Color
    private static final int DEF_COLOR = Color.rgb(200, 20, 20);

    // Text content (up to 5)
    private static final String[] DEF_TEXTS = {
        "我好恨", "我恨这一切", "为什么", "救救我", "我不想这样"
    };

    // Display duration (ms)
    private static final int DEF_DISPLAY_MS = 4000;

    // Typing speed (ms per char)
    private static final int DEF_TYPING_MS = 120;

    // Frequency (sentences/min): start 2→end 8
    private static final int DEF_FREQ_S = 2, DEF_FREQ_E = 8;

    // Direction offset (degrees): start 0→end 30
    private static final int DEF_DIR_S = 0, DEF_DIR_E = 30;

    // Shake intensity: start 0→end 12
    private static final int DEF_SHAKE_S = 0, DEF_SHAKE_E = 12;

    private SharedPreferences prefs;

    public SettingsManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    // === Time ===
    public int getStartHour() { return prefs.getInt("start_h", DEF_START_HOUR); }
    public int getStartMinute() { return prefs.getInt("start_m", DEF_START_MIN); }
    public int getEndHour() { return prefs.getInt("end_h", DEF_END_HOUR); }
    public int getEndMinute() { return prefs.getInt("end_m", DEF_END_MIN); }
    public int getStartTotalMinutes() { return getStartHour() * 60 + getStartMinute(); }
    public int getEndTotalMinutes() { return getEndHour() * 60 + getEndMinute(); }

    public void setStartTime(int h, int m) { prefs.edit().putInt("start_h", h).putInt("start_m", m).apply(); }
    public void setEndTime(int h, int m) { prefs.edit().putInt("end_h", h).putInt("end_m", m).apply(); }

    // === Font Size ===
    public int getMinFontStart() { return prefs.getInt("minf_s", DEF_MIN_FONT_S); }
    public int getMinFontEnd()   { return prefs.getInt("minf_e", DEF_MIN_FONT_E); }
    public int getMaxFontStart() { return prefs.getInt("maxf_s", DEF_MAX_FONT_S); }
    public int getMaxFontEnd()   { return prefs.getInt("maxf_e", DEF_MAX_FONT_E); }

    public void setMinFont(int start, int end) { prefs.edit().putInt("minf_s", start).putInt("minf_e", end).apply(); }
    public void setMaxFont(int start, int end) { prefs.edit().putInt("maxf_s", start).putInt("maxf_e", end).apply(); }

    // === Color ===
    public int getFontColor() { return prefs.getInt("color", DEF_COLOR); }
    public void setFontColor(int color) { prefs.edit().putInt("color", color).apply(); }

    // === Text Content ===
    public String[] getTexts() {
        String[] texts = new String[5];
        for (int i = 0; i < 5; i++) {
            texts[i] = prefs.getString("text_" + i, i < DEF_TEXTS.length ? DEF_TEXTS[i] : "");
        }
        return texts;
    }
    public void setTexts(String[] texts) {
        SharedPreferences.Editor ed = prefs.edit();
        for (int i = 0; i < 5; i++) {
            ed.putString("text_" + i, i < texts.length ? texts[i] : "");
        }
        ed.apply();
    }

    // === Display Duration ===
    public int getDisplayDurationMs() { return prefs.getInt("disp_ms", DEF_DISPLAY_MS); }
    public void setDisplayDurationMs(int ms) { prefs.edit().putInt("disp_ms", ms).apply(); }

    // === Typing Speed ===
    public int getTypingSpeedMs() { return prefs.getInt("type_ms", DEF_TYPING_MS); }
    public void setTypingSpeedMs(int ms) { prefs.edit().putInt("type_ms", ms).apply(); }

    // === Frequency ===
    public int getFreqStart() { return prefs.getInt("freq_s", DEF_FREQ_S); }
    public int getFreqEnd()   { return prefs.getInt("freq_e", DEF_FREQ_E); }
    public void setFrequency(int start, int end) { prefs.edit().putInt("freq_s", start).putInt("freq_e", end).apply(); }

    // === Direction Offset ===
    public int getDirStart() { return prefs.getInt("dir_s", DEF_DIR_S); }
    public int getDirEnd()   { return prefs.getInt("dir_e", DEF_DIR_E); }
    public void setDirection(int start, int end) { prefs.edit().putInt("dir_s", start).putInt("dir_e", end).apply(); }

    // === Shake ===
    public int getShakeStart() { return prefs.getInt("shake_s", DEF_SHAKE_S); }
    public int getShakeEnd()   { return prefs.getInt("shake_e", DEF_SHAKE_E); }
    public void setShake(int start, int end) { prefs.edit().putInt("shake_s", start).putInt("shake_e", end).apply(); }

    // === Interpolation helper ===
    public float getTimeProgress(int currentTotalMinutes) {
        int start = getStartTotalMinutes();
        int end = getEndTotalMinutes();
        if (start <= end) {
            if (currentTotalMinutes < start || currentTotalMinutes > end) return -1;
            return (float)(currentTotalMinutes - start) / (end - start);
        } else {
            // Cross midnight
            int total = (24 * 60 - start) + end;
            int elapsed;
            if (currentTotalMinutes >= start) {
                elapsed = currentTotalMinutes - start;
            } else {
                elapsed = (24 * 60 - start) + currentTotalMinutes;
            }
            return (float)elapsed / total;
        }
    }

    // Interpolated getters at a given progress (0.0 - 1.0)
    public int getInterpolatedMinFont(float p) { return lerp(getMinFontStart(), getMinFontEnd(), p); }
    public int getInterpolatedMaxFont(float p) { return lerp(getMaxFontStart(), getMaxFontEnd(), p); }
    public int getInterpolatedFrequency(float p) { return lerp(getFreqStart(), getFreqEnd(), p); }
    public float getInterpolatedDirection(float p) { return lerpF(getDirStart(), getDirEnd(), p); }
    public float getInterpolatedShake(float p) { return lerpF(getShakeStart(), getShakeEnd(), p); }

    private int lerp(int a, int b, float t) { return Math.round(a + (b - a) * Math.max(0, Math.min(1, t))); }
    private float lerpF(float a, float b, float t) { return a + (b - a) * Math.max(0, Math.min(1, t)); }

    public void resetToDefault() { prefs.edit().clear().apply(); }
}
