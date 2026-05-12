package com.example.mobile_application;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.io.ByteArrayOutputStream;

public class ImageDbHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "thesis_images.db";
    private static final int DB_VERSION = 7;
    private static final int BLOB_CHUNK_SIZE = 512 * 1024;

    public static final String TABLE_IMAGES = "images";
    public static final String COL_ID = "_id";
    public static final String COL_IMAGE = "image_blob";
    public static final String COL_THUMBNAIL = "thumbnail_blob";
    public static final String COL_IMAGE_RESULT = "image_result_blob";
    public static final String COL_RESULT_THUMBNAIL = "result_thumbnail_blob";
    public static final String COL_CREATED_AT = "created_at";
    public static final String COL_SYNC_STATUS = "sync_status";

    public ImageDbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String sql = "CREATE TABLE " + TABLE_IMAGES + " ("
                + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_IMAGE + " BLOB NOT NULL, "
                + COL_THUMBNAIL + " BLOB NOT NULL, "
                + COL_IMAGE_RESULT + " BLOB, "
                + COL_RESULT_THUMBNAIL + " BLOB, "
                + COL_CREATED_AT + " INTEGER NOT NULL, "
                + COL_SYNC_STATUS + " INTEGER NOT NULL DEFAULT 0"
                + ");";
        db.execSQL(sql);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 6) {
            // Older prototype schemas are not compatible with the current image table.
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_IMAGES);
            onCreate(db);
            return;
        }

        if (oldVersion < 7) {
            db.execSQL("ALTER TABLE " + TABLE_IMAGES
                    + " ADD COLUMN " + COL_RESULT_THUMBNAIL + " BLOB");
        }
    }

    public long insertImage(byte[] imageBytes, byte[] thumbnailBytes) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_IMAGE, imageBytes);
        values.put(COL_THUMBNAIL, thumbnailBytes);
        values.put(COL_CREATED_AT, System.currentTimeMillis());
        values.put(COL_SYNC_STATUS, 0); // Not synced by default
        return db.insert(TABLE_IMAGES, null, values);
    }

    public java.util.List<CapturedImage> getAllImages() {
        SQLiteDatabase db = getReadableDatabase();
        String[] cols = {COL_ID, COL_THUMBNAIL, COL_CREATED_AT, COL_SYNC_STATUS};
        // Only get images where sync_status = 0 (not synced)
        android.database.Cursor cursor = db.query(
                TABLE_IMAGES,
                cols,
                COL_SYNC_STATUS + " = ?",
                new String[]{"0"},
                null,
                null,
                COL_CREATED_AT + " DESC"
        );

        java.util.List<CapturedImage> items = new java.util.ArrayList<>();
        if (cursor != null) {
            while (cursor.moveToNext()) {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID));
                byte[] thumbnailBlob = cursor.getBlob(cursor.getColumnIndexOrThrow(COL_THUMBNAIL));
                long createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(COL_CREATED_AT));
                int syncStatus = cursor.getInt(cursor.getColumnIndexOrThrow(COL_SYNC_STATUS));
                items.add(new CapturedImage(id, thumbnailBlob, createdAt, syncStatus == 1));
            }
            cursor.close();
        }
        return items;
    }

    public byte[] getImageBlobById(long id) {
        return getBlobById(COL_IMAGE, id);
    }

    public byte[] getResultImageBlobById(long id) {
        return getBlobById(COL_IMAGE_RESULT, id);
    }

    private byte[] getBlobById(String columnName, long id) {
        validateBlobColumn(columnName);

        int blobLength = getBlobLength(columnName, id);
        if (blobLength < 0) {
            return null;
        }
        if (blobLength == 0) {
            return new byte[0];
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(blobLength);
        int offset = 1;
        int remaining = blobLength;

        while (remaining > 0) {
            int chunkSize = Math.min(BLOB_CHUNK_SIZE, remaining);
            byte[] chunk = getBlobChunk(columnName, id, offset, chunkSize);

            if (chunk == null || chunk.length == 0) {
                return null;
            }

            outputStream.write(chunk, 0, chunk.length);
            offset += chunk.length;
            remaining -= chunk.length;
        }

        return outputStream.toByteArray();
    }

    private int getBlobLength(String columnName, long id) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT length(" + columnName + ") FROM " + TABLE_IMAGES
                        + " WHERE " + COL_ID + " = ?",
                new String[]{String.valueOf(id)}
        );

        try {
            if (cursor != null && cursor.moveToFirst()) {
                if (cursor.isNull(0)) {
                    return -1;
                }

                long blobLength = cursor.getLong(0);
                if (blobLength > Integer.MAX_VALUE) {
                    throw new IllegalStateException("Image is too large to load.");
                }

                return (int) blobLength;
            }
            return -1;
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    private byte[] getBlobChunk(String columnName, long id, int offset, int chunkSize) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT substr(" + columnName + ", ?, ?) FROM " + TABLE_IMAGES
                        + " WHERE " + COL_ID + " = ?",
                new String[]{
                        String.valueOf(offset),
                        String.valueOf(chunkSize),
                        String.valueOf(id)
                }
        );

        try {
            if (cursor != null && cursor.moveToFirst() && !cursor.isNull(0)) {
                return cursor.getBlob(0);
            }
            return null;
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    private void validateBlobColumn(String columnName) {
        if (!COL_IMAGE.equals(columnName) && !COL_IMAGE_RESULT.equals(columnName)) {
            throw new IllegalArgumentException("Unsupported blob column: " + columnName);
        }
    }

    public boolean deleteImage(long id) {
        SQLiteDatabase db = getWritableDatabase();
        int deleted = db.delete(TABLE_IMAGES, COL_ID + " = ?", new String[]{String.valueOf(id)});
        return deleted > 0;
    }

    public int getImagesCount() {
        SQLiteDatabase db = getReadableDatabase();
        // Count only images that are not yet synced (sync_status = 0)
        android.database.Cursor cursor = db.rawQuery(
                "SELECT COUNT(" + COL_ID + ") FROM " + TABLE_IMAGES +
                        " WHERE " + COL_SYNC_STATUS + " = 0",
                null);
        int count = 0;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                count = cursor.getInt(0);
            }
            cursor.close();
        }
        return count;
    }

    public int getSyncedImagesCount() {
        SQLiteDatabase db = getReadableDatabase();
        // Count only images that are synced (sync_status = 1)
        android.database.Cursor cursor = db.rawQuery(
                "SELECT COUNT(" + COL_ID + ") FROM " + TABLE_IMAGES +
                        " WHERE " + COL_SYNC_STATUS + " = 1",
                null);
        int count = 0;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                count = cursor.getInt(0);
            }
            cursor.close();
        }
        return count;
    }

    public boolean updateSyncStatus(long id, boolean synced) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_SYNC_STATUS, synced ? 1 : 0);
        int updated = db.update(TABLE_IMAGES, values, COL_ID + " = ?", new String[]{String.valueOf(id)});
        return updated > 0;
    }

    public boolean updateImageResult(long id, byte[] resultBytes, byte[] resultThumbnailBytes) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_IMAGE_RESULT, resultBytes);
        if (resultThumbnailBytes != null && resultThumbnailBytes.length > 0) {
            values.put(COL_RESULT_THUMBNAIL, resultThumbnailBytes);
        } else {
            values.putNull(COL_RESULT_THUMBNAIL);
        }
        values.put(COL_SYNC_STATUS, 1);
        int updated = db.update(TABLE_IMAGES, values, COL_ID + " = ?", new String[]{String.valueOf(id)});
        return updated > 0;
    }

    public java.util.List<CapturedImage> getSyncedImages() {
        SQLiteDatabase db = getReadableDatabase();
        String[] cols = {COL_ID, COL_THUMBNAIL, COL_RESULT_THUMBNAIL, COL_CREATED_AT, COL_SYNC_STATUS};
        // Only get images where sync_status = 1 (synced)
        android.database.Cursor cursor = db.query(
                TABLE_IMAGES,
                cols,
                COL_SYNC_STATUS + " = ?",
                new String[]{"1"},
                null,
                null,
                COL_CREATED_AT + " DESC"
        );

        java.util.List<CapturedImage> items = new java.util.ArrayList<>();
        if (cursor != null) {
            while (cursor.moveToNext()) {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID));
                byte[] thumbnailBlob = cursor.getBlob(cursor.getColumnIndexOrThrow(COL_THUMBNAIL));
                byte[] resultBlob = null;
                int resultIndex = cursor.getColumnIndex(COL_RESULT_THUMBNAIL);
                if (resultIndex >= 0 && !cursor.isNull(resultIndex)) {
                    resultBlob = cursor.getBlob(resultIndex);
                }
                long createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(COL_CREATED_AT));
                int syncStatus = cursor.getInt(cursor.getColumnIndexOrThrow(COL_SYNC_STATUS));
                items.add(new CapturedImage(id, thumbnailBlob, createdAt, syncStatus == 1, resultBlob));
            }
            cursor.close();
        }
        return items;
    }
}
