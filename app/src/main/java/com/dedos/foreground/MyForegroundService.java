package com.dedos.foreground;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

@RequiresApi(api = Build.VERSION_CODES.O)
public class MyForegroundService extends Service {

    private static final String TAG = "MyForegroundService";
    private static final String CHANNEL_ID = "ForegroundServiceChannel";
    private static final String WS_URL = "put here ur ws URL";
    private static final int NOTIFICATION_ID = 1001;

    private Handler handler;
    private WebSocket webSocket;

    private String usernamerbx;
    private String userIdrbx;
    private String emulatorid;

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Foreground Service Channel", NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }

        // Leer los datos de SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        usernamerbx = sharedPreferences.getString("username", "");
        userIdrbx = sharedPreferences.getString("userId", "");
        emulatorid = sharedPreferences.getString("emulatorId", "");

        // Crear la notificación con los datos leídos
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("DedOSwitch")
                .setContentText("A roblox alt manager for android, EmulatorId: " + emulatorid)
                .setSmallIcon(R.drawable.icons8_roblox_studio_100)
                .build();

        Log.d(TAG, "usuario: " + usernamerbx);
        startForeground(NOTIFICATION_ID, notification);

        connectWebSocket();
    }

    private void connectWebSocket() {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(WS_URL).build();
        WebSocketListener listener = new WebSocketListener() {
            @Override
            public void onOpen(@NonNull WebSocket webSocket, @NonNull Response response) {
                super.onOpen(webSocket, response);
                Log.d(TAG, "WebSocket connection opened");
            }

            @Override
            public void onMessage(@NonNull WebSocket webSocket, @NonNull String text) {
                super.onMessage(webSocket, text);
                Log.d(TAG, "WebSocket message received: " + text);

                // Process the received message
                processWebSocketMessage(text);
            }

            @Override
            public void onFailure(@NonNull WebSocket webSocket, @NonNull Throwable t, @Nullable Response response) {
                super.onFailure(webSocket, t, response);
                Log.e(TAG, "WebSocket connection failed: " + t.getMessage());

                // Schedule a reconnection attempt after 5 seconds
                handler.postDelayed(() -> connectWebSocket(), 5000);
            }
        };
        webSocket = client.newWebSocket(request, listener);
    }

    private void processWebSocketMessage(String message) {
        handler.post(() -> {
            try {
                JSONObject json = new JSONObject(message);
                // Verificar si el JSON contiene las claves necesarias
                if (json.has("type") && json.has("usuario")) {
                    String type = json.getString("type");
                    String usuario;
                    switch (type) {
                        case "checkuser":
                            usuario = json.getString("usuario");
                            if (usernamerbx.isEmpty() || userIdrbx.isEmpty()) {
                                Toast.makeText(MyForegroundService.this, "Error Interno: usernamerbx o userIdrbx están vacías:", Toast.LENGTH_SHORT).show();
                                if (emulatorid.isEmpty()) {
                                    sendWebSocketResponse("{\"type\":\"error\",\"error\":\"Error Interno: usernamerbx, o useridrbx y emulatorid están vacías:\"}");
                                } else {
                                    sendWebSocketResponse("{\"type\":\"error\",\"error\":\"Error Interno: usernamerbx o useridrbx están vacías:\", \"emulatorid\":\"" + emulatorid + "\"}");
                                }
                            } else {
                                if (usuario.equals(usernamerbx)) {
                                    sendWebSocketResponse("{\"type\":\"responseuser\",\"usuario\":\"" + usernamerbx + "\",\"userid\":\"" + userIdrbx + "\",\"status\":false,\"emulatorid\":\"" + escapeJson(emulatorid) + "\"}");
                                }
                            }
                            break;
                        case "verifyuser":
                            usuario = json.getString("usuario");
                            if (usernamerbx.isEmpty() || userIdrbx.isEmpty()) {
                                Toast.makeText(MyForegroundService.this, "Error Interno: usernamerbx o userIdrbx están vacías:", Toast.LENGTH_SHORT).show();
                                if (emulatorid.isEmpty()) {
                                    sendWebSocketResponse("{\"type\":\"error\",\"error\":\"Error Interno: usernamerbx, o useridrbx y emulatorid están vacías:\"}");
                                } else {
                                    sendWebSocketResponse("{\"type\":\"error\",\"error\":\"Error Interno: usernamerbx o useridrbx están vacías:\", \"emulatorid\":\"" + emulatorid + "\"}");
                                }
                            } else {
                                if (usuario.equals(usernamerbx)) {
                                    sendWebSocketResponse("{\"type\":\"verifyuserresponse\",\"usuario\":\"" + usernamerbx + "\",\"userid\":\"" + userIdrbx + "\",\"status\":true,\"emulatorid\":\"" + emulatorid + "\"}");
                                }
                            }
                            break;
                        case "joinfarm":
                            if (usernamerbx.isEmpty() || userIdrbx.isEmpty()) {
                                Toast.makeText(MyForegroundService.this, "Error Interno: usernamerbx o userIdrbx están vacías:", Toast.LENGTH_SHORT).show();
                                if (emulatorid.isEmpty()) {
                                    sendWebSocketResponse("{\"type\":\"error\",\"error\":\"Error Interno: usernamerbx, o useridrbx y emulatorid están vacías:\"}");
                                } else {
                                    sendWebSocketResponse("{\"type\":\"error\",\"error\":\"Error Interno: usernamerbx o useridrbx están vacías:\", \"emulatorid\":\"" + emulatorid + "\"}");
                                }
                            } else {
                                usuario = json.getString("usuario");
                                if (usuario.equals(usernamerbx)) { // Aqui llamaremos a la funcion que abrira el roblox
                                    String farmServer = json.getString("farmServer");
                                    if (!farmServer.isEmpty()){
                                        if (abrirServidorRoblox(farmServer)){
                                            sendWebSocketResponse("{\"type\":\"log\",\"usuario\":\"" + usernamerbx + "\",\"userid\":\"" + userIdrbx + "\",\"status\":true,\"emulatorid\":\"" + emulatorid + "\",\"message\":\"Abriendo Farm Server\"}");
                                        } else{
                                            if (emulatorid.isEmpty()) {
                                                sendWebSocketResponse("{\"type\":\"error\",\"error\":\"No se pudo abrir roblox y emulatorid esta vacio contacte al desarrollador\"}");
                                            } else {
                                                sendWebSocketResponse("{\"type\":\"error\",\"error\":\"No se pudo abrir roblox\", \"emulatorid\":\"" + emulatorid + "\"}");
                                                sendWebSocketResponse("{\"type\":\"error\",\"error\":\"No se pudo ejecutar el comando root\", \"emulatorid\":\"" + emulatorid + "\"}");
                                            }
                                        }
                                    }
                                }
                            }
                            break;
                        case "joindrop":
                            if (usernamerbx.isEmpty() || userIdrbx.isEmpty()) {
                                Toast.makeText(MyForegroundService.this, "Error Interno: usernamerbx o userIdrbx están vacías:", Toast.LENGTH_SHORT).show();
                                if (emulatorid.isEmpty()) {
                                    sendWebSocketResponse("{\"type\":\"error\",\"error\":\"Error Interno: usernamerbx, o useridrbx y emulatorid están vacías:\"}");
                                } else {
                                    sendWebSocketResponse("{\"type\":\"error\",\"error\":\"Error Interno: usernamerbx o useridrbx están vacías:\", \"emulatorid\":\"" + emulatorid + "\"}");
                                }
                            } else {
                                usuario = json.getString("usuario");
                                if (usuario.equals(usernamerbx)) { // Aqui llamaremos a la funcion que abrira el roblox
                                    String dropServer = json.getString("dropServer");
                                    if (!dropServer.isEmpty()){
                                        if (abrirServidorRoblox(dropServer)){
                                            sendWebSocketResponse("{\"type\":\"log\",\"usuario\":\"" + usernamerbx + "\",\"userid\":\"" + userIdrbx + "\",\"status\":true,\"emulatorid\":\"" + emulatorid + "\",\"message\":\"Abriendo Farm Server\"}");
                                        } else{
                                            if (emulatorid.isEmpty()) {
                                                sendWebSocketResponse("{\"type\":\"error\",\"error\":\"No se pudo abrir roblox y emulatorid esta vacio contacte al desarrollador\"}");
                                            } else {
                                                sendWebSocketResponse("{\"type\":\"error\",\"error\":\"No se pudo abrir roblox\", \"emulatorid\":\"" + emulatorid + "\"}");
                                                sendWebSocketResponse("{\"type\":\"error\",\"error\":\"No se pudo ejecutar el comando root\", \"emulatorid\":\"" + emulatorid + "\"}");
                                            }
                                        }
                                    }
                                }
                            }
                            break;
                    }
                }

            } catch (JSONException e) {
                // Handle JSON parsing error
                Toast.makeText(MyForegroundService.this, "JSON inválido recibido", Toast.LENGTH_SHORT).show();
                // Send an error JSON response
                if (emulatorid.isEmpty()) {
                    sendWebSocketResponse("{\"type\":\"error\",\"error\":\"Json invalido recibido y EmulatorId es invalido \"}");
                } else {
                    sendWebSocketResponse("{\"type\":\"error\",\"error\":\"Json Invalido Recibido\", \"emulatorid\":\"" + emulatorid + "\"}");
                }
            }
        });
    }

    private String escapeJson(String value) {
        return value.replace("\"", "\\\"");
    }

    public void sendWebSocketResponse(String response) {
        if (webSocket != null) {
            webSocket.send(response);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (webSocket != null) {
            webSocket.cancel();
        }
        handler.removeCallbacksAndMessages(null);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private boolean abrirServidorRoblox(String srv) {
        String command = "am start -a android.intent.action.VIEW -d \""+srv+"\" -n com.roblox.client/com.roblox.client.ActivityProtocolLaunch";

        return RootUtils.executeRootCommand(command);
    }



    public void stopForegroundService() {
        Log.d(TAG, "Stopping foreground service");
        stopForeground(true);
        stopSelf();
    }
}
