package com.example.androidpumushibie;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.androidpumushibie.databinding.ActivityMainBinding;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_NOTIFICATION_PERMISSION = 2001;

    private ActivityMainBinding binding;
    private SettingsManager settingsManager;
    private HistoryRepository historyRepository;
    private HistoryAdapter historyAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        settingsManager = new SettingsManager(this);
        historyRepository = new HistoryRepository(this);
        historyAdapter = new HistoryAdapter();

        setupProviderSpinner();
        setupHistoryList();
        setupListeners();
        loadStoredSettings();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshHistory();
        updateSliderLabels();
        updateStatusUi();
    }

    private void setupProviderSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                Collections.singletonList(getString(R.string.provider_baidu))
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.providerSpinner.setAdapter(adapter);
        binding.providerSpinner.setEnabled(false);
    }

    private void setupHistoryList() {
        binding.historyRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.historyRecyclerView.setNestedScrollingEnabled(false);
        binding.historyRecyclerView.setAdapter(historyAdapter);

        historyAdapter.setOnItemDeleteListener(position -> {
            historyRepository.deleteRecord(position);
            refreshHistory();
        });

        binding.clearHistoryButton.setOnClickListener(v -> {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle(R.string.clear_history)
                    .setMessage(R.string.clear_history_confirm)
                    .setPositiveButton(R.string.clear_history, (dialog, which) -> {
                        historyRepository.clearHistory();
                        refreshHistory();
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        });
    }

    private void setupListeners() {
        binding.saveButton.setOnClickListener(v -> {
            saveSettings();
            Toast.makeText(this, R.string.toast_save_success, Toast.LENGTH_SHORT).show();
        });

        binding.floatingServiceButton.setOnClickListener(v -> toggleFloatingService());
        binding.overlayPermissionButton.setOnClickListener(
                v -> PermissionHelper.openOverlaySettings(this)
        );
        binding.accessibilityPermissionButton.setOnClickListener(
                v -> PermissionHelper.openAccessibilitySettings(this)
        );
        binding.batteryOptimizationButton.setOnClickListener(v -> {
            Toast.makeText(this, R.string.toast_battery_guide, Toast.LENGTH_SHORT).show();
            PermissionHelper.requestIgnoreBatteryOptimizations(this);
        });

        binding.handleSizeSlider.addOnChangeListener((slider, value, fromUser) -> updateSliderLabels());
        binding.handleAlphaSlider.addOnChangeListener((slider, value, fromUser) -> updateSliderLabels());
    }

    private void loadStoredSettings() {
        binding.apiKeyInput.setText(settingsManager.getApiKey());
        binding.secretKeyInput.setText(settingsManager.getSecretKey());
        binding.autoDetectSwitch.setChecked(settingsManager.isAutoDetectLanguage());
        binding.handleSizeSlider.setValue(settingsManager.getHandleSizeDp());
        binding.handleAlphaSlider.setValue(settingsManager.getHandleAlphaPercent());
    }

    private void refreshHistory() {
        List<String> history = historyRepository.getHistory();
        historyAdapter.submitList(history);
        binding.emptyHistoryText.setVisibility(history.isEmpty() ? android.view.View.VISIBLE : android.view.View.GONE);
        binding.historyRecyclerView.setVisibility(history.isEmpty() ? android.view.View.GONE : android.view.View.VISIBLE);
    }

    private void saveSettings() {
        settingsManager.setApiKey(String.valueOf(binding.apiKeyInput.getText()));
        settingsManager.setSecretKey(String.valueOf(binding.secretKeyInput.getText()));
        settingsManager.setAutoDetectLanguage(binding.autoDetectSwitch.isChecked());
        settingsManager.setHandleSizeDp(Math.round(binding.handleSizeSlider.getValue()));
        settingsManager.setHandleAlphaPercent(Math.round(binding.handleAlphaSlider.getValue()));
        OverlayControlService.notifySettingsChanged(this);
        updateStatusUi();
    }

    private void updateSliderLabels() {
        binding.sizeValueText.setText(String.format(
                Locale.getDefault(),
                getString(R.string.slider_size_format),
                Math.round(binding.handleSizeSlider.getValue())
        ));
        binding.alphaValueText.setText(String.format(
                Locale.getDefault(),
                getString(R.string.slider_alpha_format),
                Math.round(binding.handleAlphaSlider.getValue())
        ));
    }

    private void updateStatusUi() {
        boolean overlayGranted = PermissionHelper.canDrawOverlays(this);
        boolean accessibilityGranted = PermissionHelper.isAccessibilityEnabled(this, OcrAccessibilityService.class);
        boolean serviceEnabled = settingsManager.isServiceEnabled();

        binding.statusText.setText(serviceEnabled
                ? getString(R.string.service_running)
                : getString(R.string.service_stopped));
        binding.permissionSummaryText.setText(
                getString(R.string.permission_summary_format,
                        overlayGranted ? getString(R.string.permission_granted) : getString(R.string.permission_missing),
                        accessibilityGranted ? getString(R.string.permission_granted) : getString(R.string.permission_missing))
        );
        binding.floatingServiceButton.setText(serviceEnabled
                ? R.string.stop_overlay_button
                : R.string.start_overlay_button);
    }

    private void toggleFloatingService() {
        if (settingsManager.isServiceEnabled()) {
            OverlayControlService.stopService(this);
            settingsManager.setServiceEnabled(false);
            updateStatusUi();
            Toast.makeText(this, R.string.toast_service_stopped, Toast.LENGTH_SHORT).show();
            return;
        }

        saveSettings();
        if (!PermissionHelper.canDrawOverlays(this)) {
            Toast.makeText(this, R.string.toast_need_overlay_permission, Toast.LENGTH_SHORT).show();
            PermissionHelper.openOverlaySettings(this);
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    REQUEST_NOTIFICATION_PERMISSION
            );
            return;
        }
        startFloatingService();
    }

    private void startFloatingService() {
        OverlayControlService.startService(this);
        settingsManager.setServiceEnabled(true);
        updateStatusUi();
        Toast.makeText(this, R.string.toast_service_started, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_NOTIFICATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startFloatingService();
            } else {
                Toast.makeText(this, R.string.toast_open_settings, Toast.LENGTH_SHORT).show();
            }
        }
    }
}
