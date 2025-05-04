package com.example.synctalk.services;

import android.util.Log;
import com.example.synctalk.models.Message;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class WebSocketServiceImpl implements WebSocketService {
    private WebSocket webSocket;
    private String userId;
    private MessageListener messageListener;

    public WebSocketServiceImpl(String userId) {
        this.userId = userId;
    }

    @Override
    public void connect(String serverUrl) {
        OkHttpClient client = new OkHttpClient.Builder()
                .readTimeout(0, TimeUnit.MILLISECONDS)
                .build();

        Request request = new Request.Builder()
                .url(serverUrl)
                .build();

        webSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                // Send authentication
                try {
                    JSONObject authMessage = new JSONObject();
                    authMessage.put("type", "auth");
                    authMessage.put("userId", userId);
                    webSocket.send(authMessage.toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                try {
                    JSONObject jsonMessage = new JSONObject(text);

                    if (jsonMessage.getString("type").equals("message")) {
                        // Convert JSON to Message object
                        Message message = new Message();
                        message.setId(jsonMessage.optString("id"));
                        message.setSenderId(jsonMessage.getString("senderId"));
                        message.setText(jsonMessage.getString("text"));
                        message.setTimestamp(jsonMessage.getLong("timestamp"));
                        message.setType(jsonMessage.getString("type"));

                        // Notify listener
                        if (messageListener != null) {
                            messageListener.onMessageReceived(message);
                        }
                    }
                } catch (JSONException e) {
                    Log.e("WebSocketService", "JSON parsing error", e);
                }
            }

            @Override
            public void onClosing(WebSocket webSocket, int code, String reason) {
                webSocket.close(1000, null);
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                Log.e("WebSocketService", "WebSocket failure", t);
                // Consider implementing reconnection logic here
            }
        });
    }

    @Override
    public void sendMessage(Message message) {
        if (webSocket != null) {
            try {
                JSONObject jsonMessage = new JSONObject();
                jsonMessage.put("type", "message");
                jsonMessage.put("id", message.getId());
                jsonMessage.put("senderId", message.getSenderId());
                jsonMessage.put("text", message.getText());
                jsonMessage.put("timestamp", message.getTimestamp());
                jsonMessage.put("messageType", message.getType());

                webSocket.send(jsonMessage.toString());
            } catch (JSONException e) {
                Log.e("WebSocketService", "JSON creation error", e);
            }
        }
    }

    @Override
    public void setMessageListener(MessageListener listener) {
        this.messageListener = listener;
    }

    @Override
    public void disconnect() {
        if (webSocket != null) {
            webSocket.close(1000, "App closing");
        }
    }
}