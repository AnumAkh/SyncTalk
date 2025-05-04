package com.example.synctalk.services;

import android.net.Uri;
import com.example.synctalk.models.Message;
import java.util.List;

public interface FirebaseService {
    void getMessages(String chatId, OnMessagesLoadedListener listener);
    void sendMessage(String chatId, Message message);
    void uploadImage(Uri imageUri, OnImageUploadedListener listener);

    interface OnMessagesLoadedListener {
        void onMessagesLoaded(List<Message> messages);
    }

    interface OnImageUploadedListener {
        void onImageUploaded(String imageUrl);
    }
}