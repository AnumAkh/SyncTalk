package com.example.synctalk.services;

import android.net.Uri;
import android.util.Log;
import com.example.synctalk.models.Message;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import androidx.annotation.NonNull;

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

    public FirebaseServiceImpl() {
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
        // Create a unique filename
        String filename = "chat_images/" + UUID.randomUUID().toString();
        StorageReference imageRef = storageReference.child(filename);

        // Upload file
        imageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    // Get download URL
                    imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        String imageUrl = uri.toString();
                        listener.onImageUploaded(imageUrl);
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e("FirebaseService", "Image upload failed", e);
                });
    }
}