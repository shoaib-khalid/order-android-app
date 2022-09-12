package com.symplified.order.apis;

import com.symplified.order.models.HttpResponse;
import com.symplified.order.models.Store.StoreResponse;
import com.symplified.order.models.item.Item;
import com.symplified.order.models.item.ItemResponse;
import com.symplified.order.models.item.UpdatedItem;
import com.symplified.order.models.order.Order;
import com.symplified.order.models.order.OrderDeliveryDetailsResponse;
import com.symplified.order.models.order.OrderDetailsResponse;
import com.symplified.order.models.order.OrderResponse;
import com.symplified.order.models.order.OrderUpdateResponse;
import com.symplified.order.models.order.UpdatedOrder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Callback;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.HeaderMap;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.Url;

public interface OrderApi {

    @GET("orders/search?completionStatus=PAYMENT_CONFIRMED&completionStatus=RECEIVED_AT_STORE&sortingOrder=ASC")
    Call<OrderDetailsResponse> getNewOrdersByClientId(@Query("clientId") String clientId);

    @GET("orders/search?completionStatus=PAYMENT_CONFIRMED&completionStatus=RECEIVED_AT_STORE&sortingOrder=ASC")
    Call<OrderDetailsResponse> getNewOrdersByClientIdAndInvoiceId(@Query("clientId") String clientId, @Query("invoiceId") String invoiceId);

    @GET("orders/search?completionStatus=BEING_PREPARED&completionStatus=AWAITING_PICKUP&completionStatus=BEING_DELIVERED&sortingOrder=ASC")
    Call<OrderDetailsResponse> getOngoingOrdersByClientId (@HeaderMap Map<String, String> headers, @Query("clientId") String clientId);

    @GET("orders/search?completionStatus=CANCELED_BY_MERCHANT&completionStatus=DELIVERED_TO_CUSTOMER&completionStatus=CANCELED_BY_CUSTOMER")
    Call<OrderDetailsResponse> getSentOrdersByClientId (@HeaderMap Map<String, String> headers, @Query("clientId") String clientId, @Query("from") String from, @Query("to") String to);

    @GET("orders/{orderId}/items")
    Call<ItemResponse> getItemsForOrder(@Path("orderId") String storeId);

    @PUT("orders/{orderId}/completion-status-updates")
    Call<OrderUpdateResponse> updateOrderStatus(@HeaderMap Map<String, String> headers, @Body Order.OrderUpdate body, @Path("orderId") String orderId);

    @GET("orders/details/{orderId}")
    Call<ResponseBody> getOrderStatusDetails(@HeaderMap Map<String,String> headers, @Path("orderId") String orderId);

    @GET("orders/{orderId}")
    Call<Order.OrderByIdResponse> getOrderById(@HeaderMap Map<String,String> headers, @Path(value = "orderId", encoded = true) String orderId);

    @PUT("orders/reviseitem/{orderId}")
    Call<HttpResponse> reviseOrderItem(@HeaderMap Map<String,String> headers, @Path("orderId") String orderId, @Body List<UpdatedItem> bodyOrderItemList);
}
