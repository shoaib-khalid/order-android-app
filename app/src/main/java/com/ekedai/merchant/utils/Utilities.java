package com.ekedai.merchant.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.TaskStackBuilder;

import com.ekedai.merchant.App;
import com.ekedai.merchant.R;
import com.ekedai.merchant.enums.OrderStatus;
import com.ekedai.merchant.models.ErrorResponse;
import com.ekedai.merchant.models.order.Order;
import com.ekedai.merchant.ui.LoginActivity;
import com.ekedai.merchant.ui.orders.OrdersActivity;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.gson.Gson;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import okhttp3.ResponseBody;

public class Utilities {

    public static final String inputDatePattern = "yyyy-MM-dd HH:mm:ss";
    public static final String outputDatePattern = "dd/MM/yyyy | hh:mm:ss aa";
    private static final DecimalFormat priceFormatter = new DecimalFormat("#,###0.00");

    public static String getCurrencySymbol(Order order, Context context) {
        if (order != null
                && order.store != null
                && order.store.regionCountry != null
                && order.store.regionCountry.currencySymbol != null) {
            return order.store.regionCountry.currencySymbol;
        }
        return context.getSharedPreferences(App.SESSION, Context.MODE_PRIVATE)
                .getString(SharedPrefsKey.CURRENCY_SYMBOL, "RM");
    }

    public static DecimalFormat getMonetaryAmountFormat() {
        return new DecimalFormat("#,###0.00");
    }

    public static String formatPrice(Double price) {
        if (price == null) {
            price = 0.0;
        }
        return priceFormatter.format(price);
    }

    public static String convertUtcTimeToLocalTimezone(String dateTime, TimeZone localTimeZone) {

        SimpleDateFormat dateParser = new SimpleDateFormat(inputDatePattern, Locale.getDefault());
        SimpleDateFormat dateFormatter = new SimpleDateFormat(outputDatePattern, Locale.getDefault());

        dateParser.setTimeZone(TimeZone.getTimeZone("UTC"));
        dateFormatter.setTimeZone(localTimeZone);

        try {
            Date parsedDate = dateParser.parse(dateTime);
            if (parsedDate != null) {
                return dateFormatter.format(parsedDate);
            }
        } catch (ParseException e) {
            Log.e("datetime", "Failed to parse date. " + e.getLocalizedMessage());
        }

        return dateTime;
    }

    public static boolean isOrderNew(OrderStatus completionStatus) {
        return completionStatus == OrderStatus.PAYMENT_CONFIRMED
                || completionStatus == OrderStatus.RECEIVED_AT_STORE;
    }

    public static boolean isOrderOngoing(OrderStatus completionStatus) {
        return completionStatus == OrderStatus.BEING_PREPARED
                || completionStatus == OrderStatus.AWAITING_PICKUP
                || completionStatus == OrderStatus.BEING_DELIVERED;
    }

    public static boolean isOrderCompleted(OrderStatus completionStatus) {
        return completionStatus == OrderStatus.DELIVERED_TO_CUSTOMER;
    }

    public static boolean isBlank(String str) {
        return str == null || "".equals(str);
    }

    public static boolean isNotBlank(String str) { return str != null && !"".equals(str); }

    public static boolean isGooglePlayServicesAvailable(Context context) {
        return GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS;
    }

    public static boolean isConnectedToInternet(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.getActiveNetwork() != null && cm.getNetworkCapabilities(cm.getActiveNetwork()) != null;
    }

    public static void notify(
            Context context,
            String title,
            String text,
            String bigText,
            String channel,
            int notificationId,
            boolean shouldCancelPreviousNotification,
            Class<?> activity
    ) {
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addNextIntentWithParentStack(new Intent(context,
                activity != null ? activity : OrdersActivity.class));
        PendingIntent pendingIntent =
                stackBuilder.getPendingIntent(0,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);


        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(new NotificationChannel(channel,
                    channel, NotificationManager.IMPORTANCE_DEFAULT));
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channel)
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle()
                    .bigText(!isBlank(bigText) ? bigText : text))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setColor(Color.CYAN)
                .setAutoCancel(true);

        if (shouldCancelPreviousNotification) {
            notificationManager.cancel(notificationId);
        }
        notificationManager.notify(notificationId, builder.build());
    }

    public static void verifyLoginStatus(Activity activity) {
        SharedPreferences sharedPreferences = activity.getSharedPreferences(App.SESSION, Context.MODE_PRIVATE);
        if (!sharedPreferences.getBoolean(SharedPrefsKey.IS_LOGGED_IN, false)
                || sharedPreferences.getString(SharedPrefsKey.STORE_ID_LIST, null) == null) {
            logout(activity);
        }
    }

    public static void logout(Activity activity) {
        SharedPreferences sharedPreferences = activity.getSharedPreferences(App.SESSION, Context.MODE_PRIVATE);
        String storeIdList = sharedPreferences.getString(SharedPrefsKey.STORE_ID_LIST, null);
        if (storeIdList != null) {
            for (String storeId : storeIdList.split(" ")) {
                FirebaseMessaging.getInstance().unsubscribeFromTopic(storeId);
            }
        }

        boolean isStaging = sharedPreferences.getBoolean(SharedPrefsKey.IS_STAGING, false);
        String baseUrl = sharedPreferences.getString(SharedPrefsKey.BASE_URL, App.BASE_URL_PRODUCTION);
        sharedPreferences.edit().clear().apply();
        sharedPreferences.edit()
                .putBoolean(SharedPrefsKey.IS_STAGING, isStaging)
                .putString(SharedPrefsKey.BASE_URL, baseUrl)
                .apply();

        Intent intent = new Intent(activity, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        activity.startActivity(intent);
        activity.finish();
    }

    public static void handleUnknownError(Context context, ResponseBody errorJson) {
        Gson gson = new Gson();
        ErrorResponse errorResponse = gson.fromJson(errorJson.charStream(), ErrorResponse.class);

        new AlertDialog.Builder(context).setTitle("Error " + errorResponse.status)
                .setMessage(errorResponse.error + "\n" + errorResponse.message)
                .setIcon(R.drawable.baseline_error_outline_24)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> dialog.cancel())
                .show();
    }
}
