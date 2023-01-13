package com.symplified.order.apis;

import com.symplified.order.models.staff.PasswordChangeRequest;
import com.symplified.order.models.staff.RegisterStaffMemberRequest;
import com.symplified.order.models.staff.RegisterStaffMemberResponse;
import com.symplified.order.models.staff.SingleStaffMemberResponse;
import com.symplified.order.models.staff.StaffMemberListResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface StaffApi {

    @GET("stores/{storeId}/users/?page=0&pageSize=1000000")
    Call<StaffMemberListResponse> getStaffMembersByStoreId(@Path("storeId") String storeId);

    @POST("stores/{storeId}/users/register")
    Call<RegisterStaffMemberResponse> addStaffMember(
            @Path("storeId") String storeId,
            @Body RegisterStaffMemberRequest body
    );

    @PUT("stores/{storeId}/users/{userId}")
    Call<SingleStaffMemberResponse> changePassword(
            @Path("storeId") String storeId,
            @Path("userId") String userId,
            @Body PasswordChangeRequest body
    );

    @DELETE("stores/{storeId}/users/{userId}")
    Call<Void> deleteStaffMember(
            @Path("storeId") String storeId,
            @Path("userId") String userId
    );
}
