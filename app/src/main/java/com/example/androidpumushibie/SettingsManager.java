package com.example.androidpumushibie;

import android.content.Context;
import android.content.SharedPreferences;

public class SettingsManager {
    private static final String PREFS_NAME = "global_ocr_settings";
    private static final String KEY_API_KEY = "api_key";
    private static final String KEY_SECRET_KEY = "secret_key";
    private static final String KEY_AUTO_DETECT_LANGUAGE = "auto_detect_language";
    private static final String KEY_HANDLE_SIZE_DP = "handle_size_dp";
    private static final String KEY_HANDLE_ALPHA_PERCENT = "handle_alpha_percent";
    private static final String KEY_SERVICE_ENABLED = "service_enabled";
    private static final String KEY_DOCK_RIGHT = "dock_right";
    private static final String KEY_HANDLE_Y = "handle_y";
    private final SharedPreferences preferences;

    public SettingsManager(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public String getApiKey() {
        return preferences.getString(KEY_API_KEY, "");
    }

    public void setApiKey(String value) {
        preferences.edit().putString(KEY_API_KEY, value == null ? "" : value.trim()).apply();
    }

    public String getSecretKey() {
        return preferences.getString(KEY_SECRET_KEY, "");
    }

    public void setSecretKey(String value) {
        preferences.edit().putString(KEY_SECRET_KEY, value == null ? "" : value.trim()).apply();
    }

    public boolean isAutoDetectLanguage() {
        return preferences.getBoolean(KEY_AUTO_DETECT_LANGUAGE, false);
    }

    public void setAutoDetectLanguage(boolean enabled) {
        preferences.edit().putBoolean(KEY_AUTO_DETECT_LANGUAGE, enabled).apply();
    }

    public int getHandleSizeDp() {
        return preferences.getInt(KEY_HANDLE_SIZE_DP, 64);
    }

    public void setHandleSizeDp(int sizeDp) {
        preferences.edit().putInt(KEY_HANDLE_SIZE_DP, sizeDp).apply();
    }

    public int getHandleAlphaPercent() {
        return preferences.getInt(KEY_HANDLE_ALPHA_PERCENT, 45);
    }

    public void setHandleAlphaPercent(int alphaPercent) {
        preferences.edit().putInt(KEY_HANDLE_ALPHA_PERCENT, alphaPercent).apply();
    }

    public boolean isServiceEnabled() {
        return preferences.getBoolean(KEY_SERVICE_ENABLED, false);
    }

    public void setServiceEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_SERVICE_ENABLED, enabled).apply();
    }

    public boolean isDockRight() {
        return preferences.getBoolean(KEY_DOCK_RIGHT, true);
    }

    public int getHandleY() {
        return preferences.getInt(KEY_HANDLE_Y, 280);
    }

    public void saveHandlePosition(boolean dockRight, int y) {
        preferences.edit()
                .putBoolean(KEY_DOCK_RIGHT, dockRight)
                .putInt(KEY_HANDLE_Y, y)
                .apply();
    }
}
