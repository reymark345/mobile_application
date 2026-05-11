package com.example.mobile_application;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

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

        updateImageResultBlob();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Show splash for 2 seconds (2000 ms)
//        handler.postDelayed(goHomeRunnable, 500);
        handler.postDelayed(goHomeRunnable, 2000);
    }

    private void updateImageResultBlob() {

        new Thread(() -> {

            try {

                String imageUrl = "http://10.0.2.2:5000/image";

                URL url = new URL(imageUrl);

                HttpURLConnection connection =
                        (HttpURLConnection) url.openConnection();

                connection.connect();

                InputStream inputStream =
                        connection.getInputStream();

                ByteArrayOutputStream buffer =
                        new ByteArrayOutputStream();

                byte[] data = new byte[4096];

                int bytesRead;

                while ((bytesRead = inputStream.read(data)) != -1) {
                    buffer.write(data, 0, bytesRead);
                }

                byte[] imageBytes = buffer.toByteArray();

                inputStream.close();

                connection.disconnect();

                SQLiteDatabase db = openOrCreateDatabase(
                        "thesis_images.db",
                        MODE_PRIVATE,
                        null
                );

                ContentValues values = new ContentValues();

                values.put("image_result_blob", imageBytes);
                values.put("sync_status", 1);

                int rowsUpdated = db.update(
                        "images",
                        values,
                        "_id = ?",
                        new String[]{"3"}
                );

                db.close();

                runOnUiThread(() -> {

                    Toast.makeText(
                            this,
                            "Updated rows: " + rowsUpdated,
                            Toast.LENGTH_LONG
                    ).show();

                });

            } catch (Exception e) {

                e.printStackTrace();

                runOnUiThread(() -> {

                    Toast.makeText(
                            this,
                            "Error: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();

                });

            }

        }).start();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Avoid memory leaks if activity closes early
        handler.removeCallbacks(goHomeRunnable);
    }
}
