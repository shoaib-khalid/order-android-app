package com.symplified.easydukan.interfaces;

import com.symplified.easydukan.models.order.Order;

public interface OrderObserver {
    void onOrderReceived(Order.OrderDetails orderDetails);
    void setOrderManager(OrderManager orderManager);
}
