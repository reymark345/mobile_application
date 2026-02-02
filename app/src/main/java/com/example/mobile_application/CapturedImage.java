package com.example.mobile_application;

public class CapturedImage {
    private final long id;
    private final byte[] imageBlob;
    private final long createdAt;
    private final boolean syncStatus;
    private final byte[] resultBlob; // For classification results

    public CapturedImage(long id, byte[] imageBlob, long createdAt, boolean syncStatus) {
        this.id = id;
        this.imageBlob = imageBlob;
        this.createdAt = createdAt;
        this.syncStatus = syncStatus;
        this.resultBlob = null;
    }

    public CapturedImage(long id, byte[] imageBlob, long createdAt, boolean syncStatus, byte[] resultBlob) {
        this.id = id;
        this.imageBlob = imageBlob;
        this.createdAt = createdAt;
        this.syncStatus = syncStatus;
        this.resultBlob = resultBlob;
    }

    public long getId() {
        return id;
    }

    public byte[] getImageBlob() {
        return imageBlob;
    }

    // For backward compatibility, keep this method but it returns imageBlob
    @Deprecated
    public byte[] getThumbnailBlob() {
        return imageBlob;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public boolean isSynced() {
        return syncStatus;
    }

    public byte[] getResultBlob() {
        return resultBlob;
    }
}
