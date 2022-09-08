package com.symplified.order.services;

import static com.symplified.order.App.CHANNEL_ID;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.TaskStackBuilder;

import com.google.android.material.transition.platform.MaterialContainerTransformSharedElementCallback;
import com.google.gson.Gson;
import com.symplified.order.App;
import com.symplified.order.OrdersActivity;
import com.symplified.order.R;
import com.symplified.order.apis.OrderApi;
import com.symplified.order.apis.StoreApi;
import com.symplified.order.models.Store.StoreResponse;
import com.symplified.order.models.order.Order;
import com.symplified.order.models.order.OrderDetailsResponse;
import com.symplified.order.networking.ServiceGenerator;
import com.symplified.order.utils.Keys;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import okhttp3.ResponseBody;
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
                && sharedPrefs.getBoolean(Keys.IS_LOGGED_IN, false)) {
            if (isConnectedToInternet(context)) {
                Toast.makeText(context, "Connected to network", Toast.LENGTH_SHORT).show();
                checkForNewOrders(context);
            } else {
                Toast.makeText(context, "No network detected. Registering network callback", Toast.LENGTH_SHORT).show();
                ConnectivityManager connMan =
                        (ConnectivityManager) context.getSystemService(ConnectivityManager.class);
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

    private void playAlert(Context context) {
        Intent alertService = new Intent(context, AlertService.class);
        alertService.putExtra("first", 1);
        alertService.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(alertService);
        } else {
            context.startService(alertService);
        }
    }

    private static boolean isConnectedToInternet(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.getActiveNetwork() != null && cm.getNetworkCapabilities(cm.getActiveNetwork()) != null;
    }

    private static void checkForNewOrders(Context context) {
        Toast.makeText(context, "Checking for new orders", Toast.LENGTH_LONG).show();

        SharedPreferences sharedPrefs = context
                .getSharedPreferences(App.SESSION_DETAILS_TITLE, Context.MODE_PRIVATE);
        String clientId = sharedPrefs.getString("ownerId", "");

        OrderApi orderApiService = ServiceGenerator.createOrderService();
        Call<OrderDetailsResponse> orderResponse = orderApiService.getNewOrdersByClientId(clientId);
        orderResponse.clone().enqueue(new Callback<OrderDetailsResponse>() {
            @Override
            public void onResponse(Call<OrderDetailsResponse> call, Response<OrderDetailsResponse> response) {
                if (response.isSuccessful()) {
                    List<Order.OrderDetailsResponse> newOrders = response.body().data.content;
                    if (newOrders.size() > 0) {

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
                                .setContentTitle("Symplified")
                                .setContentText("You have new orders")
                                .setAutoCancel(false)
                                .setPriority(NotificationCompat.PRIORITY_MAX)
                                .setCategory(NotificationCompat.CATEGORY_ALARM)
                                .setColor(Color.CYAN)
                                .build();

                        notification.flags |= Notification.FLAG_AUTO_CANCEL;

                        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                        notificationManager.notify(new Random().nextInt(), notification);

                        String storeId = newOrders.get(0).order.storeId;

                        StoreApi storeApiService = ServiceGenerator.createStoreService();
                        Call<ResponseBody> storeResponse = storeApiService.getStoreById(new HashMap<>(), storeId);

                        storeResponse.clone().enqueue(new Callback<ResponseBody>() {
                            @Override
                            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                                Log.d("order-notif", "StoreRequest response: " + response);
                                Log.d("order-notif", "StoreRequest response body: " + response.body());

                                if (response.isSuccessful()) {
                                    try {
                                        StoreResponse.SingleStoreResponse responseBody = new Gson().fromJson(response.body().string(), StoreResponse.SingleStoreResponse.class);
                                        if (!AlertService.isPlaying()) {
                                            Intent intent = new Intent(context, AlertService.class);
                                            intent.putExtra(String.valueOf(R.string.store_type), responseBody.data.verticalCode);
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                context.startForegroundService(intent);
                                            } else {
                                                context.startService(intent);
                                            }
                                        }
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }

                            @Override
                            public void onFailure(Call<ResponseBody> call, Throwable t) {
                                Log.e("startup-receiver", "onFailure on storeRequest" + t.getLocalizedMessage());
                            }
                        });
                    }
                }
            }

            @Override
            public void onFailure(Call<OrderDetailsResponse> call, Throwable t) {

            }
        });
    }

    private ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback() {
        @Override
        public void onAvailable(Network network) {
            super.onAvailable(network);
        }

        @Override
        public void onLost(Network network) {
            super.onLost(network);
        }

        @Override
        public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {
            super.onCapabilitiesChanged(network, networkCapabilities);
            final boolean unmetered = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED);
        }
    };
}
