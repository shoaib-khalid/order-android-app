package com.symplified.order.interfaces;

import com.symplified.order.models.order.Order;

public interface OrderObserver {
    void onOrderReceived(Order.OrderDetails orderDetails);
    void setOrderManager(OrderManager orderManager);
}
