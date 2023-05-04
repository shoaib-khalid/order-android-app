package com.symplified.easydukan.networking.apis;

import com.symplified.easydukan.models.HttpResponse;
import com.symplified.easydukan.models.item.ItemsResponse;
import com.symplified.easydukan.models.item.UpdatedItem;
import com.symplified.easydukan.models.order.Order;
import com.symplified.easydukan.models.order.OrderDetailsResponse;
import com.symplified.easydukan.models.order.OrderUpdateResponse;
import com.symplified.easydukan.models.qrcode.QrCodeRequest;
import com.symplified.easydukan.models.qrcode.QrCodeResponse;
import com.symplified.easydukan.models.qrorders.ConsolidatedOrder;
import com.symplified.easydukan.models.qrorders.ConsolidatedOrdersResponse;

import java.util.List;

import io.reactivex.Observable;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface OrderApi {

    @GET("orders/search?completionStatus=PAYMENT_CONFIRMED,RECEIVED_AT_STORE&sortingOrder=ASC&pageSize=1000000")
    Call<OrderDetailsResponse> searchNewOrdersByClientId(@Query("clientId") String clientId);

    @GET("orders/search?completionStatus=PAYMENT_CONFIRMED,RECEIVED_AT_STORE&sortingOrder=ASC&pageSize=1000000")
    Call<OrderDetailsResponse> searchNewOrdersByClientIdAndInvoiceId(
            @Query("clientId") String clientId,
            @Query("invoiceId") String invoiceId
    );

    @GET("orders/search?completionStatus=PAYMENT_CONFIRMED,RECEIVED_AT_STORE&sortingOrder=ASC&pageSize=1000000")
    Call<OrderDetailsResponse> searchNewOrdersByClientIdAndOrderId(
            @Query("clientId") String clientId,
            @Query("orderId") String orderId
    );

    @GET("orders/search?completionStatus=BEING_PREPARED,AWAITING_PICKUP,BEING_DELIVERED&sortingOrder=ASC&pageSize=1000000")
    Call<OrderDetailsResponse> searchOngoingOrdersByClientId(@Query("clientId") String clientId);

    @GET("orders/search?completionStatus=CANCELED_BY_MERCHANT,DELIVERED_TO_CUSTOMER,CANCELED_BY_CUSTOMER")
    Call<OrderDetailsResponse> searchSentOrdersByClientId(
            @Query("clientId") String clientId,
            @Query("from") String from,
            @Query("to") String to
    );

    @GET("orders/{orderId}/items")
    Call<ItemsResponse> getItemsForOrder(@Path("orderId") String orderId);

    @PUT("orders/{orderId}/completion-status-updates")
    Call<OrderUpdateResponse> updateOrderStatus(@Body Order.OrderUpdate body, @Path("orderId") String orderId);

    @GET("orders/details/{orderId}")
    Call<ResponseBody> getOrderStatusDetails(@Path("orderId") String orderId);

    @GET("orders/{orderId}")
    Call<Order.OrderByIdResponse> getOrderById(@Path(value = "orderId", encoded = true) String orderId);

    @PUT("orders/reviseitem/{orderId}")
    Call<HttpResponse> reviseOrderItem(@Path("orderId") String orderId, @Body List<UpdatedItem> bodyOrderItemList);

    @POST("qrcode/generate")
    Call<QrCodeResponse> generateQrCode(@Body QrCodeRequest request);

    @GET("qrcode/preauth-generate")
    Call<ResponseBody> verifyQrCodeAvailability(@Query("storeId") String storeId);

    @GET("qrorder/pending?pageSize=100000&sortingOrder=ASC")
    Observable<ConsolidatedOrdersResponse> getPendingConsolidatedOrders(
            @Query("storeId") String storeId
    );

    @PUT("qrorder/update")
    Call<Void> consolidateOrder(@Body ConsolidatedOrder order);
}
