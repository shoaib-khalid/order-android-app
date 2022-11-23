package com.symplified.easydukan.models.order;

import com.symplified.easydukan.enums.OrderStatus;

public class UpdatedOrder extends Order {
    OrderStatus nextCompletionStatus;
    String nextActionText;
}
