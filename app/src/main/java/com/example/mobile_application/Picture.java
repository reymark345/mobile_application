package com.example.mobile_application;

import android.Manifest;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class Picture extends AppCompatActivity {

    private ImageView imgPreview;
    private Button btnCapture, btnSave, btnSync;

    private Bitmap capturedBitmap;
    private Uri photoUri; // URI for full-resolution camera photo
    private Uri selectedImageUri;

    private ImageDbHelper dbHelper;

    private static final int REQ_PERMS = 101;

    // We remember what the user chose so after permission grant we continue automatically
    private enum PendingAction { NONE, CAMERA, GALLERY }
    private PendingAction pendingAction = PendingAction.NONE;

    private final ActivityResultLauncher<Intent> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && photoUri != null) {
                    try {
                        // Load full-resolution image from the file
                        selectedImageUri = photoUri;
                        capturedBitmap = loadBitmapFromUri(photoUri);
                        imgPreview.setImageBitmap(capturedBitmap);
                        btnSync.setVisibility(android.view.View.VISIBLE);
                    } catch (IOException e) {
                        Toast.makeText(this, "Failed to load captured image.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, "No image captured.", Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<Intent> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    if (imageUri != null) {
                        try {
                            selectedImageUri = imageUri;
                            capturedBitmap = loadBitmapFromUri(imageUri);
                            imgPreview.setImageBitmap(capturedBitmap);
                            btnSync.setVisibility(android.view.View.VISIBLE);
                        } catch (IOException e) {
                            Toast.makeText(this, "Failed to load image.", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_picture);

        imgPreview = findViewById(R.id.imgPreview);
        btnCapture = findViewById(R.id.btnCapture);
        btnSave = findViewById(R.id.btnSave);
        btnSync = findViewById(R.id.btnSync);

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

                    if (hasRequiredPermissions(pendingAction)) {
                        runPendingAction();
                    } else {
                        requestRequiredPermissions(pendingAction);
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
            try {
                File photoFile = File.createTempFile("camera_photo_", ".jpg", getCacheDir());
                photoUri = FileProvider.getUriForFile(this,
                        getPackageName() + ".fileprovider",
                        photoFile);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                intent.setClipData(ClipData.newUri(getContentResolver(), "Captured photo", photoUri));
                intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);

                List<ResolveInfo> cameraApps = getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
                for (ResolveInfo resolveInfo : cameraApps) {
                    grantUriPermission(resolveInfo.activityInfo.packageName, photoUri,
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                }

                cameraLauncher.launch(intent);
            } catch (IOException | IllegalArgumentException e) {
                Toast.makeText(this, "Could not prepare camera capture.", Toast.LENGTH_SHORT).show();
            }
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

    private boolean hasRequiredPermissions(PendingAction action) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true; // Android 5.0 and below runtime permissions
        }

        if (action == PendingAction.CAMERA) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED;
        }

        if (action == PendingAction.GALLERY) {
            if (Build.VERSION.SDK_INT >= 33) {
                return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                        == PackageManager.PERMISSION_GRANTED;
            }

            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED;
        }

        return true;
    }

    private void requestRequiredPermissions(PendingAction action) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return;

        String[] perms;
        if (action == PendingAction.CAMERA) {
            perms = new String[]{Manifest.permission.CAMERA};
        } else if (Build.VERSION.SDK_INT >= 33) {
            perms = new String[]{Manifest.permission.READ_MEDIA_IMAGES};
        } else {
            perms = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE};
        }

        ActivityCompat.requestPermissions(this, perms, REQ_PERMS);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQ_PERMS) {
            boolean allGranted = grantResults.length > 0;
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
        if (capturedBitmap == null || selectedImageUri == null) {
            Toast.makeText(this, "Capture or choose an image first.", Toast.LENGTH_SHORT).show();
            return;
        }

        long id;
        try {
            byte[] originalImageBytes = readBytesFromUri(selectedImageUri);
            byte[] thumbnailBytes = bitmapToJpegBytes(resizeBitmap(capturedBitmap, 512), 80);
            id = dbHelper.insertImage(originalImageBytes, thumbnailBytes);
        } catch (IOException e) {
            Toast.makeText(this, "Save failed: could not read original image.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (id != -1) {
            Toast.makeText(this, "Saved to Offline", Toast.LENGTH_SHORT).show();
            resetImageViewer();
        } else {
            Toast.makeText(this, "Save failed.", Toast.LENGTH_SHORT).show();
        }
    }

    private void resetImageViewer() {
        imgPreview.setImageBitmap(null);
        btnSync.setVisibility(android.view.View.GONE);
        capturedBitmap = null;
        selectedImageUri = null;
    }

    private Bitmap resizeBitmap(Bitmap src, int maxSize) {
        int w = src.getWidth();
        int h = src.getHeight();
        if (w <= 0 || h <= 0) return src;
        int maxDim = Math.max(w, h);
        if (maxDim <= maxSize) return src;
        float scale = (float) maxSize / (float) maxDim;
        int nw = Math.max(1, Math.round(w * scale));
        int nh = Math.max(1, Math.round(h * scale));
        return Bitmap.createScaledBitmap(src, nw, nh, true);
    }

    private byte[] bitmapToJpegBytes(Bitmap bitmap, int quality) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos);
        return baos.toByteArray();
    }

    private byte[] readBytesFromUri(Uri uri) throws IOException {
        try (InputStream inputStream = getContentResolver().openInputStream(uri);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            if (inputStream == null) {
                throw new IOException("Unable to open image URI");
            }

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }
            return baos.toByteArray();
        }
    }
}
