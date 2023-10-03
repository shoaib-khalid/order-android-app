package com.ekedai.merchant.models.interfaces;

import com.ekedai.merchant.models.order.Order;

public interface OrderObserver {
    void onOrderReceived(Order.OrderDetails orderDetails);
    void setOrderManager(OrderManager orderManager);
}
