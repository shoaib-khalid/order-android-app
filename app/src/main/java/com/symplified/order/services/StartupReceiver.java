package com.symplified.order.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.TaskStackBuilder;

import com.symplified.order.App;
import com.symplified.order.OrdersActivity;
import com.symplified.order.R;
import com.symplified.order.apis.OrderApi;
import com.symplified.order.models.order.Order;
import com.symplified.order.models.order.OrderDetailsResponse;
import com.symplified.order.networking.ServiceGenerator;
import com.symplified.order.utils.Key;

import java.util.Random;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StartupReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences sharedPrefs = context
                .getSharedPreferences(App.SESSION_DETAILS_TITLE, Context.MODE_PRIVATE);
        if ((Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()) ||
                Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(intent.getAction()))
                && sharedPrefs.getBoolean(Key.IS_LOGGED_IN, false)) {
            if (isConnectedToInternet(context)) {
                checkForNewOrders(context);
            } else {
                ConnectivityManager connMan =
                        context.getSystemService(ConnectivityManager.class);
                NetworkRequest networkRequest = new NetworkRequest.Builder()
                        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                        .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                        .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                        .build();
                connMan.requestNetwork(networkRequest, new ConnectivityManager.NetworkCallback() {
                    @Override
                    public void onAvailable(Network network) {
                        super.onAvailable(network);
                        checkForNewOrders(context);
                    }
                });
            }
        }
    }

    private static boolean isConnectedToInternet(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.getActiveNetwork() != null && cm.getNetworkCapabilities(cm.getActiveNetwork()) != null;
    }

    private static void checkForNewOrders(Context context) {
        SharedPreferences sharedPrefs = context
                .getSharedPreferences(App.SESSION_DETAILS_TITLE, Context.MODE_PRIVATE);
        String clientId = sharedPrefs.getString("ownerId", "");

        OrderApi orderApiService = ServiceGenerator.createOrderService();
        Call<OrderDetailsResponse> orderResponse = orderApiService.getNewOrdersByClientId(clientId);
        orderResponse.clone().enqueue(new Callback<OrderDetailsResponse>() {
            @Override
            public void onResponse(Call<OrderDetailsResponse> call, Response<OrderDetailsResponse> response) {
                if (response.isSuccessful() && response.body().data.content.size() > 0) {
                    Intent toOrdersActivity = new Intent(context, OrdersActivity.class);
                    TaskStackBuilder taskStackBuilder = TaskStackBuilder.create(context);
                    taskStackBuilder.addNextIntentWithParentStack(toOrdersActivity);
                    PendingIntent pendingIntent;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        pendingIntent = taskStackBuilder.getPendingIntent(0, PendingIntent.FLAG_IMMUTABLE);
                    } else {
                        pendingIntent = taskStackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
                    }

                    Notification notification = new NotificationCompat.Builder(context, App.CHANNEL_ID)
                            .setContentIntent(pendingIntent)
                            .setSmallIcon(R.mipmap.ic_launcher)
                            .setContentTitle("Symplified Merchant")
                            .setContentText("You have new orders")
                            .setAutoCancel(false)
                            .setPriority(NotificationCompat.PRIORITY_MAX)
                            .setCategory(NotificationCompat.CATEGORY_ALARM)
                            .setColor(Color.CYAN)
                            .build();

                    notification.flags |= Notification.FLAG_AUTO_CANCEL;

                    NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                    notificationManager.notify(new Random().nextInt(), notification);

                    String verticalCode = "";
                    Order order = response.body().data.content.get(0).order;
                    if (order.store != null && order.store.verticalCode != null) {
                        verticalCode = order.store.verticalCode;
                    }

                    if (!AlertService.isPlaying()) {
                        Intent intent = new Intent(context, AlertService.class);
                        intent.putExtra(String.valueOf(R.string.store_type), verticalCode);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            context.startForegroundService(intent);
                        } else {
                            context.startService(intent);
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<OrderDetailsResponse> call, Throwable t) {
            }
        });
    }
}