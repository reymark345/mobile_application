package com.example.mobile_application;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable goHomeRunnable = new Runnable() {
        @Override
        public void run() {
            Intent intent = new Intent(MainActivity.this, Dashboard.class);
            startActivity(intent);
            finish(); // prevents going back to splash
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Show splash for 2 seconds (2000 ms)
        handler.postDelayed(goHomeRunnable, 500);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Avoid memory leaks if activity closes early
        handler.removeCallbacks(goHomeRunnable);
    }
}
