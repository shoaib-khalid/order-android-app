package com.ekedai.merchant.models.interfaces;

import com.ekedai.merchant.models.order.Order;

public interface OrderManager {
    void addOrderToOngoingTab(Order.OrderDetails orderDetails);
    void addOrderToHistoryTab(Order.OrderDetails orderDetails);
    void editOrder(Order order);
}
