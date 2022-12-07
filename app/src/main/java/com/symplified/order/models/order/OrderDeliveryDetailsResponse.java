package com.symplified.order.models.order;

import com.symplified.order.models.HttpResponse;

import java.io.Serializable;

public class OrderDeliveryDetailsResponse extends HttpResponse implements Serializable {

    public OrderDeliveryDetailsData data;

    public static class OrderDeliveryDetailsData implements Serializable{
        public String name;
        public String phoneNumber;
        public String plateNumber;
        public String trackingUrl;
        public String orderNumber;
        public Provider provider;
        public String riderStatus;
        public String airwayBill;

        public OrderDeliveryDetailsData(){}

        public OrderDeliveryDetailsData(String name, String phoneNumber, String plateNumber, String trackingUrl, String orderNumber, Provider provider, String riderStatus, String airwayBill) {
            this.name = name;
            this.phoneNumber = phoneNumber;
            this.plateNumber = plateNumber;
            this.trackingUrl = trackingUrl;
            this.orderNumber = orderNumber;
            this.provider = provider;
            this.riderStatus = riderStatus;
            this.airwayBill = airwayBill;
        }

        @Override
        public String toString() {
            return "OrderDeliveryDetailsData{" +
                    "name='" + name + '\'' +
                    ", phoneNumber='" + phoneNumber + '\'' +
                    ", plateNumber='" + plateNumber + '\'' +
                    ", trackingUrl='" + trackingUrl + '\'' +
                    ", orderNumber='" + orderNumber + '\'' +
                    ", provider=" + provider +
                    ", riderStatus='" + riderStatus + '\'' +
                    ", airwayBill='" + airwayBill + '\'' +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "OrderDeliveryDetailsResponse{" +
                "data=" + data +
                '}';
    }

    public static  class Provider implements Serializable{
        public int id;
        public String name;
        public String providerImage;

        public Provider(){}

        public Provider(int id, String name, String providerImage) {
            this.id = id;
            this.name = name;
            this.providerImage = providerImage;
        }
    }
}
