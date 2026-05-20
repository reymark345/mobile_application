package com.example.mobile_application;

import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class ClassificationResult extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TextView emptyState;
    private ClassificationResultAdapter adapter;
    private ImageDbHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_classification_result);

        recyclerView = findViewById(R.id.recyclerClassification);
        emptyState = findViewById(R.id.txtEmptyState);
        adapter = new ClassificationResultAdapter();
        dbHelper = new ImageDbHelper(this);

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

        adapter.setOnImageClickListener((item, isResultImage) -> showFullscreenImage(item, isResultImage));

        loadImages();
    }

    private void showFullscreenImage(CapturedImage item, boolean isResultImage) {
        // Create a custom dialog
        Dialog dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_fullscreen_image);

        ImageView fullscreenImageView = dialog.findViewById(R.id.imgFullscreen);
        ImageButton closeButton = dialog.findViewById(R.id.btnClose);

        byte[] imageBlob = null;
        
        if (isResultImage) {
            // Load the result image from database
            imageBlob = dbHelper.getResultImageBlobById(item.getId());
            if (imageBlob == null || imageBlob.length == 0) {
                // Fallback to result blob from item if available
                imageBlob = item.getResultBlob();
            }
        } else {
            // Load the original image from database
            imageBlob = dbHelper.getImageBlobById(item.getId());
        }

        if (imageBlob != null && imageBlob.length > 0) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(imageBlob, 0, imageBlob.length);
            fullscreenImageView.setImageBitmap(bitmap);
        } else {
            // Fallback to image blob if full image is not available
            imageBlob = item.getImageBlob();
            if (imageBlob != null && imageBlob.length > 0) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(imageBlob, 0, imageBlob.length);
                fullscreenImageView.setImageBitmap(bitmap);
            } else {
                Toast.makeText(this, "Image not available", Toast.LENGTH_SHORT).show();
            }
        }

        // Close button click listener
        closeButton.setOnClickListener(v -> dialog.dismiss());

        // Also allow tapping the image to close
        fullscreenImageView.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void loadImages() {
        java.util.List<CapturedImage> images = dbHelper.getSyncedImages();
        adapter.submit(images);
        emptyState.setVisibility(images.isEmpty() ? View.VISIBLE : View.GONE);
    }
}
