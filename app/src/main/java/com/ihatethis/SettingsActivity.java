package com.ihatethis;

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
import android.widget.*;
import java.util.Random;

public class SettingsActivity extends Activity {

    private SettingsManager sm;
    private LinearLayout root;
    private Random rng = new Random();
    private Handler h = new Handler(Looper.getMainLooper());

    private NumberPicker npSH, npSM, npEH, npEM;

    private SeekBar sbMinFS, sbMinFE, sbMaxFS, sbMaxFE;
    private TextView tvMinF, tvMaxF;

    private View vColorPreview;
    private SeekBar sbRed, sbGreen, sbBlue;
    private TextView tvColorHex;

    private EditText[] etTexts = new EditText[5];

    private SeekBar sbDisplay, sbTyping;
    private TextView tvDisplay, tvTyping;

    private SeekBar sbFreqS, sbFreqE;
    private TextView tvFreq;

    private SeekBar sbDirS, sbDirE;
    private TextView tvDir;

    private SeekBar sbShakeS, sbShakeE;
    private TextView tvShake;

    // Preview
    private FrameLayout previewScreen;
    private TextView previewText;
    private SeekBar sbSim;
    private TextView tvSimLabel;
    private Runnable typeRunnable;
    private String currentSimText = "";
    private int typeIndex = 0;

    private Button btnStart, btnStop, btnPreview;
    private TextView tvPerm;
    private static final int REQ_OVERLAY = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sm = new SettingsManager(this);

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(12), dp(12), dp(12), dp(24));

        buildUI();

        ScrollView sc = new ScrollView(this);
        sc.setBackgroundColor(Color.parseColor("#1A1A24"));
        sc.addView(root);
        setContentView(sc);

        loadAll();
        checkPerm();
        updatePreviewScreen();
    }

    private void buildUI() {
        addSection("时间范围");
        root.addView(hRow(lab("开始"), npSH = np(0, 23), lab("时"), npSM = np(0, 59), lab("分")));
        root.addView(hRow(lab("结束"), npEH = np(0, 23), lab("时"), npEM = np(0, 59), lab("分")));

        addSection("字体大小 (sp)");
        tvMinF = lab(""); sbMinFS = sb(8, 120); sbMinFE = sb(8, 120);
        root.addView(lab("最小字体 起始→结束")); root.addView(sbMinFS); root.addView(sbMinFE); root.addView(tvMinF);
        tvMaxF = lab(""); sbMaxFS = sb(8, 200); sbMaxFE = sb(8, 200);
        root.addView(lab("最大字体 起始→结束")); root.addView(sbMaxFS); root.addView(sbMaxFE); root.addView(tvMaxF);

        addSection("字体颜色");
        LinearLayout cr = hRow();
        vColorPreview = new View(this);
        cr.addView(vColorPreview, new LinearLayout.LayoutParams(dp(32), dp(32)));
        tvColorHex = lab("#000000"); tvColorHex.setGravity(Gravity.CENTER_VERTICAL); cr.addView(tvColorHex);
        root.addView(cr);
        sbRed = sb(0, 255); sbGreen = sb(0, 255); sbBlue = sb(0, 255);
        root.addView(lab("R")); root.addView(sbRed);
        root.addView(lab("G")); root.addView(sbGreen);
        root.addView(lab("B")); root.addView(sbBlue);
        SeekBar.OnSeekBarChangeListener cl = new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s, int v, boolean u) { updateColorPreview(); if (u) updatePreviewScreen(); }
            public void onStartTrackingTouch(SeekBar s) {}
            public void onStopTrackingTouch(SeekBar s) {}
        };
        sbRed.setOnSeekBarChangeListener(cl); sbGreen.setOnSeekBarChangeListener(cl); sbBlue.setOnSeekBarChangeListener(cl);

        addSection("文本内容 (至多5条)");
        for (int i = 0; i < 5; i++) {
            etTexts[i] = new EditText(this);
            etTexts[i].setHint("文本" + (i + 1)); etTexts[i].setSingleLine();
            etTexts[i].setTextColor(Color.WHITE); etTexts[i].setHintTextColor(Color.GRAY);
            root.addView(etTexts[i]);
        }

        addSection("显示与速度");
        sbDisplay = sb(500, 30000); tvDisplay = lab("");
        root.addView(lab("显示时长(ms)")); root.addView(sbDisplay); root.addView(tvDisplay);
        sbTyping = sb(20, 500); tvTyping = lab("");
        root.addView(lab("逐字速度(ms)")); root.addView(sbTyping); root.addView(tvTyping);

        addSection("出现频率 (句/分钟)");
        sbFreqS = sb(1, 60); sbFreqE = sb(1, 60); tvFreq = lab("");
        root.addView(lab("起始→结束")); root.addView(sbFreqS); root.addView(sbFreqE); root.addView(tvFreq);

        addSection("方向偏移 (0-45°)");
        sbDirS = sb(0, 45); sbDirE = sb(0, 45); tvDir = lab("");
        root.addView(lab("起始→结束")); root.addView(sbDirS); root.addView(sbDirE); root.addView(tvDir);

        addSection("文字抖动 (0-40px)");
        sbShakeS = sb(0, 40); sbShakeE = sb(0, 40); tvShake = lab("");
        root.addView(lab("起始→结束")); root.addView(sbShakeS); root.addView(sbShakeE); root.addView(tvShake);

        // ====== Preview ======
        addSection("实时模拟");
        tvSimLabel = lab("进度: 50%"); sbSim = sb(0, 100); sbSim.setProgress(50);
        root.addView(sbSim); root.addView(tvSimLabel);

        previewScreen = new FrameLayout(this);
        previewScreen.setBackgroundColor(Color.WHITE);
        LinearLayout.LayoutParams psp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(180));
        psp.setMargins(0, dp(4), 0, dp(2));
        previewScreen.setLayoutParams(psp);

        previewText = new TextView(this);
        previewText.setText("我好恨");
        previewText.setTextSize(36);
        previewText.setTextColor(Color.rgb(200, 20, 20));
        previewText.setShadowLayer(4, 2, 2, 0x66000000);
        FrameLayout.LayoutParams ptp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ptp.gravity = Gravity.CENTER;
        previewText.setLayoutParams(ptp);
        previewScreen.addView(previewText);
        root.addView(previewScreen);

        Button btnSim = btn("播放模拟"); root.addView(btnSim);

        addSection("服务控制");
        tvPerm = lab(""); tvPerm.setOnClickListener(v -> reqPerm()); root.addView(tvPerm);
        LinearLayout btns = hRow();
        btnStart = btn("启动"); btnStop = btn("停止"); btnPreview = btn("预览");
        btns.addView(btnStart); btns.addView(btnStop); btns.addView(btnPreview);
        root.addView(btns);
        Button saveBtn = btn("保存"); Button resetBtn = btn("默认");
