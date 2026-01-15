package com.example.mobile_application;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class HomeActivity extends AppCompatActivity {

    private ImageView imgPreview;
    private Button btnCapture, btnSave;

    private Bitmap capturedBitmap;

    private ImageDbHelper dbHelper;

    private static final int REQ_PERMS = 101;

    // We remember what the user chose so after permission grant we continue automatically
    private enum PendingAction { NONE, CAMERA, GALLERY }
    private PendingAction pendingAction = PendingAction.NONE;

    private final ActivityResultLauncher<Intent> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Bundle extras = result.getData().getExtras();
                    if (extras != null && extras.get("data") instanceof Bitmap) {
                        capturedBitmap = (Bitmap) extras.get("data"); // thumbnail bitmap
                        imgPreview.setImageBitmap(capturedBitmap);
                    } else {
                        Toast.makeText(this, "No image captured.", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    private final ActivityResultLauncher<Intent> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    if (imageUri != null) {
                        try {
                            capturedBitmap = loadBitmapFromUri(imageUri);
                            imgPreview.setImageBitmap(capturedBitmap);
                        } catch (IOException e) {
                            Toast.makeText(this, "Failed to load image.", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        imgPreview = findViewById(R.id.imgPreview);
        btnCapture = findViewById(R.id.btnCapture);
        btnSave = findViewById(R.id.btnSave);

        dbHelper = new ImageDbHelper(this);

        btnCapture.setOnClickListener(v -> showImageSourceDialog());
        btnSave.setOnClickListener(v -> saveToSqlite());
    }

    private void showImageSourceDialog() {
        String[] options = {"Capture from Camera", "Choose from Gallery"};

        new AlertDialog.Builder(this)
                .setTitle("Select Image Source")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        pendingAction = PendingAction.CAMERA;
                    } else {
                        pendingAction = PendingAction.GALLERY;
                    }

                    if (hasRequiredPermissions()) {
                        runPendingAction();
                    } else {
                        requestRequiredPermissions();
                    }
                })
                .setNegativeButton("Cancel", (d, w) -> {
                    pendingAction = PendingAction.NONE;
                    d.dismiss();
                })
                .show();
    }

    private void runPendingAction() {
        if (pendingAction == PendingAction.CAMERA) {
            openCamera();
        } else if (pendingAction == PendingAction.GALLERY) {
            openGallery();
        }
        pendingAction = PendingAction.NONE;
    }

    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            cameraLauncher.launch(intent);
        } else {
            Toast.makeText(this, "No camera app found.", Toast.LENGTH_SHORT).show();
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(intent);
    }

    private Bitmap loadBitmapFromUri(Uri uri) throws IOException {
        if (Build.VERSION.SDK_INT >= 28) {
            ImageDecoder.Source source = ImageDecoder.createSource(getContentResolver(), uri);
            return ImageDecoder.decodeBitmap(source);
        } else {
            //noinspection deprecation
            return MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
        }
    }

    private boolean hasRequiredPermissions() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true; // Android 5.0 and below runtime permissions
        }

        boolean camGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;

        boolean readGranted;
        if (Build.VERSION.SDK_INT >= 33) {
            readGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    == PackageManager.PERMISSION_GRANTED;
        } else {
            readGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED;
        }

        // If you only want to require storage permission for Gallery (not Camera),
        // you can split these checks. This is simplest: require both.
        return camGranted && readGranted;
    }

    private void requestRequiredPermissions() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return;

        String[] perms;
        if (Build.VERSION.SDK_INT >= 33) {
            perms = new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_MEDIA_IMAGES
            };
        } else {
            perms = new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_EXTERNAL_STORAGE
            };
        }

        ActivityCompat.requestPermissions(this, perms, REQ_PERMS);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQ_PERMS) {
            boolean allGranted = true;
            for (int r : grantResults) {
                if (r != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                runPendingAction(); // ✅ Continue automatically
            } else {
                Toast.makeText(this,
                        "Permission denied. Enable it in Settings to use camera/gallery.",
                        Toast.LENGTH_SHORT).show();
                pendingAction = PendingAction.NONE;
            }
        }
    }

    private void saveToSqlite() {
        if (capturedBitmap == null) {
            Toast.makeText(this, "Capture or choose an image first.", Toast.LENGTH_SHORT).show();
            return;
        }

        byte[] imageBytes = bitmapToPngBytes(capturedBitmap);
        long id = dbHelper.insertImage(imageBytes);

        if (id != -1) {
            Toast.makeText(this, "Saved to SQLite (ID: " + id + ")", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Save failed.", Toast.LENGTH_SHORT).show();
        }
    }

    private byte[] bitmapToPngBytes(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
        return baos.toByteArray();
    }
}
