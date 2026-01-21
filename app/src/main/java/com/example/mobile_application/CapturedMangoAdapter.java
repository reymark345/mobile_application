package com.example.mobile_application;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class CapturedMangoAdapter extends RecyclerView.Adapter<CapturedMangoAdapter.MangoViewHolder> {

    private final List<CapturedImage> items = new ArrayList<>();
    private final DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);
    private OnDeleteClickListener onDeleteClickListener;
    private OnSyncClickListener onSyncClickListener;

    public void submit(List<CapturedImage> data) {
        items.clear();
        if (data != null) {
            items.addAll(data);
        }
        notifyDataSetChanged();
    }

    public void setOnDeleteClickListener(OnDeleteClickListener listener) {
        this.onDeleteClickListener = listener;
    }

    public void setOnSyncClickListener(OnSyncClickListener listener) {
        this.onSyncClickListener = listener;
    }

    public interface OnDeleteClickListener {
        void onDeleteClick(CapturedImage item);
    }

    public interface OnSyncClickListener {
        void onSyncClick(CapturedImage item);
    }

    @NonNull
    @Override
    public MangoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_captured_mango, parent, false);
        return new MangoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MangoViewHolder holder, int position) {
        CapturedImage item = items.get(position);
        byte[] thumb = item.getThumbnailBlob();
        if (thumb != null && thumb.length > 0) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(thumb, 0, thumb.length);
            holder.imageView.setImageBitmap(bitmap);
        } else {
            holder.imageView.setImageResource(R.drawable.ic_launcher_foreground);
        }

        String dateText = "Date Captured: " + dateFormat.format(new Date(item.getCreatedAt()));
        holder.dateText.setText(dateText);

        holder.deleteButton.setOnClickListener(v -> {
            if (onDeleteClickListener != null) {
                onDeleteClickListener.onDeleteClick(item);
            }
        });

        holder.syncButton.setOnClickListener(v -> {
            if (onSyncClickListener != null) {
                onSyncClickListener.onSyncClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class MangoViewHolder extends RecyclerView.ViewHolder {
        final ImageView imageView;
        final TextView dateText;
        final ImageView deleteButton;
        final ImageView syncButton;

        MangoViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imgFood);
            dateText = itemView.findViewById(R.id.txtCcText);
            deleteButton = itemView.findViewById(R.id.btnDelete);
            syncButton = itemView.findViewById(R.id.btnSync);
        }
    }
}
