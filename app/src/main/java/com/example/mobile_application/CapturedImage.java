package com.example.mobile_application;

public class CapturedImage {
    private final long id;
    private final byte[] imageBlob;
    private final long createdAt;
    private final boolean syncStatus;

    public CapturedImage(long id, byte[] imageBlob, long createdAt, boolean syncStatus) {
        this.id = id;
        this.imageBlob = imageBlob;
        this.createdAt = createdAt;
        this.syncStatus = syncStatus;
    }

    public long getId() {
        return id;
    }

    public byte[] getImageBlob() {
        return imageBlob;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public boolean isSynced() {
        return syncStatus;
    }
}
