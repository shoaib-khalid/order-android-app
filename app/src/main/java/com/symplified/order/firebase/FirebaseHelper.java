package com.symplified.order.firebase;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.messaging.FirebaseMessaging;
import com.symplified.order.services.AlertService;

public class FirebaseHelper {
    static public boolean initializeFirebase(String storeId, Context context){
        boolean result =true;
        try{

            FirebaseMessaging.getInstance().getToken().addOnCompleteListener(new OnCompleteListener<String>() {
                @Override
                public void onComplete(@NonNull Task<String> task) {
                    if (!task.isSuccessful()) {
                        Log.w("TAG", "Fetching FCM registration token failed", task.getException());
                        return;
                    }

                    // Get new FCM registration token
                    String token = task.getResult();

                    // Log and toast
                    Log.d("TAG", "token : "+ token);
                }
            });

            FirebaseMessaging.getInstance().subscribeToTopic(storeId);

            if(!isServiceRunning(context))
            {
                Log.e("TAG", "onCreate: Service not running ", new Error());
                context.startService(new Intent(context, AlertService.class).putExtra("first", 1));
            }

        }catch(Exception ex){
            Log.e("FirebaseHelper",ex.toString());
            result =false;
        }
        return result;
    }
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
