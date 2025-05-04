package com.example.synctalk.activities;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.synctalk.R;

/**
 * Activity for displaying full-size images from chat messages
 */
public class ImageViewerActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_viewer);

        // Initialize views
        ImageView fullImage = findViewById(R.id.fullImage);
        ImageButton closeButton = findViewById(R.id.closeButton);

        // Get image URL from intent
        String imageUrl = getIntent().getStringExtra("image_url");

        // Load image using Glide
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_image_placeholder) // Add a placeholder drawable
                    .error(R.drawable.ic_image_error) // Add an error drawable
                    .into(fullImage);
        }

        // Set up close button click listener
        closeButton.setOnClickListener(v -> finish());
    }
}