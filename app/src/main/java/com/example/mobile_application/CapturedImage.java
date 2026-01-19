package com.example.mobile_application;

public class CapturedImage {
    private final long id;
    private final byte[] imageBlob;
    private final long createdAt;

    public CapturedImage(long id, byte[] imageBlob, long createdAt) {
        this.id = id;
        this.imageBlob = imageBlob;
        this.createdAt = createdAt;
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
}
