Real-time Instant Messaging for Android
SyncTalk is a simple Android messaging application that demonstrates basic networking concepts including WebSockets, HTTP communication, and Firebase integration. This project was developed as a learning exercise to implement fundamental real-time communication techniques.

Features

Real-time Messaging: Exchange messages instantly using WebSockets
Message History: View past conversations stored in Firebase
Push Notifications: Get notified of new messages when the app is in background
Image Sharing: Send and receive images in conversations

Technologies Used

Android Studio: Primary development environment
Java: Main programming language
OkHttp: For WebSocket implementation
Firebase Realtime Database: For message persistence
Glitch: For hosting the WebSocket server

Architecture
SyncTalk uses a hybrid networking approach:

WebSockets for low-latency real-time message delivery
Firebase for data persistence and authentication
