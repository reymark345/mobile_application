package com.example.mobile_application;

public class CapturedImage {
    private final long id;
    private final byte[] thumbnailBlob;
    private final long createdAt;
    private final boolean syncStatus;

    public CapturedImage(long id, byte[] thumbnailBlob, long createdAt, boolean syncStatus) {
        this.id = id;
        this.thumbnailBlob = thumbnailBlob;
        this.createdAt = createdAt;
        this.syncStatus = syncStatus;
    }

    public long getId() {
        return id;
    }

    public byte[] getThumbnailBlob() {
        return thumbnailBlob;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public boolean isSynced() {
        return syncStatus;
    }
}
