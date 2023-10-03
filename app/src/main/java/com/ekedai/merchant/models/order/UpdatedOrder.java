package com.ekedai.merchant.models.order;

import com.ekedai.merchant.enums.OrderStatus;

public class UpdatedOrder extends Order {
    OrderStatus nextCompletionStatus;
    String nextActionText;
}
