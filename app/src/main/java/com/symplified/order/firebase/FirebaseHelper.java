package com.symplified.order.firebase;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Color;

import androidx.core.app.NotificationCompat;
import androidx.core.app.TaskStackBuilder;

import com.google.firebase.messaging.FirebaseMessaging;
import com.symplified.order.App;
import com.symplified.order.LoginActivity;
import com.symplified.order.R;

import java.util.Random;

public class FirebaseHelper {
    /**
     * Initializes firebase messaging instance.
     * Also subscribes to storeId topic
     *
     * @param storeId storeId
     * @return returns false if any error, otherwise returns true
     */
    public static void initializeFirebase(String storeId) {
        FirebaseMessaging.getInstance().getToken();

        FirebaseMessaging.getInstance().subscribeToTopic(storeId);
    }
}
