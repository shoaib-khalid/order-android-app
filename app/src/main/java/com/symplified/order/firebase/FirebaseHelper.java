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
        notifyUser("Initializing firebase");

        FirebaseMessaging.getInstance().getToken();

        FirebaseMessaging.getInstance().subscribeToTopic(storeId).addOnCompleteListener(task -> {
            if (!task.isComplete()) {
                String message = "Unable to subscribe to Firebase notifications for store " + storeId;

                // TODO: Send error messages to backend
            }
        }).addOnFailureListener(e -> {
            String message = "Failed to subscribe to store " + storeId + ". Error: " + e.getLocalizedMessage();

        }).addOnCanceledListener(() -> {
            String message = "Subscription to store " + storeId + " cancelled.";

        });
    }

    private static void notifyUser(String body) {

        Intent toOrdersActivity = new Intent(App.getAppContext(), LoginActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(App.getAppContext());
        stackBuilder.addNextIntentWithParentStack(toOrdersActivity);
        PendingIntent pendingIntent =
                stackBuilder.getPendingIntent(0,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationManager notificationManager = App.getAppContext().getSystemService(NotificationManager.class);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("FIREBASE_SUBSCRIPTION",
                    "Firebase subscription", NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        Notification notification = new NotificationCompat.Builder(App.getAppContext(), App.CHANNEL_ID)
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Firebase Subscription")
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setColor(Color.CYAN)
                .setGroup("FIREBASE_SUBSCRIPTION_RESULT")
                .build();

        notification.flags |= Notification.FLAG_AUTO_CANCEL;

        notificationManager.notify(new Random().nextInt(), notification);
    }
}
