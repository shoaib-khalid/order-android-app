package com.symplified.order.models.Store;

import com.symplified.order.models.HttpResponse;

import java.io.Serializable;

public class StoreStatusResponse extends HttpResponse implements Serializable {

    public StoreStatus data;

    public static class StoreStatus implements Serializable {
        public boolean isSnooze = false;
        public String snoozeReason = "";
        public String snoozeStartTime = "";
        public String snoozeEndTime = "";

        @Override
        public String toString() {
            return "{ isSnooze: " + isSnooze + ", snoozeReason: " + snoozeReason
                    + ", snoozeStartTime: " + snoozeStartTime + ", snoozeEndTime: " + snoozeEndTime
                    + " }";
        }
    }
}
