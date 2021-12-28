package com.symplified.order.enums;

import com.symplified.order.utils.Utility;

public enum Status {
    BEING_DELIVERED,
    BEING_PREPARED,
    CANCELED_BY_CUSTOMER,
    DELIVERED_TO_CUSTOMER,
    CANCELED_BY_MERCHANT,
    PAYMENT_CONFIRMED,
    READY_FOR_DELIVERY,
    RECEIVED_AT_STORE,
    REFUNDED,
    REJECTED_BY_STORE,
    REQUESTING_DELIVERY_FAILED,
    AWAITING_PICKUP,
    FAILED;

    public static Status fromString(String name) {
        return Utility.getEnumFromString(Status.class, name);
    }
}
