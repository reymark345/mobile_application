package com.example.mobile_application;

import android.os.Bundle;

import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class CapturedMangoes extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TextView emptyState;
    private CapturedMangoAdapter adapter;
    private ImageDbHelper dbHelper;
    private View syncNowButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_captured_mangoes);

        recyclerView = findViewById(R.id.recyclerCaptured);
        emptyState = findViewById(R.id.txtEmptyState);
        adapter = new CapturedMangoAdapter();
        dbHelper = new ImageDbHelper(this);
        syncNowButton = findViewById(R.id.btnSyncNow);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        adapter.setOnDeleteClickListener(item -> {
            if (dbHelper.deleteImage(item.getId())) {
                Toast.makeText(this, "Deleted successfully.", Toast.LENGTH_SHORT).show();
                loadImages();
            } else {
                Toast.makeText(this, "Failed to delete.", Toast.LENGTH_SHORT).show();
            }
        });

        syncNowButton.setOnClickListener(v ->
                Toast.makeText(this, "Sync not implemented yet.", Toast.LENGTH_SHORT).show()
        );

        loadImages();
    }

    private void loadImages() {
        java.util.List<CapturedImage> images = dbHelper.getAllImages();
        adapter.submit(images);
        emptyState.setVisibility(images.isEmpty() ? View.VISIBLE : View.GONE);
    }
}