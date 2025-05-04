package com.example.synctalk.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.synctalk.R;
import com.example.synctalk.models.Message;
import com.example.synctalk.adapters.MessageAdapter;
import com.example.synctalk.services.FirebaseService;
import com.example.synctalk.services.FirebaseServiceImpl;
import com.example.synctalk.services.WebSocketService;
import com.example.synctalk.services.WebSocketServiceImpl;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private RecyclerView recyclerView;
    private EditText messageInput;
    private ImageButton sendButton;
    private ImageButton attachButton;
    private TextView chatTitle;
    private MessageAdapter messageAdapter;
    private List<Message> messageList;

    // User IDs will be set in configureUserIdentity()
    private String currentUserId;
    private String otherUserId;
    private String chatId = "user1_user2"; // This stays consistent

    private FirebaseService firebaseService;
    private WebSocketService webSocketService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize views
        recyclerView = findViewById(R.id.recyclerView);
        messageInput = findViewById(R.id.messageInput);
        sendButton = findViewById(R.id.sendButton);
        attachButton = findViewById(R.id.attachButton);
        chatTitle = findViewById(R.id.chatTitle);

        // Configure user identity based on device
        configureUserIdentity();

        // Initialize services
        firebaseService = new FirebaseServiceImpl();
        webSocketService = new WebSocketServiceImpl(currentUserId);

        // Initialize message list and adapter
        messageList = new ArrayList<>();
        messageAdapter = new MessageAdapter(this, messageList, currentUserId);

        // Set up RecyclerView
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(messageAdapter);

        // Load messages from Firebase
        loadMessages();

        // Set up WebSocket for real-time updates
        setupWebSocket();

        // Set click listeners
        sendButton.setOnClickListener(v -> sendTextMessage());
        attachButton.setOnClickListener(v -> openFileChooser());
    }

    private void configureUserIdentity() {
        // Get complete device information for more reliable identification
        String deviceModel = android.os.Build.MODEL;
        String deviceManufacturer = android.os.Build.MANUFACTURER;
        String deviceName = android.os.Build.DEVICE;

        // For debugging - log the full device info
        Log.d("DeviceInfo", "Model: " + deviceModel +
                ", Manufacturer: " + deviceManufacturer +
                ", Device Name: " + deviceName);

        // Use exact model name comparison rather than contains()
        if (deviceModel.equals("Pixel 8")) {
            currentUserId = "user1"; // Anum
            otherUserId = "user2"; // Aiman
            if (chatTitle != null) {
                chatTitle.setText("Chat with Aiman (as Anum)");
            }
        } else if (deviceModel.equals("Medium Phone API 36")) {
            currentUserId = "user2"; // Aiman
            otherUserId = "user1"; // Anum
            if (chatTitle != null) {
                chatTitle.setText("Chat with Anum (as Aiman)");
            }
        } else {
            // Fallback logic - use any unique device identifier
            // Using device name as a unique identifier
            if (deviceName.hashCode() % 2 == 0) {
                currentUserId = "user1"; // Anum
                otherUserId = "user2"; // Aiman
                if (chatTitle != null) {
                    chatTitle.setText("Chat with Aiman (as Anum)");
                }
            } else {
                currentUserId = "user2"; // Aiman
                otherUserId = "user1"; // Anum
                if (chatTitle != null) {
                    chatTitle.setText("Chat with Anum (as Aiman)");
                }
            }
        }

        // Log the final result
        Log.d("UserIdentity", "Device: " + deviceModel +
                ", Assigned User: " + (currentUserId.equals("user1") ? "Anum" : "Aiman"));
    }

    private void loadMessages() {
        firebaseService.getMessages(chatId, messages -> {
            messageList.clear();
            messageList.addAll(messages);
            messageAdapter.notifyDataSetChanged();
            recyclerView.scrollToPosition(messageList.size() - 1);
        });
    }

    private void setupWebSocket() {
        // Connect to WebSocket server
        Log.d(null, "going in web socket");
        webSocketService.connect("wss://your-websocket-server.com");

        // Listen for incoming messages
        webSocketService.setMessageListener(message -> {
            // Only add if it's a message for this chat
            runOnUiThread(() -> {
                messageList.add(message);
                messageAdapter.notifyItemInserted(messageList.size() - 1);
                recyclerView.scrollToPosition(messageList.size() - 1);
            });
        });
    }

    private void sendTextMessage() {
        String text = messageInput.getText().toString().trim();
        if (!text.isEmpty()) {
            // Create a new message
            Message message = new Message();
            message.setSenderId(currentUserId);
            message.setText(text);
            message.setTimestamp(System.currentTimeMillis());
            message.setType("text");

            // Send to Firebase
            firebaseService.sendMessage(chatId, message);

            // Send via WebSocket for instant delivery
            webSocketService.sendMessage(message);

            // Clear input
            messageInput.setText("");
        }
    }

    private void openFileChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Image"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            uploadAndSendImage(imageUri);
        }
    }

    private void uploadAndSendImage(Uri imageUri) {
        firebaseService.uploadImage(imageUri, imageUrl -> {
            // Create image message
            Message message = new Message();
            message.setSenderId(currentUserId);
            message.setText(imageUrl);
            message.setTimestamp(System.currentTimeMillis());
            message.setType("image");

            // Send to Firebase
            firebaseService.sendMessage(chatId, message);

            // Send via WebSocket
            webSocketService.sendMessage(message);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Close WebSocket connection
        if (webSocketService != null) {
            webSocketService.disconnect();
        }
    }
}