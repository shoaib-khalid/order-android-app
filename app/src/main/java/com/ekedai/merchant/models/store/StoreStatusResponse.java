package com.ekedai.merchant.models.store;

import com.ekedai.merchant.models.HttpResponse;

import java.io.Serializable;

public class StoreStatusResponse extends HttpResponse implements Serializable {

    public StoreStatus data;

    public static class StoreStatus implements Serializable {
        public boolean isSnooze = false;
        public String snoozeEndTime = "";
    }
}
