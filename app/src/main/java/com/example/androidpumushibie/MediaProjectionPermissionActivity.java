package com.example.androidpumushibie;

import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class MediaProjectionPermissionActivity extends AppCompatActivity {
    private static final int REQUEST_CAPTURE = 1201;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MediaProjectionManager projectionManager =
                (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        if (projectionManager == null) {
            finishWithoutAnimation();
            return;
        }
        try {
            startActivityForResult(projectionManager.createScreenCaptureIntent(), REQUEST_CAPTURE);
        } catch (Exception ignored) {
            finishWithoutAnimation();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CAPTURE) {
            Intent serviceIntent = new Intent(this, OverlayControlService.class);
            serviceIntent.setAction(OverlayControlService.ACTION_PROJECTION_RESULT);
            serviceIntent.putExtra(OverlayControlService.EXTRA_RESULT_CODE, resultCode);
            if (data != null) {
                serviceIntent.putExtra(OverlayControlService.EXTRA_RESULT_DATA, data);
            }
            ContextCompat.startForegroundService(this, serviceIntent);
        }
        finishWithoutAnimation();
    }

    private void finishWithoutAnimation() {
        finish();
        overridePendingTransition(0, 0);
    }
}
