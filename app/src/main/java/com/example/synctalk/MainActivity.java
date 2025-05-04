package com.example.synctalk;

import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Firebase;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        createChatBetweenUsers();

//        FirebaseDatabase database = FirebaseDatabase.getInstance("https://instant-messenger-54aca-default-rtdb.asia-southeast1.firebasedatabase.app");
//        DatabaseReference databaseReference = database.getReference();
//
//        databaseReference.child("message").setValue("Hello World!").addOnCompleteListener(new OnCompleteListener<Void>() {
//            @Override
//            public void onComplete(@NonNull Task<Void> task) {
//                if (task.isSuccessful()) {
//                    Log.d("Firebase", "Data written successfully");
//                } else {
//                    Log.e("Firebase", "Error writing data", task.getException());
//                }
//            }
//        });
    }

    private void createChatBetweenUsers() {
        // Get database reference with the correct URL
        FirebaseDatabase database = FirebaseDatabase.getInstance("https://instant-messenger-54aca-default-rtdb.asia-southeast1.firebasedatabase.app");
        DatabaseReference rootRef = database.getReference();

        // Define user IDs based on your existing users
        String user1Id = "user1"; // Anum
        String user2Id = "user2"; // Aiman

        // Create a chat ID by combining user IDs
        String chatId = user1Id + "_" + user2Id;

        // Reference to the chat node
        DatabaseReference chatRef = rootRef.child("chats").child(chatId);

        // Create message references
        DatabaseReference messagesRef = chatRef.child("messages");

        // Add first message from Anum to Aiman
        String message1Id = messagesRef.push().getKey();
        Map<String, Object> message1Data = new HashMap<>();
        message1Data.put("senderId", user1Id);
        message1Data.put("text", "Hello Aiman, how are you?");
        message1Data.put("timestamp", System.currentTimeMillis());
        message1Data.put("type", "text");
        messagesRef.child(message1Id).setValue(message1Data);

        // Add reply from Aiman to Anum
        String message2Id = messagesRef.push().getKey();
        Map<String, Object> message2Data = new HashMap<>();
        message2Data.put("senderId", user2Id);
        message2Data.put("text", "Hi Anum! I'm doing well, thanks for asking.");
        message2Data.put("timestamp", System.currentTimeMillis() + 60000); // 1 minute later
        message2Data.put("type", "text");
        messagesRef.child(message2Id).setValue(message2Data);

        // Create and set metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("lastMessage", "Hi Anum! I'm doing well, thanks for asking.");
        metadata.put("lastMessageTime", System.currentTimeMillis() + 60000);

        // Add participants
        List<String> participants = new ArrayList<>();
        participants.add(user1Id);
        participants.add(user2Id);
        metadata.put("participants", participants);

        // Add metadata to database
        chatRef.child("metadata").setValue(metadata);
    }
}