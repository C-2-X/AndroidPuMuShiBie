package com.example.androidpumushibie;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

public class SelectionOverlayView extends FrameLayout {
    public interface Listener {
        void onCancel();

        void onSelectionConfirmed(Bitmap croppedBitmap);
    }

    private static final int MODE_IDLE = 0;
    private static final int MODE_DRAW = 1;
    private static final int MODE_MOVE = 2;
    private static final int MODE_LEFT_TOP = 3;
    private static final int MODE_RIGHT_TOP = 4;
    private static final int MODE_LEFT_BOTTOM = 5;
    private static final int MODE_RIGHT_BOTTOM = 6;
    private static final int CONFIRM_DELAY_MS = 360;

    private final Bitmap screenshot;
    private final Listener listener;
    private final Paint maskPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint handlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hintPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF selectionRect = new RectF();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final float minSizePx;
    private final float handleRadiusPx;
    private final Runnable confirmRunnable;

    private float downX;
    private float downY;
    private float lastX;
    private float lastY;
    private int touchMode = MODE_IDLE;
    private boolean loading;
    private boolean confirmed;
    private int activePointerId = INVALID_POINTER_ID;
    private static final int INVALID_POINTER_ID = -1;

    public SelectionOverlayView(@NonNull Context context,
                                @NonNull Bitmap screenshot,
                                @NonNull Listener listener) {
        super(context);
        setWillNotDraw(false);
        this.screenshot = screenshot;
        this.listener = listener;
        minSizePx = dpToPx(36);
        handleRadiusPx = dpToPx(10);

        confirmRunnable = () -> {
            if (listener != null && hasValidSelection() && !confirmed) {
                confirmed = true;
                loading = true;
                invalidate();
                listener.onSelectionConfirmed(cropSelection());
            }
        };

        maskPaint.setColor(0xA6000000);
        borderPaint.setColor(Color.WHITE);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(dpToPx(2));
        handlePaint.setColor(Color.WHITE);
        hintPaint.setColor(Color.WHITE);
        hintPaint.setTextAlign(Paint.Align.CENTER);
        hintPaint.setTypeface(Typeface.DEFAULT_BOLD);
        hintPaint.setTextSize(dpToPx(15));

        TextView cancelView = new TextView(context);
        cancelView.setText(R.string.selection_cancel);
        cancelView.setTextColor(Color.WHITE);
        cancelView.setPadding(dpToPxInt(14), dpToPxInt(10), dpToPxInt(14), dpToPxInt(10));
        cancelView.setBackground(OverlayUiFactory.roundedRect(0x66000000, dpToPx(18)));
        cancelView.setOnClickListener(v -> {
            handler.removeCallbacks(confirmRunnable);
            listener.onCancel();
        });
        LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        params.leftMargin = dpToPxInt(20);
        params.topMargin = dpToPxInt(42);
        addView(cancelView, params);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawBitmap(screenshot, null, new Rect(0, 0, getWidth(), getHeight()), null);
        if (!hasValidSelection()) {
            canvas.drawRect(0, 0, getWidth(), getHeight(), maskPaint);
            canvas.drawText(getContext().getString(R.string.selection_hint), getWidth() / 2f,
                    getHeight() / 2f, hintPaint);
            return;
        }

        canvas.drawRect(0, 0, getWidth(), selectionRect.top, maskPaint);
        canvas.drawRect(0, selectionRect.top, selectionRect.left, selectionRect.bottom, maskPaint);
        canvas.drawRect(selectionRect.right, selectionRect.top, getWidth(), selectionRect.bottom, maskPaint);
        canvas.drawRect(0, selectionRect.bottom, getWidth(), getHeight(), maskPaint);
        canvas.drawRect(selectionRect, borderPaint);

        drawHandle(canvas, selectionRect.left, selectionRect.top);
        drawHandle(canvas, selectionRect.right, selectionRect.top);
        drawHandle(canvas, selectionRect.left, selectionRect.bottom);
        drawHandle(canvas, selectionRect.right, selectionRect.bottom);

        if (loading) {
            canvas.drawRect(0, 0, getWidth(), getHeight(), maskPaint);
            canvas.drawText(getContext().getString(R.string.recognizing_hint), getWidth() / 2f, getHeight() / 2f, hintPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (loading) {
            return true;
        }

        int pointerCount = event.getPointerCount();
        if (pointerCount > 1) {
            return true;
        }

        int actionIndex = event.getActionIndex();
        int pointerId = event.getPointerId(actionIndex);

        float x = event.getX(actionIndex);
        float y = event.getY(actionIndex);

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                handler.removeCallbacks(confirmRunnable);
                activePointerId = pointerId;
                downX = x;
                downY = y;
                lastX = x;
                lastY = y;
                touchMode = resolveTouchMode(x, y);
                if (touchMode == MODE_DRAW) {
                    selectionRect.set(x, y, x, y);
                }
                if (confirmed) {
                    confirmed = false;
                    loading = false;
                }
                invalidate();
                return true;
            case MotionEvent.ACTION_MOVE:
                if (pointerId != activePointerId) {
                    return true;
                }
                handleMove(x, y);
                lastX = x;
                lastY = y;
                invalidate();
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (pointerId != activePointerId) {
                    return true;
                }
                activePointerId = INVALID_POINTER_ID;
                if (hasValidSelection()) {
                    handler.postDelayed(confirmRunnable, 360);
                }
                touchMode = MODE_IDLE;
                return true;
            default:
                return super.onTouchEvent(event);
        }
    }

    private void handleMove(float x, float y) {
        switch (touchMode) {
            case MODE_DRAW:
                selectionRect.set(Math.min(downX, x), Math.min(downY, y),
                        Math.max(downX, x), Math.max(downY, y));
                constrainRect(selectionRect);
                break;
            case MODE_MOVE:
                float dx = x - lastX;
                float dy = y - lastY;
                selectionRect.offset(dx, dy);
                if (selectionRect.left < 0) {
                    selectionRect.offset(-selectionRect.left, 0);
                }
                if (selectionRect.top < 0) {
                    selectionRect.offset(0, -selectionRect.top);
                }
                if (selectionRect.right > getWidth()) {
                    selectionRect.offset(getWidth() - selectionRect.right, 0);
                }
                if (selectionRect.bottom > getHeight()) {
                    selectionRect.offset(0, getHeight() - selectionRect.bottom);
                }
                break;
            case MODE_LEFT_TOP:
                selectionRect.left = clamp(x, 0, selectionRect.right - minSizePx);
                selectionRect.top = clamp(y, 0, selectionRect.bottom - minSizePx);
                break;
            case MODE_RIGHT_TOP:
                selectionRect.right = clamp(x, selectionRect.left + minSizePx, getWidth());
                selectionRect.top = clamp(y, 0, selectionRect.bottom - minSizePx);
                break;
            case MODE_LEFT_BOTTOM:
                selectionRect.left = clamp(x, 0, selectionRect.right - minSizePx);
                selectionRect.bottom = clamp(y, selectionRect.top + minSizePx, getHeight());
                break;
            case MODE_RIGHT_BOTTOM:
                selectionRect.right = clamp(x, selectionRect.left + minSizePx, getWidth());
                selectionRect.bottom = clamp(y, selectionRect.top + minSizePx, getHeight());
                break;
            default:
                break;
        }
    }

    private int resolveTouchMode(float x, float y) {
        if (isNear(selectionRect.left, selectionRect.top, x, y)) {
            return MODE_LEFT_TOP;
        }
        if (isNear(selectionRect.right, selectionRect.top, x, y)) {
            return MODE_RIGHT_TOP;
        }
        if (isNear(selectionRect.left, selectionRect.bottom, x, y)) {
            return MODE_LEFT_BOTTOM;
        }
        if (isNear(selectionRect.right, selectionRect.bottom, x, y)) {
            return MODE_RIGHT_BOTTOM;
        }
        if (selectionRect.contains(x, y)) {
            return MODE_MOVE;
        }
        return MODE_DRAW;
    }

    private Bitmap cropSelection() {
        float scaleX = screenshot.getWidth() / (float) getWidth();
        float scaleY = screenshot.getHeight() / (float) getHeight();
        int left = Math.max(0, Math.round(selectionRect.left * scaleX));
        int top = Math.max(0, Math.round(selectionRect.top * scaleY));
        int right = Math.min(screenshot.getWidth(), Math.round(selectionRect.right * scaleX));
        int bottom = Math.min(screenshot.getHeight(), Math.round(selectionRect.bottom * scaleY));
        int width = Math.max(1, right - left);
        int height = Math.max(1, bottom - top);
        return Bitmap.createBitmap(screenshot, left, top, width, height);
    }

    private void drawHandle(Canvas canvas, float x, float y) {
        canvas.drawCircle(x, y, handleRadiusPx, handlePaint);
    }

    private void constrainRect(RectF rect) {
        rect.left = clamp(rect.left, 0, getWidth());
        rect.top = clamp(rect.top, 0, getHeight());
        rect.right = clamp(rect.right, 0, getWidth());
        rect.bottom = clamp(rect.bottom, 0, getHeight());
    }

    private boolean hasValidSelection() {
        return selectionRect.width() >= minSizePx && selectionRect.height() >= minSizePx;
    }

    private boolean isNear(float targetX, float targetY, float x, float y) {
        return Math.hypot(targetX - x, targetY - y) <= handleRadiusPx * 2;
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private float dpToPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }

    private int dpToPxInt(float dp) {
        return Math.round(dpToPx(dp));
    }
}
