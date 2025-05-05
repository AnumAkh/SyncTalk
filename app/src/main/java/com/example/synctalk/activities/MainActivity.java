package com.example.synctalk.activities;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Base64;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

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

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
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

    // Add this debug method to your MainActivity
    private void logImageDetails(Uri imageUri) {
        try {
            ContentResolver contentResolver = getContentResolver();
            String mimeType = contentResolver.getType(imageUri);
            Cursor cursor = contentResolver.query(imageUri, null, null, null, null);
            int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
            cursor.moveToFirst();
            long size = cursor.getLong(sizeIndex);
            cursor.close();

            Log.d("ImageDebug", "Selected image: " + imageUri.toString());
            Log.d("ImageDebug", "MIME type: " + mimeType);
            Log.d("ImageDebug", "File size: " + size + " bytes");
        } catch (Exception e) {
            Log.e("ImageDebug", "Error getting image details", e);
        }
    }

    // Add this call to your onActivityResult method
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            logImageDetails(imageUri);
            Toast.makeText(this, "Image selected, uploading...", Toast.LENGTH_SHORT).show();
            uploadAndSendImage(imageUri);
        }
    }
    private void uploadAndSendImage(Uri imageUri) {
        try {
            // Show a loading indicator
            Toast.makeText(this, "Processing image...", Toast.LENGTH_SHORT).show();

            // Convert the image to Base64
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

            // Compress and resize the image to reduce size
            Bitmap resizedBitmap = getResizedBitmap(bitmap, 800); // Max width/height 800px

            // Convert to Base64
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 70, byteArrayOutputStream);
            byte[] byteArray = byteArrayOutputStream.toByteArray();
            String base64Image = "data:image/jpeg;base64," + Base64.encodeToString(byteArray, Base64.DEFAULT);

            // Create image message
            Message message = new Message();
            message.setSenderId(currentUserId);
            message.setText(base64Image);
            message.setTimestamp(System.currentTimeMillis());
            message.setType("image");

            // Send to Firebase Realtime Database
            firebaseService.sendMessage(chatId, message);

            // Clear any resources
            inputStream.close();
            byteArrayOutputStream.close();

            Toast.makeText(this, "Image sent!", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Log.e("MainActivity", "Error processing image", e);
            Toast.makeText(this, "Failed to process image", Toast.LENGTH_SHORT).show();
        }
    }

    // Helper method to resize images
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Close WebSocket connection
        if (webSocketService != null) {
            webSocketService.disconnect();
        }
    }
}