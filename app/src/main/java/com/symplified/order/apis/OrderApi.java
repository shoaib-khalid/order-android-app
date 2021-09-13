package com.symplified.order.apis;

import com.symplified.order.models.Store.StoreResponse;
import com.symplified.order.models.order.OrderResponse;

import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.HeaderMap;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface OrderApi {

//    https://api.symplified.it/order-service/v1/orders
    @GET("orders?receiverName=&phoneNumber=&completionStatus=PAYMENT_CONFIRMED&pageSize=10&")
    Call<ResponseBody> getNewOrders(@HeaderMap Map<String, String> headers, @Query("storeId") String storeId);

    @GET("orders?receiverName=&phoneNumber=&completionStatus=BEING_PREPARED&pageSize=10&")
    Call<ResponseBody> getProcessedOrders (@HeaderMap Map<String, String> headers, @Query("storeId") String storeId);


    @GET("orders?receiverName=&phoneNumber=&completionStatus=BEING_DELIVERED&pageSize=10&")
    Call<ResponseBody> getSentOrders (@HeaderMap Map<String, String> headers, @Query("storeId") String storeId);

}
