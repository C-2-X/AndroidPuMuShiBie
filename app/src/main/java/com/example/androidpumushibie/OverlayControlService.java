package com.example.androidpumushibie;

import android.app.Activity;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.nio.ByteBuffer;
import java.util.List;

public class OverlayControlService extends Service {

    public static final String ACTION_PROJECTION_RESULT = "com.example.androidpumushibie.PROJECTION_RESULT";
    public static final String EXTRA_RESULT_CODE = "result_code";
    public static final String EXTRA_RESULT_DATA = "result_data";

    private static final String CHANNEL_ID = "ocr_overlay_channel";
    private static final int NOTIFICATION_ID = 3001;
    private static final int CAPTURE_DELAY_MS = 120;
    private static final String ACTION_SETTINGS_CHANGED = "com.example.androidpumushibie.SETTINGS_CHANGED";

    private WindowManager windowManager;
    private SettingsManager settingsManager;
    private BaiduOcrRepository ocrRepository;
    private HistoryRepository historyRepository;

    private View handleView;
    private WindowManager.LayoutParams handleParams;
    private boolean handleAdded;

    private MediaProjectionManager projectionManager;
    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;
    private ImageReader imageReader;
    private int screenWidth;
    private int screenHeight;
    private int screenDensity;
    private boolean projectionInitialized;

    private SelectionOverlayView selectionOverlay;
    private WindowManager.LayoutParams overlayParams;

    private ResultPanelView resultPanel;
    private WindowManager.LayoutParams resultParams;

    private boolean isSelecting;
    private boolean isShowingResult;
    private boolean screenOff;
    private BroadcastReceiver screenReceiver;
    private DisplayManager displayManager;
    private DisplayManager.DisplayListener displayListener;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        settingsManager = new SettingsManager(this);
        historyRepository = new HistoryRepository(this);
        ocrRepository = new BaiduOcrRepository(this, settingsManager, historyRepository);
        projectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        displayManager = (DisplayManager) getSystemService(DISPLAY_SERVICE);

        displayListener = new DisplayManager.DisplayListener() {
            @Override
            public void onDisplayAdded(int displayId) {}

            @Override
            public void onDisplayRemoved(int displayId) {}

            @Override
            public void onDisplayChanged(int displayId) {
                handleDisplayChange();
            }
        };

        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getRealMetrics(metrics);
        screenWidth = metrics.widthPixels;
        screenHeight = metrics.heightPixels;
        screenDensity = metrics.densityDpi;

