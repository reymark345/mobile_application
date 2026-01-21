package com.example.mobile_application;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CapturedMangoes extends AppCompatActivity {

    private static final String TAG = "CapturedMangoes";
    // TODO: Change this URL to your cloud server endpoint

    private static final String SYNC_URL = "http://192.168.254.108:5000/api/upload";

    private RecyclerView recyclerView;
    private TextView emptyState;
    private CapturedMangoAdapter adapter;
    private ImageDbHelper dbHelper;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_captured_mangoes);

        recyclerView = findViewById(R.id.recyclerCaptured);
        emptyState = findViewById(R.id.txtEmptyState);
        adapter = new CapturedMangoAdapter();
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

        adapter.setOnSyncClickListener(this::syncImageToServer);

        loadImages();
    }

    private void syncImageToServer(CapturedImage item) {
        // Check internet connectivity first
        if (!isInternetAvailable()) {
            Toast.makeText(this, "No internet connection. Please check your network settings.", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Syncing image...", Toast.LENGTH_SHORT).show();

        executor.execute(() -> {
            try {
                byte[] fullImage = dbHelper.getImageBlobById(item.getId());
                if (fullImage == null || fullImage.length == 0) {
                    runOnUiThread(() ->
                            Toast.makeText(this, "Sync failed: image data missing.", Toast.LENGTH_SHORT).show()
                    );
                    return;
                }

                URL url = new URL(SYNC_URL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);
                connection.setConnectTimeout(30000);
                connection.setReadTimeout(30000);

                // Convert image blob to Base64
                String base64Image = Base64.encodeToString(fullImage, Base64.NO_WRAP);

                // Create JSON payload
                String jsonPayload = "{"
                        + "\"id\":" + item.getId() + ","
                        + "\"image\":\"" + base64Image + "\","
                        + "\"created_at\":" + item.getCreatedAt()
                        + "}";

                // Send data
                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = jsonPayload.getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                int responseCode = connection.getResponseCode();
                Log.d(TAG, "Sync response code: " + responseCode);

                runOnUiThread(() -> {
                    if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
                        // Update sync status in database
                        dbHelper.updateSyncStatus(item.getId(), true);
                        Toast.makeText(this, "Image synced successfully!", Toast.LENGTH_SHORT).show();
                        loadImages(); // Refresh list to remove synced item
                    } else {
                        Toast.makeText(this, "Sync failed. Server error: " + responseCode, Toast.LENGTH_SHORT).show();
                    }
                });

                connection.disconnect();

            } catch (Exception e) {
                Log.e(TAG, "Sync error: " + e.getMessage(), e);
                runOnUiThread(() ->
                        Toast.makeText(this, "Sync failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    private boolean isInternetAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) {
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network network = connectivityManager.getActiveNetwork();
            if (network == null) {
                return false;
            }
            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
            return capabilities != null && (
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
            ) && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
        } else {
            // For older Android versions
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            return networkInfo != null && networkInfo.isConnected();
        }
    }

    private void loadImages() {
        java.util.List<CapturedImage> images = dbHelper.getAllImages();
        adapter.submit(images);
        emptyState.setVisibility(images.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}