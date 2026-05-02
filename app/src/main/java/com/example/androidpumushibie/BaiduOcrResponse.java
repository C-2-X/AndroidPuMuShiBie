package com.example.androidpumushibie;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class BaiduOcrResponse {
    @SerializedName("words_result")
    public List<WordItem> wordsResult;

    @SerializedName("words_result_num")
    public int wordsResultNum;

    @SerializedName("error_code")
    public Integer errorCode;

    @SerializedName("error_msg")
    public String errorMessage;

    public static class WordItem {
        @SerializedName("words")
        public String words;
    }
}
