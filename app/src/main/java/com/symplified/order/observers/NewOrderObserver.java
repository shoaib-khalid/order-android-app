package com.symplified.order.observers;

import com.symplified.order.models.order.Order;

public interface NewOrderObserver {
    void onNewOrderReceived(Order.OrderDetails orderDetails);
}
