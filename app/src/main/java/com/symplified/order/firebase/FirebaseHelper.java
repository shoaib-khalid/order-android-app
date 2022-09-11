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
     *
     * @param storeId storeId
     * @return returns false if any error, otherwise returns true
     */
    public static boolean initializeFirebase(String storeId) {
        try {
            FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            });

            FirebaseMessaging.getInstance().subscribeToTopic(storeId);
        } catch (Exception ex) {
            Log.e("FirebaseHelper", ex.toString());
            return false;
        }
        return true;
    }
}