        screenOff = isScreenLocked();
        registerScreenReceiver();
        displayManager.registerDisplayListener(displayListener, mainHandler);

        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            if (ACTION_PROJECTION_RESULT.equals(intent.getAction())) {
                int resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, -1);
                Intent data = intent.getParcelableExtra(EXTRA_RESULT_DATA);
                if (resultCode == Activity.RESULT_OK && data != null) {
                    setupMediaProjection(resultCode, data);
                    startCaptureAndShowSelection();
                } else {
                    hideSelectionOverlay();
                    showHandle();
                    showToast(getString(R.string.toast_projection_permission_denied));
                }
                return START_STICKY;
            }

            if (ACTION_SETTINGS_CHANGED.equals(intent.getAction())) {
                refreshHandleAppearance();
                return START_STICKY;
            }
        }

        startForeground(NOTIFICATION_ID, buildNotification());
        showHandle();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        unregisterScreenReceiver();
        if (displayManager != null) {
            displayManager.unregisterDisplayListener(displayListener);
        }
        hideHandle();
        hideSelectionOverlay();
        hideResultPanel();
        cleanupMediaProjection();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public static void startService(Context context) {
        Intent intent = new Intent(context, OverlayControlService.class);
        context.startForegroundService(intent);
    }

    public static void stopService(Context context) {
        context.stopService(new Intent(context, OverlayControlService.class));
    }

    public static void notifySettingsChanged(Context context) {
        Intent intent = new Intent(context, OverlayControlService.class);
        intent.setAction(ACTION_SETTINGS_CHANGED);
        try {
            context.startForegroundService(intent);
        } catch (Exception ignored) {
        }
    }

    private void showToast(String message) {
        mainHandler.post(() ->
                Toast.makeText(OverlayControlService.this, message, Toast.LENGTH_SHORT).show()
        );
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setShowBadge(false);
            channel.setSound(null, null);
            channel.setVibrationPattern(null);
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification buildNotification() {
        Intent intent = new Intent(this, MainActivity.class);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, flags);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.notification_title))
                .setContentText(getString(R.string.notification_text))
                .setSmallIcon(android.R.drawable.ic_menu_search)
                .setOngoing(true)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void showHandle() {
        if (handleAdded) return;
        if (screenOff) return;

        int handleSizeDp = settingsManager.getHandleSizeDp();
        int alphaPercent = settingsManager.getHandleAlphaPercent();
        int alpha = Math.round(255f * alphaPercent / 100f);

        handleView = new View(this);
        handleView.setBackgroundColor(Color.argb(alpha, 100, 107, 122));

        int handleWidth = dpToPx(14);
        int handleLength = dpToPx(handleSizeDp);

        int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        int flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;

        handleParams = new WindowManager.LayoutParams(
                handleWidth,
                handleLength,
                type,
                flags,
                PixelFormat.TRANSLUCENT
        );

        boolean dockRight = settingsManager.isDockRight();
        int handleY = settingsManager.getHandleY();

        handleParams.gravity = Gravity.TOP | (dockRight ? Gravity.RIGHT : Gravity.LEFT);
        handleParams.x = 0;
        handleParams.y = clampHandleY(handleY);

        setupHandleTouch();
        windowManager.addView(handleView, handleParams);
        handleAdded = true;
    }

    private void hideHandle() {
        if (handleAdded && handleView != null) {
            try {
                windowManager.removeView(handleView);
            } catch (Exception ignored) {
            }
            handleAdded = false;
        }
    }

    private int clampHandleY(int y) {
        if (handleView == null) return y;
        int maxY = screenHeight - dpToPx(settingsManager.getHandleSizeDp());
        return Math.max(0, Math.min(y, maxY));
    }

    private void refreshHandleAppearance() {
        if (!handleAdded || handleView == null) return;

        int handleSizeDp = settingsManager.getHandleSizeDp();
        int alphaPercent = settingsManager.getHandleAlphaPercent();
        int alpha = Math.round(255f * alphaPercent / 100f);

        handleView.setBackgroundColor(Color.argb(alpha, 100, 107, 122));

        handleParams.height = dpToPx(handleSizeDp);
        handleParams.y = clampHandleY(handleParams.y);
        windowManager.updateViewLayout(handleView, handleParams);
    }

    private void setupHandleTouch() {
        final int clickThreshold = dpToPx(12);
        final float[] downRawYRef = new float[1];
        final int[] initialParamYRef = new int[1];

        handleView.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    downRawYRef[0] = event.getRawY();
                    initialParamYRef[0] = handleParams.y;
                    return true;
                case MotionEvent.ACTION_MOVE: {
                    int dy = Math.round(event.getRawY() - downRawYRef[0]);
                    handleParams.y = clampHandleY(initialParamYRef[0] + dy);
                    windowManager.updateViewLayout(handleView, handleParams);
                    return true;
                }
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL: {
                    int totalDy = Math.abs(Math.round(event.getRawY() - downRawYRef[0]));
                    if (totalDy < clickThreshold) {
                        onHandleClick();
                    } else {
                        boolean dockRight = (handleParams.gravity & Gravity.RIGHT) == Gravity.RIGHT;
                        settingsManager.saveHandlePosition(dockRight, handleParams.y);
                    }
                    return true;
                }
                default:
                    return false;
            }
        });
    }

    private void onHandleClick() {
        if (isSelecting || isShowingResult) return;

        hideHandle();
        isSelecting = true;

        if (hasMediaProjection()) {
            startCaptureAndShowSelection();
        } else {
            requestMediaProjectionPermission();
        }
    }

    private boolean hasMediaProjection() {
        return mediaProjection != null;
    }

    private void requestMediaProjectionPermission() {
        Intent intent = new Intent(this, MediaProjectionPermissionActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(intent);
        } catch (Exception e) {
            isSelecting = false;
            showHandle();
            showToast(getString(R.string.toast_capture_failed));
        }
    }

    private void setupMediaProjection(int resultCode, Intent data) {
        try {
            mediaProjection = projectionManager.getMediaProjection(resultCode, data);
        } catch (Exception e) {
            mediaProjection = null;
        }
    }

    private void startCaptureAndShowSelection() {
        if (mediaProjection == null) {
            isSelecting = false;
            showHandle();
            showToast(getString(R.string.toast_capture_failed));
            return;
        }

        if (!projectionInitialized) {
            initProjection();
        }

        performCaptureAndShowSelection();
    }

    private void initProjection() {
        try {
            imageReader = ImageReader.newInstance(
                    screenWidth, screenHeight,
                    PixelFormat.RGBA_8888, 2
            );

            virtualDisplay = mediaProjection.createVirtualDisplay(
                    "OCR_CAPTURE",
                    screenWidth, screenHeight, screenDensity,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    imageReader.getSurface(),
                    null, null
            );

            projectionInitialized = true;
        } catch (Exception e) {
            projectionInitialized = false;
        }
    }

    private void performCaptureAndShowSelection() {
        Bitmap bitmap = captureScreen();
        if (bitmap == null) {
            isSelecting = false;
            showHandle();
            showToast(getString(R.string.toast_capture_failed));
            return;
        }
        showSelectionOverlay(bitmap);
    }

    private Bitmap captureScreen() {
        if (imageReader == null) {
            return null;
        }
        try {
            Image image = imageReader.acquireLatestImage();
            if (image != null) {
                Bitmap bitmap = imageToBitmap(image);
                image.close();
                return bitmap;
            }
        } catch (Exception e) {
        }
        return null;
    }

    private void cleanupMediaProjection() {
        projectionInitialized = false;
        if (virtualDisplay != null) {
            try {
                virtualDisplay.release();
            } catch (Exception ignored) {
            }
            virtualDisplay = null;
        }
        if (imageReader != null) {
            try {
                imageReader.close();
            } catch (Exception ignored) {
            }
            imageReader = null;
        }
        if (mediaProjection != null) {
            try {
                mediaProjection.stop();
            } catch (Exception ignored) {
            }
            mediaProjection = null;
        }
    }

    private void handleDisplayChange() {
        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getRealMetrics(metrics);
        int newWidth = metrics.widthPixels;
        int newHeight = metrics.heightPixels;
        int newDensity = metrics.densityDpi;

        if (newWidth != screenWidth || newHeight != screenHeight) {
            screenWidth = newWidth;
            screenHeight = newHeight;
            screenDensity = newDensity;
            projectionInitialized = false;

            if (isSelecting || isShowingResult) {
                return;
            }

            cleanupMediaProjection();
            initProjection();
        }
    }

    private Bitmap imageToBitmap(Image image) {
        Image.Plane[] planes = image.getPlanes();
        if (planes == null || planes.length == 0) return null;

        ByteBuffer buffer = planes[0].getBuffer();
        int pixelStride = planes[0].getPixelStride();
        int rowStride = planes[0].getRowStride();
        int rowPadding = rowStride - pixelStride * image.getWidth();

        int width = image.getWidth();
        int height = image.getHeight();

        Bitmap bitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888);
        bitmap.copyPixelsFromBuffer(buffer);

        if (rowPadding == 0) {
            return bitmap;
        }
        Bitmap cropped = Bitmap.createBitmap(bitmap, 0, 0, width, height);
        if (bitmap != cropped) {
            bitmap.recycle();
        }
        return cropped;
    }

    private void showSelectionOverlay(Bitmap screenshot) {
        hideSelectionOverlay();
        hideResultPanel();
        isSelecting = true;

        selectionOverlay = new SelectionOverlayView(this, screenshot,
                new SelectionOverlayView.Listener() {
                    @Override
                    public void onCancel() {
                        hideSelectionOverlay();
                        isSelecting = false;
                        showHandle();
                    }

                    @Override
                    public void onSelectionConfirmed(Bitmap croppedBitmap) {
                        hideSelectionOverlay();
                        isSelecting = false;
                        doOcr(croppedBitmap);
                    }
                });

        int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        int flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_FULLSCREEN;

        overlayParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                type,
                flags,
                PixelFormat.TRANSLUCENT
        );

        try {
            windowManager.addView(selectionOverlay, overlayParams);
        } catch (Exception e) {
            hideSelectionOverlay();
            isSelecting = false;
            showHandle();
            showToast(getString(R.string.toast_capture_failed));
        }
    }

    private void hideSelectionOverlay() {
        if (selectionOverlay != null) {
            try {
                windowManager.removeView(selectionOverlay);
            } catch (Exception ignored) {
            }
            selectionOverlay = null;
        }
    }

    private void doOcr(Bitmap bitmap) {
        ocrRepository.recognize(bitmap, new BaiduOcrRepository.Callback() {
            @Override
            public void onSuccess(List<String> lines, String fullText) {
                mainHandler.post(() -> {
                    if (bitmap != null && !bitmap.isRecycled()) {
                        bitmap.recycle();
                    }
                    showResultPanel(lines);
                });
            }

            @Override
            public void onError(String message) {
                mainHandler.post(() -> {
                    if (bitmap != null && !bitmap.isRecycled()) {
                        bitmap.recycle();
                    }
                    showToast(message);
                    showHandle();
                });
            }
        });
    }

    private void showResultPanel(List<String> lines) {
        hideHandle();
        hideResultPanel();
        isShowingResult = true;

        resultPanel = new ResultPanelView(this, lines,
                new ResultPanelView.Listener() {
                    @Override
                    public void onCopyRequested(String text) {
                        copyToClipboard(text);
                        hideResultPanel();
                        isShowingResult = false;
                        showHandle();
                        showToast(getString(R.string.toast_copy_done));
                    }

                    @Override
                    public void onDismissRequested() {
                        hideResultPanel();
                        isShowingResult = false;
                        showHandle();
                    }
                });

        int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        int flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;

        resultParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                Math.min(screenHeight * 3 / 5, screenHeight - dpToPx(140)),
                type,
                flags,
                PixelFormat.TRANSLUCENT
        );

        resultParams.gravity = Gravity.TOP;
        resultParams.y = dpToPx(48);

        windowManager.addView(resultPanel, resultParams);

        setupResultPanelDrag();
    }

    private void setupResultPanelDrag() {
        if (resultPanel == null) return;

        View dragHandle = resultPanel.getDragHandleView();
        if (dragHandle == null) return;

        final float[] downRawYRef = new float[1];
        final int[] initialYRef = new int[1];

        dragHandle.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    downRawYRef[0] = event.getRawY();
                    initialYRef[0] = resultParams.y;
                    return true;
                case MotionEvent.ACTION_MOVE: {
                    int dy = Math.round(event.getRawY() - downRawYRef[0]);
                    resultParams.y = Math.max(0, initialYRef[0] + dy);
                    windowManager.updateViewLayout(resultPanel, resultParams);
                    return true;
                }
                case MotionEvent.ACTION_UP:
                    return true;
                default:
                    return false;
            }
        });
    }

    private void hideResultPanel() {
        if (resultPanel != null) {
            try {
                windowManager.removeView(resultPanel);
            } catch (Exception ignored) {
            }
            resultPanel = null;
        }
    }

    private void copyToClipboard(String text) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText("ocr_result", text));
        }
    }

    private void registerScreenReceiver() {
        screenReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent == null) return;
                String action = intent.getAction();
                if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                    screenOff = true;
                    hideHandle();
                } else if (Intent.ACTION_USER_PRESENT.equals(action)) {
                    screenOff = false;
                    if (!isSelecting && !isShowingResult) {
                        showHandle();
                    }
                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_USER_PRESENT);
        registerReceiver(screenReceiver, filter);
    }

    private void unregisterScreenReceiver() {
        if (screenReceiver != null) {
            try {
                unregisterReceiver(screenReceiver);
            } catch (Exception ignored) {
            }
            screenReceiver = null;
        }
    }

    private boolean isScreenLocked() {
        KeyguardManager kgm = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        if (kgm != null) {
            return kgm.isKeyguardLocked();
        }
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        if (pm != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            return !pm.isInteractive();
        }
        return false;
    }

    private int dpToPx(int dp) {
        return Math.round(dp * screenDensity / 160f);
    }
}
