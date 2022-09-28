package com.symplified.order.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.RemoteException;
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
import com.symplified.order.enums.DineInOption;
import com.symplified.order.enums.ServiceType;
import com.symplified.order.enums.Status;
import com.symplified.order.helpers.SunmiPrintHelper;
import com.symplified.order.models.HttpResponse;
import com.symplified.order.models.item.ItemResponse;
import com.symplified.order.models.order.Order;
import com.symplified.order.models.order.OrderDetailsResponse;
import com.symplified.order.models.order.OrderUpdateResponse;
import com.symplified.order.networking.ServiceGenerator;
import com.symplified.order.observers.OrderObserver;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OrderNotificationService extends FirebaseMessagingService {

    private Pattern pattern;
    private String TAG = "order-notification-service";
    private static List<OrderObserver> newOrderObservers = new ArrayList<>();

    @Override
    public void onCreate() {
        super.onCreate();
        pattern = Pattern.compile("\\#(\\S+?)$");
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        String messageTitle = remoteMessage.getData().get("title");
        SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences(App.SESSION_DETAILS_TITLE, MODE_PRIVATE);
        String clientId = sharedPreferences.getString("ownerId", "null");

        Log.d(TAG, "onMessageReceived title: " + messageTitle + "onMessageReceived: " + remoteMessage.getData().get("body"));

        if (messageTitle != null && messageTitle.equalsIgnoreCase("heartbeat")) {
            LoginApi userService = ServiceGenerator.createLoginService();
            String transactionId = remoteMessage.getData().get("body");

            Call<HttpResponse> pingRequest = userService.ping(clientId, transactionId);
            pingRequest.clone().enqueue(new Callback<HttpResponse>() {
                @Override
                public void onResponse(@NonNull Call<HttpResponse> call, @NonNull Response<HttpResponse> response) {
                }

                @Override
                public void onFailure(@NonNull Call<HttpResponse> call, @NonNull Throwable t) {
                }
            });
        } else {
            String invoiceId = parseInvoiceId(remoteMessage.getData().get("body"));
            if (invoiceId != null) {
                OrderApi orderApiService = ServiceGenerator.createOrderService();
                Call<OrderDetailsResponse> orderRequest = orderApiService.getNewOrdersByClientIdAndInvoiceId(clientId, invoiceId);

                orderRequest.clone().enqueue(new Callback<OrderDetailsResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<OrderDetailsResponse> call,
                                           @NonNull Response<OrderDetailsResponse> response) {
                        if (response.isSuccessful() && response.body().data.content.size() > 0) {
                            Order.OrderDetails orderDetails = response.body().data.content.get(0);

                            if (ServiceType.DINEIN.toString().equals(orderDetails.order.serviceType)
                                    && DineInOption.SENDTOTABLE.toString().equals(orderDetails.order.dineInOption)
                                    && SunmiPrintHelper.getInstance().isPrinterConnected()) {

                                orderApiService.updateOrderStatus(
                                        new Order.OrderUpdate(orderDetails.order.id, Status.DELIVERED_TO_CUSTOMER),
                                        orderDetails.order.id).clone()
                                        .enqueue(new Callback<OrderUpdateResponse>() {
                                    @Override
                                    public void onResponse(@NonNull Call<OrderUpdateResponse> call,
                                                           @NonNull Response<OrderUpdateResponse> response) {
                                        if (response.isSuccessful()) {
                                            orderApiService.getItemsForOrder(orderDetails.order.id).clone()
                                                    .enqueue(new Callback<ItemResponse>() {
                                                        @Override
                                                        public void onResponse(@NonNull Call<ItemResponse> call,
                                                                               @NonNull Response<ItemResponse> response) {
                                                            if (response.isSuccessful()) {
                                                                try {
                                                                    SunmiPrintHelper.getInstance().printReceipt(orderDetails.order, response.body().data.content);
                                                                    alertUser(remoteMessage, orderDetails);
                                                                } catch (RemoteException e) {
                                                                    addNewOrderAndAlertUser(remoteMessage, orderDetails);
                                                                }
                                                            } else {
                                                                addNewOrderAndAlertUser(remoteMessage, orderDetails);
                                                            }
                                                        }

                                                        @Override
                                                        public void onFailure(@NonNull Call<ItemResponse> call,
                                                                              @NonNull Throwable t) {
                                                            addNewOrderAndAlertUser(remoteMessage, orderDetails);
                                                        }
                                                    });
                                        } else {
                                            addNewOrderAndAlertUser(remoteMessage, orderDetails);
                                        }
                                    }

                                    @Override
                                    public void onFailure(Call<OrderUpdateResponse> call, Throwable t) {
                                        addNewOrderAndAlertUser(remoteMessage, orderDetails);
                                    }
                                });
                            } else {
                                addNewOrderAndAlertUser(remoteMessage, orderDetails);
                            }
                        } else {
                            alertUser(remoteMessage, null);
                        }
                    }

                    @Override
                    public void onFailure(Call<OrderDetailsResponse> call, Throwable t) {
                        Log.e(TAG, "onFailure on orderRequest. " + t.getLocalizedMessage());
                        addNewOrderAndAlertUser(remoteMessage, null);
                    }
                });
            }
        }
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

    private void addNewOrderAndAlertUser(RemoteMessage remoteMessage, Order.OrderDetails orderDetails) {
        if (orderDetails != null) {
            for (OrderObserver observer : newOrderObservers) {
                observer.onOrderReceived(orderDetails);
            }
        }

        alertUser(remoteMessage, orderDetails);
    }

    private void alertUser(RemoteMessage remoteMessage, Order.OrderDetails orderDetails) {
        Intent toOrdersActivity = new Intent(this, OrdersActivity.class);

        TaskStackBuilder taskStackBuilder = TaskStackBuilder.create(this);
        taskStackBuilder.addNextIntentWithParentStack(toOrdersActivity);

        PendingIntent pendingIntent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            pendingIntent = taskStackBuilder.getPendingIntent(0, PendingIntent.FLAG_IMMUTABLE);
        } else {
            pendingIntent = taskStackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
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

        if (!AlertService.isPlaying()) {
            Intent intent = new Intent(getApplicationContext(), AlertService.class);
            intent.putExtra(String.valueOf(R.string.store_type),
                    orderDetails != null ? orderDetails.order.store.verticalCode : "");
            intent.putExtra(String.valueOf(R.string.service_type),
                    orderDetails != null ? orderDetails.order.serviceType : "");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent);
            } else {
                startService(intent);
            }
        }
    }

    public static void addObserver(OrderObserver observer) {
        newOrderObservers.add(observer);
    }

    public static void removeObserver(OrderObserver observer) {
        newOrderObservers.remove(observer);
    }
}
