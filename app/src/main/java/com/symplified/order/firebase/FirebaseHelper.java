package com.symplified.order.firebase;

import com.google.firebase.messaging.FirebaseMessaging;

public class FirebaseHelper {
    /**
     * Initializes firebase messaging instance.
     * Also subscribes to storeId topic
     *
     * @param storeId storeId
     */
    public static void initializeFirebase(String storeId) {
        FirebaseMessaging.getInstance().subscribeToTopic(storeId);
    }
}
