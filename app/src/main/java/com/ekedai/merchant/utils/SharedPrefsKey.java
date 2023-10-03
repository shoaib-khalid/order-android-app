package com.ekedai.merchant.utils;

public interface SharedPrefsKey {
    String IS_LOGGED_IN = "is_logged_in";
    String INVOICE_ID = "invoice_id";
    String ORDER_DETAILS = "order_details";
    String BASE_URL = "base_url";
    String IS_STAGING = "is_staging";
    String IS_SUBSCRIBED_TO_NOTIFICATIONS = "is_subscribed_to_notifications";
    String STORE_ID_LIST = "storeIdList";
    String CLIENT_ID = "ownerId";
    String USERNAME = "username";
    String ACCESS_TOKEN = "accessToken";
    String REFRESH_TOKEN = "refreshToken";
    String IS_ORDER_CONSOLIDATION_ENABLED = "is_order_consolidation_enabled";
    String CURRENCY_SYMBOL = "currency";

    String BT_DEVICE_PREFS_FILE_NAME = "Symplified Merchant App Bluetooth Device Preferences";
    String SAVED_BT_DEVICES_FILE_NAME = "Symplified Merchant App Bluetooth Devices";

}