root.addView(hRow(saveBtn, resetBtn));

        // Listeners
        SeekBar.OnSeekBarChangeListener gL = new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s, int v, boolean u) { refreshLabels(); if (u) updatePreviewScreen(); }
            public void onStartTrackingTouch(SeekBar s) {}
            public void onStopTrackingTouch(SeekBar s) {}
        };
        sbMinFS.setOnSeekBarChangeListener(gL); sbMinFE.setOnSeekBarChangeListener(gL);
        sbMaxFS.setOnSeekBarChangeListener(gL); sbMaxFE.setOnSeekBarChangeListener(gL);
        sbDisplay.setOnSeekBarChangeListener(gL); sbTyping.setOnSeekBarChangeListener(gL);
        sbFreqS.setOnSeekBarChangeListener(gL); sbFreqE.setOnSeekBarChangeListener(gL);
        sbDirS.setOnSeekBarChangeListener(gL); sbDirE.setOnSeekBarChangeListener(gL);
        sbShakeS.setOnSeekBarChangeListener(gL); sbShakeE.setOnSeekBarChangeListener(gL);
        sbSim.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s, int v, boolean u) {
                tvSimLabel.setText("进度: " + v + "%");
                if (u) updatePreviewScreen();
            }
            public void onStartTrackingTouch(SeekBar s) {}
            public void onStopTrackingTouch(SeekBar s) {}
        });

        btnSim.setOnClickListener(v -> simAnimate());
        btnStart.setOnClickListener(v -> { saveAll(); startSvc("START"); t("已启动"); });
        btnStop.setOnClickListener(v -> { startSvc("STOP"); t("已停止"); });
        btnPreview.setOnClickListener(v -> {
            saveAll(); if (!permOk()) { reqPerm(); return; }
            startSvc("PREVIEW"); t("预览已触发");
        });
        saveBtn.setOnClickListener(v -> { saveAll(); t("已保存"); });
        resetBtn.setOnClickListener(v -> { sm.resetToDefault(); loadAll(); updatePreviewScreen(); t("已恢复默认"); });
    }

    // ====== Preview ======

    private void updatePreviewScreen() {
        float p = sbSim.getProgress() / 100f;
        int minF = sm.getInterpolatedMinFont(p);
        int maxF = sm.getInterpolatedMaxFont(p);
        int fs = minF + (maxF - minF) / 2;
        int color = sm.getFontColor();
        float dir = sm.getInterpolatedDirection(p);

        String[] pool = sm.getTexts();
        java.util.List<String> valid = new java.util.ArrayList<>();
        for (String s : pool) if (s != null && !s.isEmpty()) valid.add(s);
        String text = valid.isEmpty() ? "我好恨" : valid.get(rng.nextInt(valid.size()));

        previewText.setText(text);
        previewText.setTextSize(fs);
        previewText.setTextColor(color);
        previewText.setRotation(rng.nextBoolean() ? dir : -dir);
        currentSimText = text;
    }

    private void simAnimate() {
        if (typeRunnable != null) h.removeCallbacks(typeRunnable);
        updatePreviewScreen();
        typeIndex = 0;
        previewText.setText("");
        final int speed = v(sbTyping);
        typeRunnable = new Runnable() {
            public void run() {
                if (typeIndex < currentSimText.length()) {
                    previewText.setText(currentSimText.substring(0, typeIndex + 1));
                    typeIndex++;
                    h.postDelayed(this, speed);
                } else {
                    h.postDelayed(() -> {
                        previewText.animate().alpha(0.2f).setDuration(500).withEndAction(() -> {
                            previewText.setAlpha(1f);
                            previewText.setText(currentSimText);
                        }).start();
                    }, v(sbDisplay));
                }
            }
        };
        h.post(typeRunnable);
    }

    // ====== Helpers ======

    private void addSection(String t) {
        TextView tv = new TextView(this);
        tv.setText(t); tv.setTextSize(15); tv.setTextColor(Color.parseColor("#FFC107"));
        tv.setPadding(0, dp(12), 0, dp(4)); root.addView(tv);
    }

    private TextView lab(String t) {
        TextView tv = new TextView(this);
        tv.setText(t); tv.setTextSize(12); tv.setTextColor(Color.parseColor("#AAAAAA"));
        tv.setPadding(0, dp(2), 0, 0); return tv;
    }

    private Button btn(String t) {
        Button b = new Button(this); b.setText(t); b.setTextSize(12);
        b.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT)); return b;
    }

    private LinearLayout hRow(View... vs) {
        LinearLayout r = new LinearLayout(this); r.setOrientation(LinearLayout.HORIZONTAL);
        for (View v : vs) r.addView(v); return r;
    }

    private NumberPicker np(int min, int max) {
        NumberPicker n = new NumberPicker(this);
        n.setMinValue(min); n.setMaxValue(max);
        n.setFormatter(new NumberPicker.Formatter() {
            public String format(int v) { return String.format("%02d", v); }
        });
        n.setLayoutParams(new LinearLayout.LayoutParams(dp(52), ViewGroup.LayoutParams.WRAP_CONTENT));
        return n;
    }

    private SeekBar sb(int min, int max) {
        SeekBar s = new SeekBar(this);
        s.setMax(max - min); s.setTag(min); return s;
    }

    private int v(SeekBar s) { return (int) s.getTag() + s.getProgress(); }
    private void sv(SeekBar s, int val) { s.setProgress(Math.max(0, Math.min(s.getMax(), val - (int)s.getTag()))); }
    private int dp(int d) { return (int)(d * getResources().getDisplayMetrics().density); }

    private void refreshLabels() {
        tvMinF.setText(String.format("%dsp → %dsp", v(sbMinFS), v(sbMinFE)));
        tvMaxF.setText(String.format("%dsp → %dsp", v(sbMaxFS), v(sbMaxFE)));
        tvDisplay.setText(String.format("%.1f秒", v(sbDisplay) / 1000f));
        tvTyping.setText(String.format("%dms", v(sbTyping)));
        tvFreq.setText(String.format("%d → %d /分", v(sbFreqS), v(sbFreqE)));
        tvDir.setText(String.format("%d° → %d°", v(sbDirS), v(sbDirE)));
        tvShake.setText(String.format("%d → %d px", v(sbShakeS), v(sbShakeE)));
    }

    private void updateColorPreview() {
        int c = Color.rgb(v(sbRed), v(sbGreen), v(sbBlue));
        vColorPreview.setBackgroundColor(c);
        tvColorHex.setText(String.format("#%02X%02X%02X", v(sbRed), v(sbGreen), v(sbBlue)));
    }

    // ====== Load / Save ======

    private void loadAll() {
        npSH.setValue(sm.getStartHour()); npSM.setValue(sm.getStartMinute());
        npEH.setValue(sm.getEndHour()); npEM.setValue(sm.getEndMinute());
        sv(sbMinFS, sm.getMinFontStart()); sv(sbMinFE, sm.getMinFontEnd());
        sv(sbMaxFS, sm.getMaxFontStart()); sv(sbMaxFE, sm.getMaxFontEnd());
        int c = sm.getFontColor();
        sv(sbRed, Color.red(c)); sv(sbGreen, Color.green(c)); sv(sbBlue, Color.blue(c));
        String[] t = sm.getTexts();
        for (int i = 0; i < 5; i++) etTexts[i].setText(t[i]);
        sv(sbDisplay, sm.getDisplayDurationMs()); sv(sbTyping, sm.getTypingSpeedMs());
        sv(sbFreqS, sm.getFreqStart()); sv(sbFreqE, sm.getFreqEnd());
        sv(sbDirS, sm.getDirStart()); sv(sbDirE, sm.getDirEnd());
        sv(sbShakeS, sm.getShakeStart()); sv(sbShakeE, sm.getShakeEnd());
        refreshLabels(); updateColorPreview();
    }

    private void saveAll() {
        sm.setStartTime(npSH.getValue(), npSM.getValue()); sm.setEndTime(npEH.getValue(), npEM.getValue());
        sm.setMinFont(v(sbMinFS), v(sbMinFE)); sm.setMaxFont(v(sbMaxFS), v(sbMaxFE));
        sm.setFontColor(Color.rgb(v(sbRed), v(sbGreen), v(sbBlue)));
        String[] t = new String[5];
        for (int i = 0; i < 5; i++) t[i] = etTexts[i].getText().toString().trim();
        sm.setTexts(t);
        sm.setDisplayDurationMs(v(sbDisplay)); sm.setTypingSpeedMs(v(sbTyping));
        sm.setFrequency(v(sbFreqS), v(sbFreqE));
        sm.setDirection(v(sbDirS), v(sbDirE)); sm.setShake(v(sbShakeS), v(sbShakeE));
    }

    // ====== Service ======

    private void startSvc(String action) {
        if (!permOk() && ("START".equals(action) || "PREVIEW".equals(action))) { reqPerm(); return; }
        Intent i = new Intent(this, FloatingTextService.class);
        i.setAction(action);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(i);
        else startService(i);
    }

    private boolean permOk() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this);
    }

    private void checkPerm() {
        if (permOk()) { tvPerm.setText("悬浮窗: OK"); tvPerm.setTextColor(Color.parseColor("#4CAF50")); }
        else { tvPerm.setText("悬浮窗: 需授权"); tvPerm.setTextColor(Color.parseColor("#FF9800")); }
    }

    private void reqPerm() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            startActivityForResult(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName())), REQ_OVERLAY);
    }

    @Override
    protected void onActivityResult(int rc, int res, Intent d) {
        super.onActivityResult(rc, res, d);
        if (rc == REQ_OVERLAY) checkPerm();
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        if (typeRunnable != null) h.removeCallbacks(typeRunnable);
    }

    private void t(String m) { Toast.makeText(this, m, Toast.LENGTH_SHORT).show(); }
}
