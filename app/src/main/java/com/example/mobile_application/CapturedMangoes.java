package com.example.mobile_application;

import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CapturedMangoes extends AppCompatActivity {

    private static final String TAG = "CapturedMangoes";
    private static final String BASE_URL = "http://10.0.2.2:5000";

//    private static final String BASE_URL = "http://192.168.254.115:5000";

    private static final String SYNC_URL = BASE_URL + "/api/upload";

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
        adapter.setOnImageClickListener(this::showFullscreenImage);

        loadImages();
    }

    private void showFullscreenImage(CapturedImage item) {
        Dialog dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_fullscreen_image);

        ImageView fullscreenImageView = dialog.findViewById(R.id.imgFullscreen);
        ImageButton closeButton = dialog.findViewById(R.id.btnClose);

        byte[] fullImageBlob = dbHelper.getImageBlobById(item.getId());

        if (fullImageBlob != null && fullImageBlob.length > 0) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(fullImageBlob, 0, fullImageBlob.length);
            fullscreenImageView.setImageBitmap(bitmap);
        } else {
            byte[] imageBlob = item.getImageBlob();

            if (imageBlob != null && imageBlob.length > 0) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(imageBlob, 0, imageBlob.length);
                fullscreenImageView.setImageBitmap(bitmap);
            }

            Toast.makeText(this, "Full image not available", Toast.LENGTH_SHORT).show();
        }

        closeButton.setOnClickListener(v -> dialog.dismiss());
        fullscreenImageView.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void syncImageToServer(CapturedImage item) {
        if (!isInternetAvailable()) {
            Toast.makeText(this, "No internet connection. Please check your network settings.", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Syncing image...", Toast.LENGTH_SHORT).show();

        executor.execute(() -> {
            HttpURLConnection connection = null;

            try {
                byte[] fullImage = dbHelper.getImageBlobById(item.getId());

                if (fullImage == null || fullImage.length == 0) {
                    runOnUiThread(() ->
                            Toast.makeText(this, "Sync failed: image data missing.", Toast.LENGTH_SHORT).show()
                    );
                    return;
                }

                String base64Image = Base64.encodeToString(fullImage, Base64.NO_WRAP);

                String jsonPayload = "{"
                        + "\"id\":" + item.getId() + ","
                        + "\"image\":\"" + base64Image + "\","
                        + "\"created_at\":" + item.getCreatedAt()
                        + "}";

                byte[] input = jsonPayload.getBytes("UTF-8");

                System.setProperty("http.keepAlive", "false");

                URL url = new URL(SYNC_URL);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                connection.setRequestProperty("Accept", "application/json");
                connection.setRequestProperty("Connection", "close");
                connection.setDoInput(true);
                connection.setDoOutput(true);
                connection.setConnectTimeout(60000);
                connection.setReadTimeout(300000);
                connection.setFixedLengthStreamingMode(input.length);

                try (OutputStream os = connection.getOutputStream()) {
                    os.write(input);
                    os.flush();
                }

                int responseCode = connection.getResponseCode();
                Log.d(TAG, "Sync response code: " + responseCode);

                InputStream responseStream = responseCode >= 200 && responseCode < 300
                        ? connection.getInputStream()
                        : connection.getErrorStream();

                String jsonResponse = readTextResponse(responseStream);
                Log.d(TAG, "Sync response body: " + jsonResponse);

                if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
                    JSONObject jsonObject = new JSONObject(jsonResponse);

                    if (!jsonObject.optBoolean("success", false)) {
                        String errorMessage = jsonObject.optString("error", "Server did not return success");
                        throw new Exception(errorMessage);
                    }

                    String rowId = jsonObject.getString("id");
                    downloadResultImage(rowId);

                    runOnUiThread(() -> {
                        Toast.makeText(this, "Image synced and result saved!", Toast.LENGTH_SHORT).show();
                        loadImages();
                    });
                } else {
                    runOnUiThread(() ->
                            Toast.makeText(this, "Sync failed. Server error: " + responseCode, Toast.LENGTH_SHORT).show()
                    );
                }

            } catch (Exception e) {
                Log.e(TAG, "Sync error: " + e.getMessage(), e);

                runOnUiThread(() ->
                        Toast.makeText(this, "Sync failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    private String readTextResponse(InputStream inputStream) throws Exception {
        if (inputStream == null) {
            return "";
        }

        StringBuilder responseBuilder = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;

            while ((line = reader.readLine()) != null) {
                responseBuilder.append(line);
            }
        }

        return responseBuilder.toString();
    }

    private void downloadResultImage(String rowId) throws Exception {
        HttpURLConnection connection = null;

        try {
            URL url = new URL(BASE_URL + "/image?id=" + rowId);

            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "image/jpeg");
            connection.setRequestProperty("Connection", "close");
            connection.setConnectTimeout(60000);
            connection.setReadTimeout(300000);

            int responseCode = connection.getResponseCode();
            Log.d(TAG, "Download response code: " + responseCode);

            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new Exception("Download result failed. Server error: " + responseCode);
            }

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();

            try (InputStream inputStream = connection.getInputStream()) {
                byte[] data = new byte[4096];
                int bytesRead;

                while ((bytesRead = inputStream.read(data)) != -1) {
                    buffer.write(data, 0, bytesRead);
                }
            }

            updateImageResultBlob(rowId, buffer.toByteArray());

        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
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

            return capabilities != null
                    && (
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                            || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                            || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
            )
                    && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
        } else {
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            return networkInfo != null && networkInfo.isConnected();
        }
    }

    private void loadImages() {
        java.util.List<CapturedImage> images = dbHelper.getAllImages();
        adapter.submit(images);
        emptyState.setVisibility(images.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void updateImageResultBlob(String rowId, byte[] imageBytes) {
        try {
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
                    new String[]{rowId}
            );

            db.close();

            runOnUiThread(() ->
                    Toast.makeText(
                            this,
                            "Updated ID " + rowId + ", rows: " + rowsUpdated,
                            Toast.LENGTH_LONG
                    ).show()
            );

        } catch (Exception e) {
            e.printStackTrace();

            runOnUiThread(() ->
                    Toast.makeText(
                            this,
                            "Error: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show()
            );
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
