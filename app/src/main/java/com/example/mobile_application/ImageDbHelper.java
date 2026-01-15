package com.example.mobile_application;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class ImageDbHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "thesis_images.db";
    private static final int DB_VERSION = 1;

    public static final String TABLE_IMAGES = "images";
    public static final String COL_ID = "_id";
    public static final String COL_IMAGE = "image_blob";
    public static final String COL_CREATED_AT = "created_at";

    public ImageDbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String sql = "CREATE TABLE " + TABLE_IMAGES + " ("
                + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_IMAGE + " BLOB NOT NULL, "
                + COL_CREATED_AT + " INTEGER NOT NULL"
                + ");";
        db.execSQL(sql);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // For thesis/prototype purposes
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_IMAGES);
        onCreate(db);
    }

    public long insertImage(byte[] imageBytes) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_IMAGE, imageBytes);
        values.put(COL_CREATED_AT, System.currentTimeMillis());
        return db.insert(TABLE_IMAGES, null, values);
    }
}
