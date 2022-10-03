package com.symplified.order.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.util.Base64;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import com.symplified.order.App;
import com.symplified.order.models.order.Order;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
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
}
