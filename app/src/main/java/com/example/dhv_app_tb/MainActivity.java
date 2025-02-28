package com.example.dhv_app_tb;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MQTTNotification";
    // Chú ý sử dụng "tcp://" ở đầu URL
    private static final String BROKER_URL = "tcp://192.168.0.104:1883";
    private static final String CLIENT_ID = "AndroidClient-" + System.currentTimeMillis();
    private static final String TOPIC = "tb";

    private static final String CHANNEL_ID = "notification_channel";
    private static final int NOTIFICATION_ID = 1;

    private TextView titleTextView;
    private TextView contentTextView;
    private MqttClient mqttClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        titleTextView = findViewById(R.id.titleTextView);
        contentTextView = findViewById(R.id.contentTextView);

        createNotificationChannel();

        // Kiểm tra xem activity được mở từ thông báo không
        if (getIntent().hasExtra("notification_title") && getIntent().hasExtra("notification_content")) {
            String title = getIntent().getStringExtra("notification_title");
            String content = getIntent().getStringExtra("notification_content");
            titleTextView.setText(title);
            contentTextView.setText(content);
        }

        // Kết nối MQTT
        connectMqtt();
    }

    private void connectMqtt() {
        try {
            Log.d(TAG, "Bắt đầu kết nối đến MQTT broker: " + BROKER_URL);

            mqttClient = new MqttClient(BROKER_URL, CLIENT_ID, new MemoryPersistence());
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);

            mqttClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    Log.e(TAG, "Mất kết nối MQTT: " + cause.getMessage());
                    runOnUiThread(() -> Toast.makeText(MainActivity.this,
                            "Mất kết nối MQTT: " + cause.getMessage(), Toast.LENGTH_SHORT).show());
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    String payload = new String(message.getPayload());
                    Log.d(TAG, "Nhận tin nhắn MQTT từ topic '" + topic + "': " + payload);

                    // Hiển thị toast để xác nhận đã nhận tin nhắn
                    runOnUiThread(() -> Toast.makeText(MainActivity.this,
                            "Đã nhận tin nhắn MQTT", Toast.LENGTH_SHORT).show());

                    // Xử lý JSON
                    processJsonMessage(payload);
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    // Không cần xử lý
                }
            });

            mqttClient.connect(options);
            mqttClient.subscribe(TOPIC);
            Log.d(TAG, "Đã kết nối MQTT thành công và đăng ký topic: " + TOPIC);

            // Hiển thị toast để xác nhận kết nối thành công
            runOnUiThread(() -> Toast.makeText(MainActivity.this,
                    "Đã kết nối MQTT và đăng ký topic: " + TOPIC, Toast.LENGTH_SHORT).show());

        } catch (MqttException e) {
            Log.e(TAG, "Lỗi MQTT: " + e.getMessage(), e);
            Toast.makeText(this, "Lỗi kết nối MQTT: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void processJsonMessage(String jsonMessage) {
        try {
            Log.d(TAG, "Bắt đầu phân tích JSON: " + jsonMessage);

            // Làm sạch chuỗi JSON nếu cần
            jsonMessage = jsonMessage.trim();

            // Phân tích JSON để lấy title và content
            JSONObject jsonObject = new JSONObject(jsonMessage);

            if (jsonObject.has("title") && jsonObject.has("content")) {
                String title = jsonObject.getString("title");
                String content = jsonObject.getString("content");

                Log.d(TAG, "Phân tích JSON thành công: title='" + title + "', content='" + content + "'");

                // Hiển thị thông báo với dữ liệu từ JSON
                showNotification(title, content);

                // Cập nhật UI nếu ứng dụng đang hiển thị
                runOnUiThread(() -> {
                    titleTextView.setText(title);
                    contentTextView.setText(content);
                    Toast.makeText(MainActivity.this,
                            "Đã cập nhật UI với dữ liệu mới", Toast.LENGTH_SHORT).show();
                });
            } else {
                Log.e(TAG, "JSON không chứa các trường title và content cần thiết");
                runOnUiThread(() -> Toast.makeText(MainActivity.this,
                        "JSON không có title hoặc content", Toast.LENGTH_SHORT).show());
            }

        } catch (JSONException e) {
            Log.e(TAG, "Lỗi phân tích JSON: " + e.getMessage() + "\nJSON string: " + jsonMessage, e);
            runOnUiThread(() -> Toast.makeText(MainActivity.this,
                    "Lỗi phân tích JSON: " + e.getMessage(), Toast.LENGTH_LONG).show());
        }
    }

    private void showNotification(String title, String content) {
        Log.d(TAG, "Hiển thị thông báo: title='" + title + "', content='" + content + "'");

        // Tạo intent để mở activity khi click vào thông báo
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("notification_title", title);
        intent.putExtra("notification_content", content);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Tạo thông báo
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setVibrate(new long[]{0, 500, 200, 500}) // Mẫu rung
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        // Hiển thị thông báo
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_ID, builder.build());
            Log.d(TAG, "Thông báo đã được gửi đến notification manager");
        } else {
            Log.e(TAG, "Không thể lấy notification manager");
        }

        // Kích hoạt rung
        vibrate();
    }

    private void vibrate() {
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(500);
            }
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Thông báo";
            String description = "Kênh thông báo từ MQTT";
            int importance = NotificationManager.IMPORTANCE_HIGH;

            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            channel.enableVibration(true);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
                Log.d(TAG, "Đã tạo notification channel");
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mqttClient != null && mqttClient.isConnected()) {
            try {
                mqttClient.disconnect();
                Log.d(TAG, "Đã ngắt kết nối MQTT");
            } catch (MqttException e) {
                Log.e(TAG, "Lỗi khi ngắt kết nối MQTT: " + e.getMessage(), e);
            }
        }
    }
}