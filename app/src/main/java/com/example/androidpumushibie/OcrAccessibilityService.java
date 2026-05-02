package com.example.androidpumushibie;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;

public class OcrAccessibilityService extends AccessibilityService {
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // This service acts as a lightweight permission anchor for full-screen scenarios.
    }

    @Override
    public void onInterrupt() {
        // No-op.
    }
}
