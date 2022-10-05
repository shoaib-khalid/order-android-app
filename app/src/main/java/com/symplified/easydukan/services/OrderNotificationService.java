package com.symplified.easydukan.services;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.TaskStackBuilder;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.symplified.easydukan.App;
import com.symplified.easydukan.OrdersActivity;
import com.symplified.easydukan.R;
import com.symplified.easydukan.apis.LoginApi;
import com.symplified.easydukan.models.HttpResponse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class OrderNotificationService extends FirebaseMessagingService {


    @Override
    public void onNewToken(@NonNull String s) {
        Log.d("FIREBASE_SERVICE","Token refresh : " + s);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {

        String messageTitle = remoteMessage.getData().get("title");
        SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences(App.SESSION_DETAILS_TITLE, MODE_PRIVATE);
        String clientId = sharedPreferences.getString("ownerId", null);

        if (clientId != null
                && "heartbeat".equalsIgnoreCase(messageTitle)) {

            String baseUrl = sharedPreferences.getString("base_url", App.BASE_URL);
            Retrofit retrofit = new Retrofit.Builder()
                    .client(new OkHttpClient())
                    .baseUrl(baseUrl+App.USER_SERVICE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            LoginApi userService = retrofit.create(LoginApi.class);

            String transactionId = remoteMessage.getData().get("body");

            Map<String, String> headers = new HashMap<>();
            headers.put("Authorization", "Bearer Bearer accessToken");

            Call<HttpResponse> pingRequest = userService.ping(headers, clientId, transactionId);
            pingRequest.clone().enqueue(new Callback<HttpResponse>() {
                @Override
                public void onResponse(@NonNull Call<HttpResponse> call, @NonNull Response<HttpResponse> response) {
                }

                @Override
                public void onFailure(@NonNull Call<HttpResponse> call, @NonNull Throwable t) {
                }
            });
        } else {
            Intent toOrdersActivity = new Intent(this, OrdersActivity.class);
            TaskStackBuilder taskStackBuilder = TaskStackBuilder.create(this);
            taskStackBuilder.addNextIntentWithParentStack(toOrdersActivity);
            PendingIntent pendingIntent;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                pendingIntent = taskStackBuilder.getPendingIntent(0, PendingIntent.FLAG_IMMUTABLE);
            } else {
                pendingIntent = taskStackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
            }

            Notification notification = new NotificationCompat.Builder(this, App.CHANNEL_ID)
                    .setContentIntent(pendingIntent)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(remoteMessage.getData().get("title"))
                    .setContentText(remoteMessage.getData().get("body"))
                    .setAutoCancel(false)
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setCategory(NotificationCompat.CATEGORY_ALARM)
                    .setColor(Color.CYAN)
                    .build();

            notification.flags |= Notification.FLAG_AUTO_CANCEL;

            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            notificationManager.notify(new Random().nextInt(), notification);

            // && !isAppOnForeground(getApplicationContext(), getPackageName())
            if (!AlertService.isPlaying()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(new Intent(this, AlertService.class));
                } else {
                    startService(new Intent(this, AlertService.class));
                }
            }
        }

    }

    public boolean isServiceRunning(){
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (AlertService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private boolean isAppOnForeground(Context context,String appPackageName) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
        if (appProcesses == null) {
            return false;
        }
        final String packageName = appPackageName;
        for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
            if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND && appProcess.processName.equals(packageName)) {
                //                Log.e("app",appPackageName);
                return true;
            }
        }
        return false;
    }
}
