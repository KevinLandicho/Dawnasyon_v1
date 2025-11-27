// File: ImageCarouselAdapter.java
package com.example.dawnasyon_v1;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
// IMPORTANT: Ensure this R import path matches your project's package name
import com.example.dawnasyon_v1.R;
import java.util.List;

public class ImageCarouselAdapter extends RecyclerView.Adapter<ImageCarouselAdapter.ImageViewHolder> {

    // List of drawable resource IDs (Integers)
    private List<Integer> imageList;

    public ImageCarouselAdapter(List<Integer> imageList) {
        this.imageList = imageList;
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the layout for a single image slide
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_image_slide, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        // Set the image from the resource ID in the list
        holder.imageView.setImageResource(imageList.get(position));
    }

    @Override
    public int getItemCount() {
        return imageList.size();
    }

    // ⭐ FIX: Make the ViewHolder class STATIC to resolve the error ⭐
    public static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        public ImageViewHolder(View itemView) {
            super(itemView);
            // R.id.slide_image refers to the ImageView ID in item_image_slide.xml
            imageView = itemView.findViewById(R.id.slide_image);

            // Set scale type for better fit of your photos
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        }
    }
}