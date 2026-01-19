package com.example.mobile_application;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class Dashboard extends AppCompatActivity {

    private ImageDbHelper dbHelper;
    private TextView txtCountMangoes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_dashboard);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        dbHelper = new ImageDbHelper(this);
        txtCountMangoes = findViewById(R.id.txtCountMangoes);

        updateMangoesCount();
        
        // Set click listener for dashboard_item_1
        findViewById(R.id.dashboardItem1).setOnClickListener(v -> {
            Intent intent = new Intent(Dashboard.this, Picture.class);
            startActivity(intent);
        });

        findViewById(R.id.dashboardItem2).setOnClickListener(v -> {
            Intent intent = new Intent(Dashboard.this, CapturedMangoes.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateMangoesCount();
    }

    private void updateMangoesCount() {
        if (txtCountMangoes != null && dbHelper != null) {
            int count = dbHelper.getImagesCount();
            txtCountMangoes.setText(String.valueOf(count));
        }
    }
}