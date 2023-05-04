package com.symplified.easydukan.networking.apis;

import com.symplified.easydukan.models.staff.PasswordChangeRequest;
import com.symplified.easydukan.models.staff.RegisterStaffMemberRequest;
import com.symplified.easydukan.models.staff.RegisterStaffMemberResponse;
import com.symplified.easydukan.models.staff.StaffMemberListResponse;
import com.symplified.easydukan.models.staff.shift.EndShiftRequest;
import com.symplified.easydukan.models.staff.shift.SummaryDetailsResponse;

import io.reactivex.Observable;
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

    @GET("stores/{storeId}/users/?page=0&pageSize=1000000")
    Observable<StaffMemberListResponse> getStaffMembersByStoreIdObservable(@Path("storeId") String storeId);

    @GET("stores/{storeId}/users/shiftSummary/{userId}")
    Call<SummaryDetailsResponse> getShiftSummary(
            @Path("storeId") String storeId,
            @Path("userId") String userId
    );

    @POST("stores/{storeId}/users/register")
    Call<RegisterStaffMemberResponse> addStaffMember(
            @Path("storeId") String storeId,
            @Body RegisterStaffMemberRequest body
    );

    @POST("stores/{storeId}/users/endShift")
    Call<Void> endShift(
            @Path("storeId") String storeId,
            @Body EndShiftRequest body
    );

    @PUT("stores/{storeId}/users/{userId}")
    Call<Void> changePassword(
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
