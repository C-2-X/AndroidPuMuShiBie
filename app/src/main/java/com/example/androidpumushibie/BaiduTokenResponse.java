package com.example.androidpumushibie;

import com.google.gson.annotations.SerializedName;

public class BaiduTokenResponse {
    @SerializedName("access_token")
    public String accessToken;

    @SerializedName("expires_in")
    public long expiresIn;

    @SerializedName("error")
    public String error;

    @SerializedName("error_description")
    public String errorDescription;
}
