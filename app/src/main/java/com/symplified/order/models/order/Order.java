package com.symplified.order.models.order;

import androidx.annotation.NonNull;

import java.io.Serializable;
import java.util.List;

public class Order implements Serializable {
    public String id;
    public String storeId;
    public double subTotal;
    public double deliveryCharges;
    public double total;
    public String completionStatus;
    public String paymentStatus;
    public String customerNotes;
    public String privateAdminNotes;
    public String cartId;
    public String customerId;
    public String created;
    public String updated;
    public String invoiceId;
    public double klCommission;
    public double storeServiceCharges;
    public double storeShare;
    public Object paymentType;
    public OrderShipmentDetail orderShipmentDetail;
    public OrderPaymentDetail orderPaymentDetail;


    @NonNull
    @Override
    public String toString() {
        return super.toString();
    }

    public static class OrderShipmentDetail implements Serializable{
        public String receiverName;
        public String phoneNumber;
        public String address;
        public String city;
        public String zipcode;
        public String email;
        public int deliveryProviderId;
        public String state;
        public String country;
        public Object trackingUrl;
        public String orderId;
        public boolean storePickup;
        public Object merchantTrackingUrl;
        public Object customerTrackingUrl;
        public Object trackingNumber;
    }

    public static class OrderPaymentDetail implements Serializable{
        public String accountName;
        public String gatewayId;
        public Object couponId;
        public Object time;
        public String orderId;
        public String deliveryQuotationReferenceId;
        public double deliveryQuotationAmount;
    }

    public static class OrderList implements Serializable{
        public List<Order> content;
    }

}