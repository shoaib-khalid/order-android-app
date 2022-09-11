package com.symplified.order.services;

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
import com.symplified.order.App;
import com.symplified.order.OrdersActivity;
import com.symplified.order.R;
import com.symplified.order.apis.LoginApi;
import com.symplified.order.apis.OrderApi;
import com.symplified.order.apis.StoreApi;
import com.symplified.order.enums.Status;
import com.symplified.order.models.HttpResponse;
import com.symplified.order.models.Store.StoreResponse;
import com.symplified.order.models.order.Order;
import com.symplified.order.models.order.OrderDetailsResponse;
import com.symplified.order.models.order.OrderResponse;
import com.symplified.order.networking.ServiceGenerator;
import com.symplified.order.observers.NewOrderObserver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OrderNotificationService extends FirebaseMessagingService {

    private Pattern pattern;
    private Map<String, String> headers;
    private String TAG = "order-notification-service";
    private static List<NewOrderObserver> newOrderObservers = new ArrayList<>();

    @Override
    public void onCreate() {
        super.onCreate();

        pattern = Pattern.compile("\\#(\\S+?)$");

        headers = new HashMap<>();
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        String messageTitle = remoteMessage.getData().get("title");
        SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences(App.SESSION_DETAILS_TITLE, MODE_PRIVATE);
        String clientId = sharedPreferences.getString("ownerId", "null");

        if (messageTitle != null && messageTitle.equalsIgnoreCase("heartbeat")) {
            LoginApi userService = ServiceGenerator.createLoginService();
            String transactionId = remoteMessage.getData().get("body");

            Call<HttpResponse> pingRequest = userService.ping(clientId, transactionId);
            pingRequest.clone().enqueue(new Callback<HttpResponse>() {
                @Override
                public void onResponse(Call<HttpResponse> call, Response<HttpResponse> response) {
                }

                @Override
                public void onFailure(Call<HttpResponse> call, Throwable t) {
                }
            });
        } else {
            Intent toOrdersActivity = new Intent(this, OrdersActivity.class);
            String storeIdList = sharedPreferences.getString("storeIdList", null);
            List<String> storeIds = Arrays.asList(storeIdList.split(" "));
            String storeName = remoteMessage.getData().get("storeName");
            String currentStoreId = null;
            for (String storeId : storeIds) {
                if (sharedPreferences.getString(storeId + "-name", null).equals(storeName)) {
                    currentStoreId = storeId;
                }
            }
            TaskStackBuilder taskStackBuilder = TaskStackBuilder.create(this);
            taskStackBuilder.addNextIntentWithParentStack(toOrdersActivity);
            PendingIntent pendingIntent;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                pendingIntent = taskStackBuilder.getPendingIntent(0, PendingIntent.FLAG_IMMUTABLE);
            } else {
                pendingIntent = taskStackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
            }

            String invoiceId = parseInvoiceId(remoteMessage.getData().get("body"));
            if (currentStoreId != null && invoiceId != null) {

                OrderApi orderApiService = ServiceGenerator.createOrderService();
                Call<OrderDetailsResponse> orderRequest = orderApiService.getNewOrdersByClientIdAndInvoiceId(clientId, invoiceId);

                String finalCurrentStoreId = currentStoreId;
                orderRequest.clone().enqueue(new Callback<OrderDetailsResponse>() {
                    @Override
                    public void onResponse(Call<OrderDetailsResponse> call, Response<OrderDetailsResponse> response) {
                        if (response.isSuccessful() && response.body().data.content.size() > 0) {
                            Order.OrderDetails orderDetails = response.body().data.content.get(0);
                            for (NewOrderObserver observer : newOrderObservers) {
                                observer.onNewOrderReceived(orderDetails);
                            }
                            Notification notification = new NotificationCompat.Builder(getApplicationContext(), App.CHANNEL_ID)
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

                            StoreApi storeApiService = ServiceGenerator.createStoreService();
                            Call<StoreResponse.SingleStoreResponse> storeResponse = storeApiService.getStoreByIdNew(headers, finalCurrentStoreId);

                            storeResponse.clone().enqueue(new Callback<StoreResponse.SingleStoreResponse>() {
                                @Override
                                public void onResponse(Call<StoreResponse.SingleStoreResponse> call, Response<StoreResponse.SingleStoreResponse> response) {
                                    if (response.isSuccessful()
                                            && response.body() != null
                                            && response.body().data != null
                                            && !AlertService.isPlaying()) {
                                        Intent intent = new Intent(getApplicationContext(), AlertService.class);
                                        intent.putExtra(String.valueOf(R.string.store_type),
                                                response.body().data.verticalCode);
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                            startForegroundService(intent);
                                        } else {
                                            startService(intent);
                                        }
                                    }
                                }

                                @Override
                                public void onFailure(Call<StoreResponse.SingleStoreResponse> call, Throwable t) {
                                    Log.e(TAG, "onFailure on storeRequest. " + t.getLocalizedMessage());
                                }
                            });
                        }
                    }

                    @Override
                    public void onFailure(Call<OrderDetailsResponse> call, Throwable t) {
                        Log.e(TAG, "onFailure on orderRequest. " + t.getLocalizedMessage());
                    }
                });
            }
        }
    }

    public boolean isServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (AlertService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private boolean isAppOnForeground(Context context, String appPackageName) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
        if (appProcesses == null) {
            return false;
        }
        final String packageName = appPackageName;
        for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
            if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
                    && appProcess.processName.equals(packageName)) {
                return true;
            }
        }
        return false;
    }

    private String parseInvoiceId(String body) {
        if (body != null) {
            Matcher matcher = pattern.matcher(body);
            if (matcher.find() && matcher.group(0) != null) {
                try {
                    return matcher.group().replace("#", "");
                } catch (Exception e) {
                    Log.e(TAG, "Error while parsing invoiceId: " + e.getLocalizedMessage());
                }
            }
        }
        return null;
    }

    public static void setObserver(NewOrderObserver observer) {
        newOrderObservers.add(observer);
    }

    public static void removeObserver(NewOrderObserver observer) {
        newOrderObservers.remove(observer);
    }
}
