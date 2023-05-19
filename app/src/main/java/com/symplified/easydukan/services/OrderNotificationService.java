package com.symplified.easydukan.services;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.symplified.easydukan.App;
import com.symplified.easydukan.R;
import com.symplified.easydukan.enums.DineInOption;
import com.symplified.easydukan.enums.OrderStatus;
import com.symplified.easydukan.enums.ServiceType;
import com.symplified.easydukan.interfaces.OrderObserver;
import com.symplified.easydukan.interfaces.QrCodeObserver;
import com.symplified.easydukan.models.error.ErrorRequest;
import com.symplified.easydukan.models.item.ItemsResponse;
import com.symplified.easydukan.models.order.Order;
import com.symplified.easydukan.models.order.OrderDetailsResponse;
import com.symplified.easydukan.models.order.OrderUpdateResponse;
import com.symplified.easydukan.models.ping.PingRequest;
import com.symplified.easydukan.networking.ServiceGenerator;
import com.symplified.easydukan.networking.apis.AuthApi;
import com.symplified.easydukan.networking.apis.OrderApi;
import com.symplified.easydukan.utils.EmptyCallback;
import com.symplified.easydukan.utils.SharedPrefsKey;
import com.symplified.easydukan.utils.Utility;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OrderNotificationService extends FirebaseMessagingService {

    private final Pattern pattern = Pattern.compile("orderId:(\\S+?)$");
    private final String TAG = "order-notification-service";
    private final String PRINT_TAG = "print-helper";

    private static boolean isOrderNotifsEnabled = true;

    private static final List<OrderObserver> newOrderObservers = new ArrayList<>();
    private static final List<OrderObserver> ongoingOrderObservers = new ArrayList<>();
    private static final List<OrderObserver> pastOrderObservers = new ArrayList<>();
    private static final List<QrCodeObserver> qrCodeObservers = new ArrayList<>();

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        String messageTitle = remoteMessage.getData().get("title");
        SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences(App.SESSION, MODE_PRIVATE);
        String clientId = sharedPreferences.getString(SharedPrefsKey.CLIENT_ID, "null");

        if (messageTitle != null && messageTitle.equalsIgnoreCase("heartbeat")) {
            AuthApi userService = ServiceGenerator.createUserService(this);
            String transactionId = remoteMessage.getData().get("body");

            String deviceModel = Build.MANUFACTURER + " " + Build.MODEL;
            userService.ping(clientId, transactionId, new PingRequest(deviceModel))
                    .clone().enqueue(new EmptyCallback());
        } else if (messageTitle != null && messageTitle.equalsIgnoreCase("qrcode")) {
            for (QrCodeObserver observer : qrCodeObservers) {
                observer.onRedeemed();
            }
        } else if (isOrderNotifsEnabled) {
            String orderId = parseOrderId(remoteMessage.getData().get("body"));

            if (orderId != null) {
                OrderApi orderApiService = ServiceGenerator.createOrderService(getApplicationContext());
                orderApiService.searchNewOrdersByClientIdAndOrderId(clientId, orderId)
                        .clone().enqueue(new Callback<OrderDetailsResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<OrderDetailsResponse> call,
                                           @NonNull Response<OrderDetailsResponse> response) {

                        if (response.isSuccessful() &&
                                response.body() != null &&
                                response.body().data.content.size() > 0) {
                            Order.OrderDetails orderDetails = response.body().data.content.get(0);
                            alert(remoteMessage, orderDetails.order);
                            if (orderDetails.order.serviceType == ServiceType.DINEIN
                                    && (App.isPrinterConnected() || App.btPrinters.size() > 0)) {
                                printAndProcessOrder(orderApiService, orderDetails);
                            } else {
                                addOrderToView(newOrderObservers, orderDetails);
                            }
                        } else {
                            alert(remoteMessage, null);
                            if (!response.isSuccessful()) {
                                sendErrorToServer("Error " + response.code()
                                        + " received when querying order "
                                        + orderId + " after receiving Firebase notification.");
                            }
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<OrderDetailsResponse> call,
                                          @NonNull Throwable t) {
                        Log.e(TAG, "onFailure on orderRequest. " + t.getLocalizedMessage());
                        sendErrorToServer("Failed to query order "
                                + orderId + " after receiving Firebase notification. Error: " + t.getLocalizedMessage());
                        alert(remoteMessage, null);
                    }
                });
            }
        }
    }

    private void printAndProcessOrder(
            OrderApi orderApiService,
            Order.OrderDetails orderDetails
    ) {

        orderApiService.getItemsForOrder(orderDetails.order.id).clone().enqueue(new Callback<ItemsResponse>() {
            @Override
            public void onResponse(@NonNull Call<ItemsResponse> call,
                                   @NonNull Response<ItemsResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        App.printOrderReceipt(
                                orderDetails.order,
                                response.body().data.content,
                                Utility.getCurrencySymbol(orderDetails.order, getApplicationContext()),
                                getApplicationContext()
                        );
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

        orderApiService.updateOrderStatus(
                new Order.OrderUpdate(
                        orderDetails.order.id,
                        nextCompletionStatus
                ), orderDetails.order.id
        ).clone().enqueue(new Callback<OrderUpdateResponse>() {
            @Override
            public void onResponse(@NonNull Call<OrderUpdateResponse> call,
                                   @NonNull Response<OrderUpdateResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Order.OrderDetails updatedOrderDetails
                            = new Order.OrderDetails(response.body().data);
                    if (Utility.isOrderCompleted(updatedOrderDetails.currentCompletionStatus)) {
                        addOrderToView(pastOrderObservers, updatedOrderDetails);
                    } else if (Utility.isOrderOngoing(updatedOrderDetails.currentCompletionStatus)) {
                        addOrderToView(ongoingOrderObservers, updatedOrderDetails);
                    }
                } else {
                    addOrderToView(newOrderObservers, orderDetails);
                    Log.e("order-adapter", "OrderNotificationService: Failed to process order: Error " + response.code());
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
        String clientId = getSharedPreferences(App.SESSION, MODE_PRIVATE)
                .getString(SharedPrefsKey.CLIENT_ID, "");

        AuthApi userService = ServiceGenerator.createUserService(this);
        userService.logError(new ErrorRequest(clientId, errorMessage, "HIGH"))
                .clone().enqueue(new EmptyCallback());
    }

    private String parseOrderId(String body) {
        if (body != null) {
            Matcher matcher = pattern.matcher(body);
            if (matcher.find() && matcher.group(0) != null) {
                try {
                    return matcher.group().replace("orderId:", "");
                } catch (Exception e) {
                    Log.e(TAG, "Error while parsing orderId: " + e.getLocalizedMessage());
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
     * @param order         Values used by AlertService to determine looping frequency
     */
    private void alert(RemoteMessage remoteMessage, Order order) {

        if (!AlertService.isPlaying()) {
            Intent intent = new Intent(getApplicationContext(), AlertService.class);
            intent.putExtra(getString(R.string.store_type),
                    order != null && order.store.verticalCode != null ? order.store.verticalCode : "");
            intent.putExtra(getString(R.string.service_type),
                    order != null && order.serviceType != null ? order.serviceType.toString() : "");
            intent.putExtra("title", remoteMessage.getData().get("title"));
            intent.putExtra("body", remoteMessage.getData().get("body"));

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent);
            } else {
                startService(intent);
            }
        }
    }

    public static void addNewOrderObserver(OrderObserver observer) {
        newOrderObservers.add(observer);
    }

    public static void removeNewOrderObserver(OrderObserver observer) {
        newOrderObservers.remove(observer);
    }

    public static void addOngoingOrderObserver(OrderObserver observer) {
        ongoingOrderObservers.add(observer);
    }

    public static void removeOngoingOrderObserver(OrderObserver observer) {
        ongoingOrderObservers.remove(observer);
    }

    public static void addPastOrderObserver(OrderObserver observer) {
        pastOrderObservers.add(observer);
    }
    public static void removePastOrderObserver(OrderObserver observer) {
        pastOrderObservers.remove(observer);
    }

    public static void addQrCodeObserver(QrCodeObserver observer) {
        qrCodeObservers.add(observer);
    }
    public static void removeQrCodeObserver(QrCodeObserver observer) {
        qrCodeObservers.remove(observer);
    }

    public static void disableOrderNotifications() {
        isOrderNotifsEnabled = false;
    }
    public static void enableOrderNotifications() {
        isOrderNotifsEnabled = true;
    }
}