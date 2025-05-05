package com.example.synctalk.services;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import com.example.synctalk.R;
import com.example.synctalk.activities.MainActivity;
import com.example.synctalk.models.Message;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class FirebaseServiceImpl implements FirebaseService {
    private FirebaseDatabase database;
    private DatabaseReference databaseReference;
    private FirebaseStorage storage;
    private StorageReference storageReference;
    private Context context;  // Added context variable

    // Updated constructor with context parameter
    public FirebaseServiceImpl(Context context) {
        this.context = context;

        // Initialize with correct URL for your region
        database = FirebaseDatabase.getInstance("https://instant-messenger-54aca-default-rtdb.asia-southeast1.firebasedatabase.app");
        databaseReference = database.getReference();
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();
    }

    @Override
    public void getMessages(String chatId, OnMessagesLoadedListener listener) {
        databaseReference.child("chats").child(chatId).child("messages")
                .orderByChild("timestamp")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<Message> messages = new ArrayList<>();
                        for (DataSnapshot messageSnapshot : snapshot.getChildren()) {
                            Message message = messageSnapshot.getValue(Message.class);
                            if (message != null) {
                                message.setId(messageSnapshot.getKey());
                                messages.add(message);
                            }
                        }
                        listener.onMessagesLoaded(messages);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("FirebaseService", "Failed to load messages", error.toException());
                    }
                });
    }

    @Override
    public void sendMessage(String chatId, Message message) {
        // Get reference to message node
        DatabaseReference messagesRef = databaseReference.child("chats").child(chatId).child("messages");

        // Create a unique ID
        String messageId = messagesRef.push().getKey();
        if (messageId != null) {
            message.setId(messageId);

            // Save the message
            messagesRef.child(messageId).setValue(message);

            // Update metadata
            DatabaseReference metadataRef = databaseReference.child("chats").child(chatId).child("metadata");
            Map<String, Object> updates = new HashMap<>();

            String lastMessageText = message.getType().equals("image") ? "📷 Image" : message.getText();
            updates.put("lastMessage", lastMessageText);
            updates.put("lastMessageTime", message.getTimestamp());

            metadataRef.updateChildren(updates);
        }
    }

    @Override
    public void uploadImage(Uri imageUri, OnImageUploadedListener listener) {
        if (context != null) {
            Toast.makeText(context, "Starting upload in service...", Toast.LENGTH_SHORT).show();
        }
        Log.d("FirebaseService", "Starting image upload: " + imageUri);

        // Create a storage reference
        StorageReference fileRef = storageReference.child("chat_images/" + UUID.randomUUID().toString());

        UploadTask uploadTask = fileRef.putFile(imageUri);

        // Add progress listener
        uploadTask.addOnProgressListener(taskSnapshot -> {
            double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
            Log.d("FirebaseService", "Upload progress: " + progress + "%");
        });

        // Handle success
        uploadTask.addOnSuccessListener(taskSnapshot -> {
            Log.d("FirebaseService", "Upload successful");

            // Get the download URL
            fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                String imageUrl = uri.toString();
                Log.d("FirebaseService", "Download URL: " + imageUrl);
                listener.onImageUploaded(imageUrl);
            }).addOnFailureListener(e -> {
                Log.e("FirebaseService", "Failed to get download URL", e);
                listener.onImageUploaded(null); // Notify with null to indicate failure
            });
        });

        // Handle failure
        uploadTask.addOnFailureListener(e -> {
            Log.e("FirebaseService", "Upload failed", e);
            listener.onImageUploaded(null); // Notify with null to indicate failure
        });
    }

    public Bitmap getResizedBitmap(Bitmap image, int maxSize) {
        int width = image.getWidth();
        int height = image.getHeight();

        float bitmapRatio = (float) width / (float) height;
        if (bitmapRatio > 1) {
            width = maxSize;
            height = (int) (width / bitmapRatio);
        } else {
            height = maxSize;
            width = (int) (height * bitmapRatio);
        }

        return Bitmap.createScaledBitmap(image, width, height, true);
    }

}