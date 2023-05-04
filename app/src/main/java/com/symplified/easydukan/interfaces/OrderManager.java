package com.symplified.easydukan.interfaces;

import com.symplified.easydukan.models.order.Order;

public interface OrderManager {
    void addOrderToOngoingTab(Order.OrderDetails orderDetails);
    void addOrderToHistoryTab(Order.OrderDetails orderDetails);
    void editOrder(Order order);
}
