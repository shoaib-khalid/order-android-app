package com.symplified.order.utils;

import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.os.Environment;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.TaskStackBuilder;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.firebase.messaging.FirebaseMessaging;
import com.symplified.order.App;
import com.symplified.order.R;
import com.symplified.order.enums.OrderStatus;
import com.symplified.order.models.order.Order;
import com.symplified.order.ui.LoginActivity;

import java.io.File;
import java.io.FileWriter;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

public class Utility {

    private static final SimpleDateFormat dateParser = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final SimpleDateFormat dateFormatter = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss aa");

    public static <T extends Enum<T>> T getEnumFromString(Class<T> c, String string) {
        if (c != null && string != null) {
            try {
                return Enum.valueOf(c, string.trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
            }
        }
        return null;
    }

    public static void logToFile(String text, Context context) {
        File file = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "log.txt");
        try {
            FileWriter fr = new FileWriter(file, true);
            fr.write(text);
            fr.close();
        } catch (Exception e) {
            String errorText = "Failed to write to file. " + e.getLocalizedMessage();
            Log.e("print", errorText);
        }
    }

    public static String getCurrencySymbol(Order order, Context context) {
        if (order != null
                && order.store != null
                && order.store.regionCountry != null
                && order.store.regionCountry.currencySymbol != null) {
            return order.store.regionCountry.currencySymbol;
        }
        return context.getSharedPreferences(App.SESSION, Context.MODE_PRIVATE)
                .getString("currency", "RM");
    }

    public static DecimalFormat getMonetaryAmountFormat() {
        return new DecimalFormat("#,###0.00");
    }

    public static String convertUtcTimeToLocalTimezone(String dateTime, TimeZone localTimeZone) {

        dateParser.setTimeZone(TimeZone.getTimeZone("UTC"));
        dateFormatter.setTimeZone(localTimeZone);

        try {
            return dateFormatter.format(dateParser.parse(dateTime));
        } catch (ParseException e) {
            Log.e("datetime", "Failed to parse date. " + e.getLocalizedMessage());
        } catch (NullPointerException e) {
            Log.e("datetime", "Parsed date was null. " + e.getLocalizedMessage());
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

    public static boolean isGooglePlayServicesAvailable(Context context) {
        return GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS;
    }

    public static boolean isConnectedToInternet(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.getActiveNetwork() != null && cm.getNetworkCapabilities(cm.getActiveNetwork()) != null;
    }

    public static void notify(Context context,
                              String title,
                              String text,
                              String bigText,
                              String channel,
                              int notificationId,
                              Class<?> activity) {
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addNextIntentWithParentStack(new Intent(context, activity));
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
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(text);
        if (!isBlank(bigText)) {
            builder.setStyle(new NotificationCompat.BigTextStyle()
                    .bigText(bigText));
        }
        builder.setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setColor(Color.CYAN)
                .setAutoCancel(true)
                .build();

        notificationManager.cancel(notificationId);
        notificationManager.notify(notificationId, builder.build());
    }

    public static void verifyLoginStatus(Activity activity) {
        SharedPreferences sharedPreferences = activity.getSharedPreferences(App.SESSION, Context.MODE_PRIVATE);
        if (!sharedPreferences.getBoolean(Key.IS_LOGGED_IN, false)
                || sharedPreferences.getString(Key.STORE_ID_LIST, null) == null) {
            Log.d("utility", "Not logged in");
            logout(activity);
        }
    }

    public static void logout(Activity activity) {
        SharedPreferences sharedPreferences = activity.getSharedPreferences(App.SESSION, Context.MODE_PRIVATE);
        String storeIdList = sharedPreferences.getString(Key.STORE_ID_LIST, null);
        if (storeIdList != null) {
            for (String storeId : storeIdList.split(" ")) {
                FirebaseMessaging.getInstance().unsubscribeFromTopic(storeId);
            }
        }
        boolean isStaging = sharedPreferences.getBoolean(Key.IS_STAGING, false);
        String baseUrl = sharedPreferences.getString(Key.BASE_URL, App.BASE_URL_PRODUCTION);
        sharedPreferences.edit().clear().apply();
        sharedPreferences.edit()
                .putBoolean(Key.IS_STAGING, isStaging)
                .putString(Key.BASE_URL, baseUrl)
                .apply();

        Intent intent = new Intent(activity, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        activity.startActivity(intent);
        activity.finish();
    }
}
