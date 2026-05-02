package com.example.androidpumushibie;

import android.graphics.drawable.GradientDrawable;

public final class OverlayUiFactory {
    private OverlayUiFactory() {
    }

    public static GradientDrawable roundedRect(int color, float radiusPx) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radiusPx);
        return drawable;
    }
}
