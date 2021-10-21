package com.symplified.order.firebase;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessaging;
import com.symplified.order.services.AlertService;

public class FirebaseHelper {
    /**
     * Initializes firebase messaging instance.
     * Also subscribes to storeId topic
     * Starts AlertService if not already running
     * @param storeId storeId
     * @param context application context
     * @return returns false if any error, otherwise returns true
     */
    static public boolean initializeFirebase(String storeId, Context context){
        boolean result =true;
        try{

            FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
                if (!task.isSuccessful()) {
                    Log.w("TAG", "Fetching FCM registration token failed", task.getException());
                    return;
                }
                // Get new FCM registration token
                String token = task.getResult();

                // Log and toast
                Log.d("TAG", "token : "+ token);
            });

            FirebaseMessaging.getInstance().subscribeToTopic(storeId);
            Log.i("SUBS", "initializeFirebase: subscribed to topic "+ storeId);

            if(!isServiceRunning(context))
            {
                Log.e("TAG", "onCreate: Service not running ", new Error());
//                context.startService(new Intent(context, AlertService.class).putExtra("first", 1));
            }
        }catch(Exception ex){
            Log.e("FirebaseHelper",ex.toString());
            result =false;
        }
        return result;
    }

    /**
     * Checks if Alert service is running
     * @param context application context
     * @return true if service is running, otherwise returns false
     */
    private static boolean isServiceRunning(Context context){
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (AlertService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
