package com.example.androidpumushibie;

import android.accessibilityservice.AccessibilityService;
import android.os.Build;
import android.view.accessibility.AccessibilityEvent;

public class OcrAccessibilityService extends AccessibilityService {
    private static OcrAccessibilityService instance;

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
    }

    @Override
    public void onInterrupt() {
    }

    @Override
    public void onDestroy() {
        instance = null;
        super.onDestroy();
    }

    public static OcrAccessibilityService getInstance() {
        return instance;
    }

    public boolean isScreenshotAvailable() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.P;
    }
}