package com.example.synctalk.services;

import com.example.synctalk.models.Message;

public interface WebSocketService {
    void connect(String serverUrl);
    void disconnect();
    void sendMessage(Message message);
    void setMessageListener(MessageListener listener);

    interface MessageListener {
        void onMessageReceived(Message message);
    }
}