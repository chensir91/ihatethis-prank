package com.ihatethis;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 直接跳转到设置界面
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
        finish();
    }
}
