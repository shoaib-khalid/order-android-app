package com.symplified.order.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.util.Base64;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import com.symplified.order.App;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;

public class Utility {

    public static String encodeTobase64(Bitmap image) {
        Bitmap bitmap_image = image;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap_image.compress(Bitmap.CompressFormat.PNG, 100, baos);
        byte[] b = baos.toByteArray();
        String imageEncoded = Base64.encodeToString(b, Base64.DEFAULT);

        return imageEncoded;
    }

    public static void decodeAndSetImage(ImageView imageView, String encodedImage){
        byte[] imageAsBytes = Base64.decode(encodedImage.getBytes(), Base64.DEFAULT);
        Bitmap bitmap = BitmapFactory.decodeByteArray(imageAsBytes, 0, imageAsBytes.length);
//        Log.e("TAG", "decodeAndSetImage: ", new Error());
        imageView.setImageBitmap(bitmap);
    }

    public static <T extends Enum<T>> T getEnumFromString(Class<T> c, String string) {
        if( c != null && string != null ) {
            try {
                return Enum.valueOf(c, string.trim().toUpperCase());
            } catch(IllegalArgumentException ex) {
            }
        }
        return null;
    }

    public static String removeUnderscores(String s){
        return s.replace("_", " ");
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

}
