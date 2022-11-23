package com.symplified.easydukan.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
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
import com.symplified.easydukan.apis.OrderApi;
import com.symplified.easydukan.callbacks.EmptyCallback;
import com.symplified.easydukan.enums.DineInOption;
import com.symplified.easydukan.enums.OrderStatus;
import com.symplified.easydukan.enums.ServiceType;
import com.symplified.easydukan.interfaces.OrderObserver;
import com.symplified.easydukan.models.error.ErrorRequest;
import com.symplified.easydukan.models.item.ItemsResponse;
import com.symplified.easydukan.models.order.Order;
import com.symplified.easydukan.models.order.OrderDetailsResponse;
import com.symplified.easydukan.models.order.OrderUpdateResponse;
import com.symplified.easydukan.models.ping.PingRequest;
import com.symplified.easydukan.networking.ServiceGenerator;
import com.symplified.easydukan.utils.Utility;

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
    private final String TAG = "order-notification-service";
    private static final List<OrderObserver> newOrderObservers = new ArrayList<>();
    private static final List<OrderObserver> ongoingOrderObservers = new ArrayList<>();
    private static final List<OrderObserver> pastOrderObservers = new ArrayList<>();

    @Override
    public void onCreate() {
        super.onCreate();
        pattern = Pattern.compile("\\#(\\S+?)$");
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        String messageTitle = remoteMessage.getData().get("title");
        SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences(App.SESSION_DETAILS_TITLE, MODE_PRIVATE);
        String clientId = sharedPreferences.getString("ownerId", "null");

        if (messageTitle != null && messageTitle.equalsIgnoreCase("heartbeat")) {
            LoginApi userService = ServiceGenerator.createUserService();
            String transactionId = remoteMessage.getData().get("body");

            String deviceModel = Build.MANUFACTURER + " " + Build.MODEL;
            userService.ping(clientId, transactionId, new PingRequest(deviceModel))
                    .clone().enqueue(new EmptyCallback());
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

                            alert(remoteMessage, orderDetails.order);
                            if (orderDetails.order.serviceType == ServiceType.DINEIN
                                    && App.isPrinterConnected()) {
                                printAndProcessOrder(orderApiService, remoteMessage, orderDetails);
                            } else {
                                addOrderToView(newOrderObservers, orderDetails);

                                if (orderDetails.order.serviceType == ServiceType.DINEIN) {
                                    sendErrorToServer("Dine-In order " + invoiceId
                                            + " cannot be printed and auto-processed because a printer was not detected");
                                }
                            }
                        } else {
                            alert(remoteMessage, null);
                            sendErrorToServer("Error " + response.code() + " received when querying order "
                                    + invoiceId + " after receiving Firebase notification.");
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<OrderDetailsResponse> call,
                                          @NonNull Throwable t) {
                        Log.e(TAG, "onFailure on orderRequest. " + t.getLocalizedMessage());
                        sendErrorToServer("Failed to query order "
                                + invoiceId + " after receiving Firebase notification. Error: " + t.getLocalizedMessage());
                        alert(remoteMessage, null);
                    }
                });
            }
        }
    }

    private void printAndProcessOrder(OrderApi orderApiService,
                                      RemoteMessage remoteMessage,
                                      Order.OrderDetails orderDetails) {

        orderApiService.getItemsForOrder(orderDetails.order.id)
                .clone()
                .enqueue(new Callback<ItemsResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<ItemsResponse> call,
                                           @NonNull Response<ItemsResponse> response) {
                        if (response.isSuccessful()) {
                            try {
                                App.getPrinter()
                                        .printReceipt(orderDetails.order, response.body().data.content);
                                processNewOrder(orderApiService, orderDetails);
                            } catch (Exception e) {
                                addOrderToView(newOrderObservers, orderDetails);

                                String errorMessage = "Error occurred while printing Dine-in order "
                                        + orderDetails.order.id + " after receiving notification. " +
                                        "Cannot proceed with auto-process of order.";
                                sendErrorToServer(errorMessage);
                            }
                        } else {
                            addOrderToView(newOrderObservers, orderDetails);

                            String errorMessage = "Error " + response.code() + " received while retrieving items for " +
                                    "Dine-in order " + orderDetails.order.id + " after receiving notification. " +
                                    "Cannot proceed with printing and auto-process of order.";
                            sendErrorToServer(errorMessage);
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ItemsResponse> call,
                                          @NonNull Throwable t) {
                        addOrderToView(newOrderObservers, orderDetails);
                        String errorMessage = "Failed to retrieve items for " +
                                "Dine-in order " + orderDetails.order.id + " after receiving notification. " +
                                "Cannot proceed with printing and auto-process of order. Error: " +
                                t.getLocalizedMessage();
                        sendErrorToServer(errorMessage);
                    }
                });
    }

    private void processNewOrder(OrderApi orderApiService, Order.OrderDetails orderDetails) {
        OrderStatus nextCompletionStatus
                = orderDetails.order.dineInOption == DineInOption.SENDTOTABLE
                ? OrderStatus.DELIVERED_TO_CUSTOMER
                : orderDetails.nextCompletionStatus;

        orderApiService.updateOrderStatus(new Order.OrderUpdate(orderDetails.order.id, nextCompletionStatus),
                        orderDetails.order.id)
                .clone()
                .enqueue(new Callback<OrderUpdateResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<OrderUpdateResponse> call,
                                           @NonNull Response<OrderUpdateResponse> response) {
                        if (response.isSuccessful()) {
                            Order.OrderDetails updatedOrderDetails = new Order.OrderDetails(response.body().data);
                            if (Utility.isOrderCompleted(orderDetails.currentCompletionStatus)) {
                                addOrderToView(pastOrderObservers, updatedOrderDetails);
                            } else if (Utility.isOrderOngoing(orderDetails.currentCompletionStatus)) {
                                addOrderToView(ongoingOrderObservers, updatedOrderDetails);
                            }
                        } else {
                            addOrderToView(newOrderObservers, orderDetails);
                            sendErrorToServer("Failed to auto-process order " + orderDetails.order.id);
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<OrderUpdateResponse> call,
                                          @NonNull Throwable t) {
                        addOrderToView(newOrderObservers, orderDetails);
                    }
                });
    }

    private void sendErrorToServer(String errorMessage) {
        String clientId = getSharedPreferences(App.SESSION_DETAILS_TITLE, MODE_PRIVATE)
                .getString("ownerId", "");

        LoginApi userService = ServiceGenerator.createUserService();
        userService.logError(new ErrorRequest(clientId, errorMessage, "HIGH"))
                .clone().enqueue(new EmptyCallback());
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

    private void addOrderToView(List<OrderObserver> orderObservers, Order.OrderDetails orderDetails) {
        if (orderDetails != null) {
            for (OrderObserver observer : orderObservers) {
                observer.onOrderReceived(orderDetails);
            }
        }
    }

    /**
     * Shows notification for order and plays a custom track on loop
     *
     * @param remoteMessage Message received from firebase used for notification
     * @param order Values used by AlertService to determine looping frequency
     *
     */
    private void alert(RemoteMessage remoteMessage, Order order) {
        Intent toOrdersActivity = new Intent(this, OrdersActivity.class);

        TaskStackBuilder taskStackBuilder = TaskStackBuilder.create(this);
        taskStackBuilder.addNextIntentWithParentStack(toOrdersActivity);

        PendingIntent pendingIntent = taskStackBuilder.getPendingIntent(0, 
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S 
                    ? PendingIntent.FLAG_IMMUTABLE : PendingIntent.FLAG_UPDATE_CURRENT);
        
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
                    order != null && order.store.verticalCode != null ? order.store.verticalCode : "");
            intent.putExtra(String.valueOf(R.string.service_type),
                    order != null && order.serviceType != null ? order.serviceType.toString() : "");

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent);
            } else {
                startService(intent);
            }
        }
    }

    private void notifyUser(String title, String body) {
        
        Intent toOrdersActivity = new Intent(this, OrdersActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addNextIntentWithParentStack(toOrdersActivity);
        PendingIntent pendingIntent =
                stackBuilder.getPendingIntent(0,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(App.CHANNEL_ID,
                    "New Orders", NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        Notification notification = new NotificationCompat.Builder(getApplicationContext(), App.CHANNEL_ID)
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setColor(Color.CYAN)
                .build();

        notification.flags |= Notification.FLAG_AUTO_CANCEL;

        notificationManager.notify(new Random().nextInt(), notification);
    }

    public static void addNewOrderObserver(OrderObserver observer) { newOrderObservers.add(observer); }
    public static void removeNewOrderObserver(OrderObserver observer) { newOrderObservers.remove(observer); }

    public static void addOngoingOrderObserver(OrderObserver observer) { ongoingOrderObservers.add(observer); }
    public static void removeOngoingOrderObserver(OrderObserver observer) { ongoingOrderObservers.remove(observer); }

    public static void addPastOrderObserver(OrderObserver observer) { pastOrderObservers.add(observer); }
    public static void removePastOrderObserver(OrderObserver observer) { pastOrderObservers.remove(observer); }
}
