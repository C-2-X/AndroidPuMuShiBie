package com.example.androidpumushibie;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.text.TextUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class BaiduOcrRepository {
    private static final String CACHE_PREFS = "baidu_ocr_cache";
    private static final String KEY_TOKEN = "access_token";
    private static final String KEY_TOKEN_EXPIRE_AT = "token_expire_at";
    private static final String KEY_TOKEN_API_KEY = "token_api_key";
    private static final long TOKEN_REFRESH_EARLY_MS = 5 * 60 * 1000L;

    public interface Callback {
        void onSuccess(List<String> lines, String fullText);

        void onError(String message);
    }

    private final Context context;
    private final SettingsManager settingsManager;
    private final HistoryRepository historyRepository;
    private final SharedPreferences cachePreferences;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final BaiduOcrApi api;

    public BaiduOcrRepository(Context context,
                              SettingsManager settingsManager,
                              HistoryRepository historyRepository) {
        this.context = context.getApplicationContext();
        this.settingsManager = settingsManager;
        this.historyRepository = historyRepository;
        cachePreferences = this.context.getSharedPreferences(CACHE_PREFS, Context.MODE_PRIVATE);

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .callTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://aip.baidubce.com/")
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        api = retrofit.create(BaiduOcrApi.class);
    }

    public void recognize(Bitmap bitmap, Callback callback) {
        String apiKey = settingsManager.getApiKey();
        String secretKey = settingsManager.getSecretKey();
        boolean autoDetect = settingsManager.isAutoDetectLanguage();
        if (TextUtils.isEmpty(apiKey) || TextUtils.isEmpty(secretKey)) {
            callback.onError(context.getString(R.string.toast_need_api_credentials));
            return;
        }
        if (!PermissionHelper.isNetworkAvailable(context)) {
            callback.onError(context.getString(R.string.toast_network_unavailable));
            return;
        }

        executorService.execute(() -> {
            try {
                String accessToken = getValidAccessToken(apiKey, secretKey);
                if (TextUtils.isEmpty(accessToken)) {
                    callback.onError(context.getString(R.string.toast_token_invalid));
                    return;
                }

                Map<String, String> fields = new HashMap<>();
                fields.put("image", encodeBitmap(bitmap));
                fields.put("language_type", "CHN_ENG");
                fields.put("detect_direction", "true");
                fields.put("paragraph", "false");
                fields.put("probability", "false");
                if (autoDetect) {
                    fields.put("detect_language", "true");
                }

                Response<BaiduOcrResponse> response = api.generalBasic(accessToken, fields).execute();
                BaiduOcrResponse body = response.body();
                if (!response.isSuccessful() || body == null) {
                    callback.onError(context.getString(R.string.toast_capture_failed));
                    return;
                }
                if (body.errorCode != null) {
                    callback.onError(TextUtils.isEmpty(body.errorMessage)
                            ? context.getString(R.string.toast_capture_failed)
                            : body.errorMessage);
                    return;
                }

                List<String> lines = new ArrayList<>();
                if (body.wordsResult != null) {
                    for (BaiduOcrResponse.WordItem item : body.wordsResult) {
                        if (item != null && !TextUtils.isEmpty(item.words)) {
                            lines.add(item.words.trim());
                        }
                    }
                }
                if (lines.isEmpty()) {
                    callback.onError(context.getString(R.string.toast_no_text));
                    return;
                }

                String fullText = TextUtils.join("\n", lines);
                historyRepository.addRecord(fullText);
                callback.onSuccess(lines, fullText);
            } catch (SocketTimeoutException e) {
                callback.onError(context.getString(R.string.toast_ocr_timeout));
            } catch (IOException e) {
                callback.onError(context.getString(R.string.toast_capture_failed));
            } catch (Exception e) {
                callback.onError(context.getString(R.string.toast_capture_failed));
            }
        });
    }

    private String getValidAccessToken(String apiKey, String secretKey) throws IOException {
        long now = System.currentTimeMillis();
        String cachedApiKey = cachePreferences.getString(KEY_TOKEN_API_KEY, "");
        String cachedToken = cachePreferences.getString(KEY_TOKEN, "");
        long expireAt = cachePreferences.getLong(KEY_TOKEN_EXPIRE_AT, 0L);
        if (apiKey.equals(cachedApiKey)
                && !TextUtils.isEmpty(cachedToken)
                && expireAt > now + TOKEN_REFRESH_EARLY_MS) {
            return cachedToken;
        }

        Response<BaiduTokenResponse> response = api.requestToken(
                "client_credentials",
                apiKey,
                secretKey
        ).execute();
        BaiduTokenResponse body = response.body();
        if (!response.isSuccessful() || body == null || TextUtils.isEmpty(body.accessToken)) {
            clearTokenCache();
            return null;
        }
        long expiresIn = body.expiresIn <= 0 ? 30L * 24 * 60 * 60 : body.expiresIn;
        long expireAtMs = now + expiresIn * 1000L;
        cachePreferences.edit()
                .putString(KEY_TOKEN, body.accessToken)
                .putLong(KEY_TOKEN_EXPIRE_AT, expireAtMs)
                .putString(KEY_TOKEN_API_KEY, apiKey)
                .apply();
        return body.accessToken;
    }

    private void clearTokenCache() {
        cachePreferences.edit().clear().apply();
    }

    private String encodeBitmap(Bitmap bitmap) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream);
        String base64 = android.util.Base64.encodeToString(
                outputStream.toByteArray(),
                android.util.Base64.NO_WRAP
        );
        return URLEncoder.encode(base64, StandardCharsets.UTF_8.name());
    }
}
