package com.example.synctalk.adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.synctalk.R;
import com.example.synctalk.activities.ImageViewerActivity;
import com.example.synctalk.models.Message;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int VIEW_TYPE_SENT_TEXT = 1;
    private static final int VIEW_TYPE_SENT_IMAGE = 2;
    private static final int VIEW_TYPE_RECEIVED_TEXT = 3;
    private static final int VIEW_TYPE_RECEIVED_IMAGE = 4;

    private Context context;
    private List<Message> messages;
    private String currentUserId;

    public MessageAdapter(Context context, List<Message> messages, String currentUserId) {
        this.context = context;
        this.messages = messages;
        this.currentUserId = currentUserId;
    }

    @Override
    public int getItemViewType(int position) {
        Message message = messages.get(position);
        boolean isSender = message.getSenderId().equals(currentUserId);

        if (message.getType().equals("text")) {
            return isSender ? VIEW_TYPE_SENT_TEXT : VIEW_TYPE_RECEIVED_TEXT;
        } else {
            return isSender ? VIEW_TYPE_SENT_IMAGE : VIEW_TYPE_RECEIVED_IMAGE;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_SENT_TEXT) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_message_sent, parent, false);
            return new SentTextViewHolder(view);
        } else if (viewType == VIEW_TYPE_SENT_IMAGE) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_message_sent_image, parent, false);
            return new SentImageViewHolder(view);
        } else if (viewType == VIEW_TYPE_RECEIVED_TEXT) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_message_received, parent, false);
            return new ReceivedTextViewHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.item_message_received_image, parent, false);
            return new ReceivedImageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message message = messages.get(position);

        if (holder instanceof SentTextViewHolder) {
            ((SentTextViewHolder) holder).bind(message);
        } else if (holder instanceof SentImageViewHolder) {
            ((SentImageViewHolder) holder).bind(message);
        } else if (holder instanceof ReceivedTextViewHolder) {
            ((ReceivedTextViewHolder) holder).bind(message);
        } else if (holder instanceof ReceivedImageViewHolder) {
            ((ReceivedImageViewHolder) holder).bind(message);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    // ViewHolder classes for different message types
    class SentTextViewHolder extends RecyclerView.ViewHolder {
        TextView messageText, timeText;

        SentTextViewHolder(View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.messageText);
            timeText = itemView.findViewById(R.id.messageTime);
        }

        void bind(Message message) {
            messageText.setText(message.getText());
            timeText.setText(formatTime(message.getTimestamp()));
        }
    }

    class SentImageViewHolder extends RecyclerView.ViewHolder {
        ImageView messageImage;
        TextView timeText;

        SentImageViewHolder(View itemView) {
            super(itemView);
            messageImage = itemView.findViewById(R.id.messageImage);
            timeText = itemView.findViewById(R.id.messageTime);
        }

        void bind(Message message) {
            // Load image using Glide
            Glide.with(context)
                    .load(message.getText())
                    .placeholder(R.drawable.ic_image_placeholder)
                    .error(R.drawable.ic_image_error)
                    .into(messageImage);

            timeText.setText(formatTime(message.getTimestamp()));

            // Add click listener to open full image
            messageImage.setOnClickListener(v -> {
                Intent intent = new Intent(context, ImageViewerActivity.class);
                intent.putExtra("image_url", message.getText());
                context.startActivity(intent);
            });
        }
    }

    class ReceivedTextViewHolder extends RecyclerView.ViewHolder {
        TextView messageText, timeText;

        ReceivedTextViewHolder(View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.messageText);
            timeText = itemView.findViewById(R.id.messageTime);
        }

        void bind(Message message) {
            messageText.setText(message.getText());
            timeText.setText(formatTime(message.getTimestamp()));
        }
    }

    class ReceivedImageViewHolder extends RecyclerView.ViewHolder {
        ImageView messageImage;
        TextView timeText;

        ReceivedImageViewHolder(View itemView) {
            super(itemView);
            messageImage = itemView.findViewById(R.id.messageImage);
            timeText = itemView.findViewById(R.id.messageTime);
        }

        void bind(Message message) {
            timeText.setText(formatTime(message.getTimestamp()));

            if (message.getText().startsWith("data:image")) {
                // It's a Base64 image
                String base64Image = message.getText().substring(message.getText().indexOf(",") + 1);
                byte[] decodedString = Base64.decode(base64Image, Base64.DEFAULT);
                Bitmap decodedBitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                messageImage.setImageBitmap(decodedBitmap);
            } else {
                // Try to load it as a URL (in case you switch to Storage later)
                Glide.with(context)
                        .load(message.getText())
                        .into(messageImage);
            }
        }
    }

    private String formatTime(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }
}