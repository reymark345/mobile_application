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

public class ClassificationResultAdapter extends RecyclerView.Adapter<ClassificationResultAdapter.ResultViewHolder> {

    private final List<CapturedImage> items = new ArrayList<>();
    private final DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);
    private OnDeleteClickListener onDeleteClickListener;
    private OnImageClickListener onImageClickListener;

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

    public void setOnImageClickListener(OnImageClickListener listener) {
        this.onImageClickListener = listener;
    }

    public interface OnDeleteClickListener {
        void onDeleteClick(CapturedImage item);
    }

    public interface OnImageClickListener {
        void onImageClick(CapturedImage item, boolean isResultImage);
    }

    @NonNull
    @Override
    public ResultViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_classification_result, parent, false);
        return new ResultViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ResultViewHolder holder, int position) {
        CapturedImage item = items.get(position);
        
        // Display original image
        byte[] imageBlob = item.getImageBlob();
        if (imageBlob != null && imageBlob.length > 0) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(imageBlob, 0, imageBlob.length);
            holder.originalImageView.setImageBitmap(bitmap);
        } else {
            holder.originalImageView.setImageResource(R.drawable.ic_launcher_foreground);
        }

        // Display result image
        byte[] resultBlob = item.getResultBlob();
        if (resultBlob != null && resultBlob.length > 0) {
            Bitmap resultBitmap = BitmapFactory.decodeByteArray(resultBlob, 0, resultBlob.length);
            holder.resultImageView.setImageBitmap(resultBitmap);
        } else {
            holder.resultImageView.setImageResource(R.drawable.ic_launcher_foreground);
        }

        String dateText = "Date Captured: " + dateFormat.format(new Date(item.getCreatedAt()));
        holder.dateText.setText(dateText);

        holder.deleteButton.setOnClickListener(v -> {
            if (onDeleteClickListener != null) {
                onDeleteClickListener.onDeleteClick(item);
            }
        });

        holder.originalImageView.setOnClickListener(v -> {
            if (onImageClickListener != null) {
                onImageClickListener.onImageClick(item, false);
            }
        });

        holder.resultImageView.setOnClickListener(v -> {
            if (onImageClickListener != null) {
                onImageClickListener.onImageClick(item, true);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ResultViewHolder extends RecyclerView.ViewHolder {
        final ImageView originalImageView;
        final ImageView resultImageView;
        final TextView dateText;
        final ImageView deleteButton;

        ResultViewHolder(@NonNull View itemView) {
            super(itemView);
            originalImageView = itemView.findViewById(R.id.imgOriginal);
            resultImageView = itemView.findViewById(R.id.imgResult);
            dateText = itemView.findViewById(R.id.txtCcText);
            deleteButton = itemView.findViewById(R.id.btnDelete);
        }
    }
}
