package com.example.androidpumushibie;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.FieldMap;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface BaiduOcrApi {
    @GET("oauth/2.0/token")
    Call<BaiduTokenResponse> requestToken(
            @Query("grant_type") String grantType,
            @Query("client_id") String clientId,
            @Query("client_secret") String clientSecret
    );

    @FormUrlEncoded
    @POST("rest/2.0/ocr/v1/general_basic")
    Call<BaiduOcrResponse> generalBasic(
            @Query("access_token") String accessToken,
            @FieldMap(encoded = true) Map<String, String> fields
    );
}
