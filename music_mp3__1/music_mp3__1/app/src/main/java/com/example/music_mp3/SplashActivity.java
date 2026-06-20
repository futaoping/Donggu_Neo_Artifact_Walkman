package com.example.music_mp3;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    // 开屏停留时间（毫秒），这里设置为 2500 毫秒（2.5秒）
    private static final long SPLASH_DELAY = 2500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // 隐藏 ActionBar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        ImageView ivSplash = findViewById(R.id.iv_splash);
        TextView tvSplashText = findViewById(R.id.tv_splash_text);

        // 添加一个简单的淡入动画，让开屏不那么生硬
        AlphaAnimation fadeIn = new AlphaAnimation(0.0f, 1.0f);
        fadeIn.setDuration(1000); // 动画持续 1 秒
        ivSplash.startAnimation(fadeIn);
        tvSplashText.startAnimation(fadeIn);

        // 延迟跳转到 MainActivity
        new Handler().postDelayed(() -> {
            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
            startActivity(intent);
            finish(); // 关闭开屏页，防止用户按返回键回到这里
        }, SPLASH_DELAY);
    }
}