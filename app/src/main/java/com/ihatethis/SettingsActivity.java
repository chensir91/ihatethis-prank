package com.ihatethis;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.*;
import java.util.Calendar;
import java.util.Random;

public class SettingsActivity extends Activity {

    private SettingsManager sm;
    private LinearLayout root;
    private ScrollView scroll;
    private Random rng = new Random();
    private Handler h = new Handler(Looper.getMainLooper());

    // Time pickers
    private NumberPicker npSH, npSM, npEH, npEM;

    // Font size
    private SeekBar sbMinFS, sbMinFE, sbMaxFS, sbMaxFE;
    private TextView tvMinF, tvMaxF;

    // Color
    private View vColorPreview;
    private SeekBar sbRed, sbGreen, sbBlue;
    private TextView tvColorHex;

    // Text content
    private EditText[] etTexts = new EditText[5];

    // Display duration & typing speed
    private SeekBar sbDisplay, sbTyping;
    private TextView tvDisplay, tvTyping;

    // Frequency
    private SeekBar sbFreqS, sbFreqE;
    private TextView tvFreq;

    // Direction
    private SeekBar sbDirS, sbDirE;
    private TextView tvDir;

    // Shake
    private SeekBar sbShakeS, sbShakeE;
    private TextView tvShake;

    // ====== Preview Screen ======
    private FrameLayout previewScreen;
    private TextView previewText;
    private SeekBar sbSim;
    private TextView tvSimLabel;
    private ValueAnimator shakeAnim;
    private Runnable typeRunnable;
    private String currentSimText = "";
    private int typeIndex = 0;

    // Service / Perm
    private Button btnStart, btnStop, btnPreview;
    private TextView tvPerm;

    private static final int REQ_OVERLAY = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sm = new SettingsManager(this);

        scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.parseColor("#1A1A24"));
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(32, 24, 32, 48);

        buildUI();
        scroll.addView(root);
        setContentView(scroll);
        loadAll();
        checkPerm();
        updatePreviewScreen();
    }

    // ==================== Build UI ====================

    private void buildUI() {
        addSection("时间范围");
        LinearLayout row1 = hRow();
        npSH = makeNP(0, 23); npSM = makeNP(0, 59);
        row1.addView(label("开始:")); row1.addView(npSH); row1.addView(label(":")); row1.addView(npSM);
        root.addView(row1);
        LinearLayout row2 = hRow();
        npEH = makeNP(0, 23); npEM = makeNP(0, 59);
        row2.addView(label("结束:")); row2.addView(npEH); row2.addView(label(":")); row2.addView(npEM);
        root.addView(row2);

        addSection("字体大小 (sp)");
        tvMinF = label(""); sbMinFS = makeSB(8, 120); sbMinFE = makeSB(8, 120);
        root.addView(label("最小字体: 起始→结束")); root.addView(sbMinFS); root.addView(sbMinFE); root.addView(tvMinF);

        tvMaxF = label(""); sbMaxFS = makeSB(8, 200); sbMaxFE = makeSB(8, 200);
        root.addView(label("最大字体: 起始→结束")); root.addView(sbMaxFS); root.addView(sbMaxFE); root.addView(tvMaxF);

        addSection("字体颜色");
        LinearLayout cr = hRow();
        vColorPreview = new View(this);
        vColorPreview.setLayoutParams(new LinearLayout.LayoutParams(64, 64));
        cr.addView(vColorPreview);
        tvColorHex = label("#000000"); tvColorHex.setGravity(Gravity.CENTER_VERTICAL);
        cr.addView(tvColorHex);
        root.addView(cr);
        sbRed = makeSB(0, 255); sbGreen = makeSB(0, 255); sbBlue = makeSB(0, 255);
        root.addView(label("红")); root.addView(sbRed);
        root.addView(label("绿")); root.addView(sbGreen);
        root.addView(label("蓝")); root.addView(sbBlue);

        SeekBar.OnSeekBarChangeListener cl = new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s, int v, boolean u) { updateColorPreview(); if (u) updatePreviewScreen(); }
            public void onStartTrackingTouch(SeekBar s) {}
            public void onStopTrackingTouch(SeekBar s) {}
        };
        sbRed.setOnSeekBarChangeListener(cl); sbGreen.setOnSeekBarChangeListener(cl); sbBlue.setOnSeekBarChangeListener(cl);

        addSection("文本内容 (至多5条)");
        for (int i = 0; i < 5; i++) {
            etTexts[i] = new EditText(this);
            etTexts[i].setHint("文本 " + (i + 1));
            etTexts[i].setSingleLine();
            etTexts[i].setTextColor(Color.WHITE);
            etTexts[i].setHintTextColor(Color.GRAY);
            root.addView(etTexts[i]);
        }

        addSection("显示与速度");
        sbDisplay = makeSB(500, 30000); tvDisplay = label("");
        root.addView(label("每句显示时长 (ms)")); root.addView(sbDisplay); root.addView(tvDisplay);
        sbTyping = makeSB(20, 500); tvTyping = label("");
        root.addView(label("逐字速度 (ms/字)")); root.addView(sbTyping); root.addView(tvTyping);

        addSection("出现频率 (句/分钟)");
        sbFreqS = makeSB(1, 60); sbFreqE = makeSB(1, 60); tvFreq = label("");
        root.addView(label("起始→结束")); root.addView(sbFreqS); root.addView(sbFreqE); root.addView(tvFreq);

        addSection("方向偏移 (度, 0-45)");
        sbDirS = makeSB(0, 45); sbDirE = makeSB(0, 45); tvDir = label("");
        root.addView(label("起始→结束")); root.addView(sbDirS); root.addView(sbDirE); root.addView(tvDir);

        addSection("文字抖动程度");
        sbShakeS = makeSB(0, 40); sbShakeE = makeSB(0, 40); tvShake = label("");
        root.addView(label("起始→结束")); root.addView(sbShakeS); root.addView(sbShakeE); root.addView(tvShake);

        // ====== Simulation Preview Screen ======
        addSection("实时模拟");
        tvSimLabel = label("时间进度: 50%");
        sbSim = makeSB(0, 100); sbSim.setProgress(50);
        root.addView(sbSim); root.addView(tvSimLabel);

        // White background preview screen
        previewScreen = new FrameLayout(this);
        previewScreen.setBackgroundColor(Color.WHITE);
        LinearLayout.LayoutParams psp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(220));
        psp.setMargins(0, 8, 0, 4);
        previewScreen.setLayoutParams(psp);

        previewText = new TextView(this);
        previewText.setText("我好恨");
        previewText.setTextSize(36);
        previewText.setTextColor(Color.rgb(200, 20, 20));
        previewText.setShadowLayer(6, 2, 2, 0x66000000);
        previewText.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams ptp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ptp.gravity = Gravity.CENTER;
        previewText.setLayoutParams(ptp);
        previewScreen.addView(previewText);
        root.addView(previewScreen);

        Button btnSimPlay = btn("模拟播放");
        root.addView(btnSimPlay);

        // Service control
        addSection("服务控制");
        tvPerm = label(""); tvPerm.setTextSize(12); tvPerm.setOnClickListener(v -> reqPerm());
        root.addView(tvPerm);
        LinearLayout btns = hRow();
        btnStart = btn("启动"); btnStop = btn("停止"); btnPreview = btn("预览");
        btns.addView(btnStart); btns.addView(btnStop); btns.addView(btnPreview);
        root.addView(btns);
        Button btnSave = btn("保存设置"); Button btnReset = btn("恢复默认");
        root.addView(btnSave); root.addView(btnReset);

        // ====== Listeners ======
        SeekBar.OnSeekBarChangeListener generalL = new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s, int v, boolean u) { refreshLabels(); if (u) updatePreviewScreen(); }
            public void onStartTrackingTouch(SeekBar s) {}
            public void onStopTrackingTouch(SeekBar s) {}
        };
        sbMinFS.setOnSeekBarChangeListener(generalL); sbMinFE.setOnSeekBarChangeListener(generalL);
        sbMaxFS.setOnSeekBarChangeListener(generalL); sbMaxFE.setOnSeekBarChangeListener(generalL);
        sbDisplay.setOnSeekBarChangeListener(generalL); sbTyping.setOnSeekBarChangeListener(generalL);
        sbFreqS.setOnSeekBarChangeListener(generalL); sbFreqE.setOnSeekBarChangeListener(generalL);
        sbDirS.setOnSeekBarChangeListener(generalL); sbDirE.setOnSeekBarChangeListener(generalL);
        sbShakeS.setOnSeekBarChangeListener(generalL); sbShakeE.setOnSeekBarChangeListener(generalL);

        sbSim.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s, int v, boolean u) {
                tvSimLabel.setText("时间进度: " + v + "%");
                if (u) updatePreviewScreen();
            }
            public void onStartTrackingTouch(SeekBar s) {}
            public void onStopTrackingTouch(SeekBar s) {}
        });

        btnSimPlay.setOnClickListener(v -> simulateAnimation());

        btnStart.setOnClickListener(v -> { saveAll(); startSvc("START"); toast("服务已启动"); });
        btnStop.setOnClickListener(v -> { startSvc("STOP"); toast("服务已停止"); });
        btnPreview.setOnClickListener(v -> {
            saveAll();
            if (!checkPermQuick()) { reqPerm(); return; }
            startSvc("PREVIEW");
            toast("预览已触发（需有悬浮窗权限）");
        });
        btnSave.setOnClickListener(v -> { saveAll(); toast("已保存"); });
        btnReset.setOnClickListener(v -> { sm.resetToDefault(); loadAll(); updatePreviewScreen(); toast("已恢复默认"); });
    }

    // ==================== Preview Screen ====================

    private void updatePreviewScreen() {
        float p = sbSim.getProgress() / 100f;
        int minF = sm.getInterpolatedMinFont(p);
        int maxF = sm.getInterpolatedMaxFont(p);
        int fs = minF + (maxF - minF) / 2;
        int color = sm.getFontColor();
        float dir = sm.getInterpolatedDirection(p);
        float shake = sm.getInterpolatedShake(p);

        // Pick random text
        String[] pool = sm.getTexts();
        java.util.List<String> valid = new java.util.ArrayList<>();
        for (String s : pool) if (s != null && !s.isEmpty()) valid.add(s);
        String text = valid.isEmpty() ? "我好恨" : valid.get(rng.nextInt(valid.size()));

        previewText.setText(text);
        previewText.setTextSize(fs);
        previewText.setTextColor(color);
        previewText.setRotation(rng.nextBoolean() ? dir : -dir);

        // Start/update shake
        startSimShake(shake);

        // Save current sim text for animation
        currentSimText = text;
    }

    private void startSimShake(float intensity) {
        if (shakeAnim != null) shakeAnim.cancel();
        if (intensity < 0.5f) return;

        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) previewText.getLayoutParams();
        lp.gravity = Gravity.CENTER;
        final int cx = lp.leftMargin, cy = lp.topMargin;

        shakeAnim = ValueAnimator.ofFloat(0f, 1f);
        shakeAnim.setDuration(60);
        shakeAnim.setRepeatCount(ValueAnimator.INFINITE);
        shakeAnim.setInterpolator(new LinearInterpolator());
        shakeAnim.addUpdateListener(anim -> {
            float dx = (rng.nextFloat() - 0.5f) * 2 * intensity;
            float dy = (rng.nextFloat() - 0.5f) * 2 * intensity;
            FrameLayout.LayoutParams l = (FrameLayout.LayoutParams) previewText.getLayoutParams();
            l.leftMargin = (int) dx;
            l.topMargin = (int) dy;
            l.gravity = Gravity.CENTER;
            previewText.setLayoutParams(l);
        });
        shakeAnim.start();
    }

    private void simulateAnimation() {
        if (typeRunnable != null) h.removeCallbacks(typeRunnable);
        updatePreviewScreen();
        typeIndex = 0;
        previewText.setText("");

        int speed = getSBVal(sbTyping);
        typeRunnable = new Runnable() {
            @Override
            public void run() {
                if (typeIndex < currentSimText.length()) {
                    previewText.setText(currentSimText.substring(0, typeIndex + 1));
                    typeIndex++;
                    h.postDelayed(this, speed);
                } else {
                    // Fade out after display duration
                    int dur = getSBVal(sbDisplay);
                    h.postDelayed(() -> {
                        previewText.animate().alpha(0.3f).setDuration(400).withEndAction(() -> {
                            previewText.setAlpha(1f);
                            previewText.setText(currentSimText);
                        }).start();
                    }, dur);
                }
            }
        };
        h.post(typeRunnable);
    }

    // ==================== Helpers ====================

    private void addSection(String title) {
        TextView tv = new TextView(this);
        tv.setText(title);
        tv.setTextSize(16);
        tv.setTextColor(Color.parseColor("#FFC107"));
        tv.setPadding(0, 28, 0, 8);
        root.addView(tv);
    }

    private TextView label(String txt) {
        TextView tv = new TextView(this);
        tv.setText(txt); tv.setTextSize(12); tv.setTextColor(Color.parseColor("#AAAAAA"));
        tv.setPadding(0, 4, 0, 2);
        return tv;
    }

    private Button btn(String txt) {
        Button b = new Button(this);
        b.setText(txt); b.setTextSize(12);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(4, 4, 4, 4);
        b.setLayoutParams(lp);
        return b;
    }

    private LinearLayout hRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        return row;
    }

    private NumberPicker makeNP(int min, int max) {
        NumberPicker np = new NumberPicker(this);
        np.setMinValue(min); np.setMaxValue(max); np.setFormatter(v -> String.format("%02d", v));
        np.setLayoutParams(new LinearLayout.LayoutParams(120, ViewGroup.LayoutParams.WRAP_CONTENT));
        return np;
    }

    private SeekBar makeSB(int min, int max) {
        SeekBar sb = new SeekBar(this);
        sb.setMax(max - min);
        sb.setTag(min);
        return sb;
    }

    private int getSBVal(SeekBar sb) { return ((int)sb.getTag()) + sb.getProgress(); }
    private void setSBVal(SeekBar sb, int val) {
        int min = (int)sb.getTag();
        sb.setProgress(Math.max(0, Math.min(sb.getMax(), val - min)));
    }

    private int dpToPx(int dp) {
        return (int)(dp * getResources().getDisplayMetrics().density);
    }

    // ==================== Load / Save ====================

    private void loadAll() {
        npSH.setValue(sm.getStartHour()); npSM.setValue(sm.getStartMinute());
        npEH.setValue(sm.getEndHour()); npEM.setValue(sm.getEndMinute());
        setSBVal(sbMinFS, sm.getMinFontStart()); setSBVal(sbMinFE, sm.getMinFontEnd());
        setSBVal(sbMaxFS, sm.getMaxFontStart()); setSBVal(sbMaxFE, sm.getMaxFontEnd());
        int c = sm.getFontColor();
        setSBVal(sbRed, Color.red(c)); setSBVal(sbGreen, Color.green(c)); setSBVal(sbBlue, Color.blue(c));
        String[] texts = sm.getTexts();
        for (int i = 0; i < 5; i++) etTexts[i].setText(texts[i]);
        setSBVal(sbDisplay, sm.getDisplayDurationMs());
        setSBVal(sbTyping, sm.getTypingSpeedMs());
        setSBVal(sbFreqS, sm.getFreqStart()); setSBVal(sbFreqE, sm.getFreqEnd());
        setSBVal(sbDirS, sm.getDirStart()); setSBVal(sbDirE, sm.getDirEnd());
        setSBVal(sbShakeS, sm.getShakeStart()); setSBVal(sbShakeE, sm.getShakeEnd());
        refreshLabels(); updateColorPreview();
    }

    private void saveAll() {
        sm.setStartTime(npSH.getValue(), npSM.getValue());
        sm.setEndTime(npEH.getValue(), npEM.getValue());
        sm.setMinFont(getSBVal(sbMinFS), getSBVal(sbMinFE));
        sm.setMaxFont(getSBVal(sbMaxFS), getSBVal(sbMaxFE));
        int color = Color.rgb(getSBVal(sbRed), getSBVal(sbGreen), getSBVal(sbBlue));
        sm.setFontColor(color);
        String[] texts = new String[5];
        for (int i = 0; i < 5; i++) texts[i] = etTexts[i].getText().toString().trim();
        sm.setTexts(texts);
        sm.setDisplayDurationMs(getSBVal(sbDisplay));
        sm.setTypingSpeedMs(getSBVal(sbTyping));
        sm.setFrequency(getSBVal(sbFreqS), getSBVal(sbFreqE));
        sm.setDirection(getSBVal(sbDirS), getSBVal(sbDirE));
        sm.setShake(getSBVal(sbShakeS), getSBVal(sbShakeE));
    }

    private void refreshLabels() {
        tvMinF.setText(String.format("最小: %dsp → %dsp", getSBVal(sbMinFS), getSBVal(sbMinFE)));
        tvMaxF.setText(String.format("最大: %dsp → %dsp", getSBVal(sbMaxFS), getSBVal(sbMaxFE)));
        tvDisplay.setText(String.format("显示: %.1f秒", getSBVal(sbDisplay) / 1000f));
        tvTyping.setText(String.format("速度: %dms/字", getSBVal(sbTyping)));
        tvFreq.setText(String.format("频率: %d → %d 句/分", getSBVal(sbFreqS), getSBVal(sbFreqE)));
        tvDir.setText(String.format("偏移: %d° → %d°", getSBVal(sbDirS), getSBVal(sbDirE)));
        tvShake.setText(String.format("抖动: %d → %d", getSBVal(sbShakeS), getSBVal(sbShakeE)));
    }

    private void updateColorPreview() {
        int c = Color.rgb(getSBVal(sbRed), getSBVal(sbGreen), getSBVal(sbBlue));
        vColorPreview.setBackgroundColor(c);
        tvColorHex.setText(String.format("#%02X%02X%02X", getSBVal(sbRed), getSBVal(sbGreen), getSBVal(sbBlue)));
    }

    // ==================== Service / Permission ====================

    private void startSvc(String action) {
        if (!checkPermQuick() && ("START".equals(action) || "PREVIEW".equals(action))) {
            reqPerm(); return;
        }
        Intent i = new Intent(this, FloatingTextService.class);
        i.setAction(action);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(i);
        } else {
            startService(i);
        }
    }

    private boolean checkPermQuick() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            return Settings.canDrawOverlays(this);
        return true;
    }

    private void checkPerm() {
        if (checkPermQuick()) {
            tvPerm.setText("悬浮窗权限: 已开启"); tvPerm.setTextColor(Color.parseColor("#4CAF50"));
        } else {
            tvPerm.setText("悬浮窗权限: 未开启 (点击设置)"); tvPerm.setTextColor(Color.parseColor("#FF9800"));
        }
    }

    private void reqPerm() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            startActivityForResult(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName())), REQ_OVERLAY);
    }

    @Override
    protected void onActivityResult(int rc, int result, Intent data) {
        super.onActivityResult(rc, result, data);
        if (rc == REQ_OVERLAY) checkPerm();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (shakeAnim != null) shakeAnim.cancel();
        if (typeRunnable != null) h.removeCallbacks(typeRunnable);
    }

    private void toast(String msg) { Toast.makeText(this, msg, Toast.LENGTH_SHORT).show(); }
}
