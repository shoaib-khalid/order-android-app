package com.symplified.order.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.util.Log;

import com.symplified.order.App;
import com.symplified.order.enums.OrderStatus;
import com.symplified.order.models.order.Order;

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

    public static void logToFile(String text) {
        File file = new File(App.getAppContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "log.txt");
        Log.d("print", "File path: " + file.getAbsolutePath());

        try {
            FileWriter fr = new FileWriter(file, true);
            fr.write(text);
            fr.close();
        } catch (Exception e) {
            String errorText = "Failed to write to file. " + e.getLocalizedMessage();
            Log.e("print", errorText);
        }
    }

    public static String getCurrencySymbol(Order order) {
        if (order != null && order.store != null && order.store.regionCountry != null
                && order.store.regionCountry.currencySymbol != null) {
            return order.store.regionCountry.currencySymbol;
        }
        SharedPreferences sharedPreferences
                = App.getAppContext().getSharedPreferences(App.SESSION_DETAILS_TITLE, Context.MODE_PRIVATE);
        return sharedPreferences.getString("currency", "RM");
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
}