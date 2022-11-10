package com.symplified.order.firebase;

import com.google.firebase.messaging.FirebaseMessaging;

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
