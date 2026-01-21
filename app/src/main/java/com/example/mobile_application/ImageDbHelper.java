package com.example.mobile_application;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class ImageDbHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "thesis_images.db";
    private static final int DB_VERSION = 3;

    public static final String TABLE_IMAGES = "images";
    public static final String COL_ID = "_id";
    public static final String COL_IMAGE = "image_blob";
    public static final String COL_THUMB = "image_thumb";
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
                + COL_THUMB + " BLOB NOT NULL, "
                + COL_CREATED_AT + " INTEGER NOT NULL, "
                + COL_SYNC_STATUS + " INTEGER NOT NULL DEFAULT 0"
                + ");";
        db.execSQL(sql);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // For thesis/prototype purposes
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_IMAGES);
        onCreate(db);
    }

    public long insertImage(byte[] imageBytes, byte[] thumbnailBytes) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_IMAGE, imageBytes);
        values.put(COL_THUMB, thumbnailBytes);
        values.put(COL_CREATED_AT, System.currentTimeMillis());
        values.put(COL_SYNC_STATUS, 0); // Not synced by default
        return db.insert(TABLE_IMAGES, null, values);
    }

    public java.util.List<CapturedImage> getAllImages() {
        SQLiteDatabase db = getReadableDatabase();
        // NOTE: Do not select full image blobs here; can exceed CursorWindow.
        String[] cols = {COL_ID, COL_THUMB, COL_CREATED_AT, COL_SYNC_STATUS};
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
                byte[] thumb = cursor.getBlob(cursor.getColumnIndexOrThrow(COL_THUMB));
                long createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(COL_CREATED_AT));
                int syncStatus = cursor.getInt(cursor.getColumnIndexOrThrow(COL_SYNC_STATUS));
                items.add(new CapturedImage(id, thumb, createdAt, syncStatus == 1));
            }
            cursor.close();
        }
        return items;
    }

    public byte[] getImageBlobById(long id) {
        SQLiteDatabase db = getReadableDatabase();
        android.database.Cursor cursor = db.query(
                TABLE_IMAGES,
                new String[]{COL_IMAGE},
                COL_ID + " = ?",
                new String[]{String.valueOf(id)},
                null,
                null,
                null,
                "1"
        );

        try {
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getBlob(cursor.getColumnIndexOrThrow(COL_IMAGE));
            }
            return null;
        } finally {
            if (cursor != null) cursor.close();
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

    public boolean updateSyncStatus(long id, boolean synced) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_SYNC_STATUS, synced ? 1 : 0);
        int updated = db.update(TABLE_IMAGES, values, COL_ID + " = ?", new String[]{String.valueOf(id)});
        return updated > 0;
    }
}
