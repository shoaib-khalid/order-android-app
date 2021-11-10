package com.symplified.order.apis;

import com.symplified.order.models.order.OrderDeliveryDetailsResponse;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.HeaderMap;
import retrofit2.http.Path;

public interface DeliveryApi {

    @GET("orders/getDeliveryRiderDetails/{orderId}")
    Call<OrderDeliveryDetailsResponse> getOrderDeliveryDetailsById(@HeaderMap Map<String,String> headers, @Path("orderId") String orderId);

}
