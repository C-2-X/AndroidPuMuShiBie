package com.example.androidpumushibie;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class ResultPanelView extends FrameLayout {
    public interface Listener {
        void onCopyRequested(String text);

        void onDismissRequested();
    }

    private final List<String> lines;
    private final List<CheckBox> checkBoxes = new ArrayList<>();
    private final Listener listener;
    private final View dragHandleView;
    private final Button copyAllButton;

    public ResultPanelView(@NonNull Context context,
                           @NonNull List<String> lines,
                           @NonNull Listener listener) {
        super(context);
        this.lines = lines;
        this.listener = listener;
        int padding = dp(18);

        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(OverlayUiFactory.roundedRect(Color.WHITE, dp(24)));
        card.setElevation(dp(10));
        card.setPadding(padding, padding, padding, padding);

        LayoutParams cardParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        addView(card, cardParams);

        LinearLayout header = new LinearLayout(context);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setOrientation(LinearLayout.HORIZONTAL);
        card.addView(header, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        dragHandleView = header;

        TextView titleView = new TextView(context);
        titleView.setText(R.string.results_title);
        titleView.setTextColor(0xFF17212B);
        titleView.setTextSize(18);
        titleView.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
        header.addView(titleView, titleParams);

        ImageButton closeButton = new ImageButton(context);
        closeButton.setBackgroundColor(Color.TRANSPARENT);
        closeButton.setImageDrawable(ContextCompat.getDrawable(context, android.R.drawable.ic_menu_close_clear_cancel));
        closeButton.setOnClickListener(v -> listener.onDismissRequested());
        header.addView(closeButton);

        ScrollView scrollView = new ScrollView(context);
        scrollView.setVerticalScrollBarEnabled(false);
        scrollView.setOverScrollMode(ScrollView.OVER_SCROLL_NEVER);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, 0, 1f
        );
        scrollParams.topMargin = dp(10);
        card.addView(scrollView, scrollParams);

        LinearLayout rowsContainer = new LinearLayout(context);
        rowsContainer.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(rowsContainer, new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        for (String line : lines) {
            rowsContainer.addView(createRow(context, line));
        }

        LinearLayout bottomBar = new LinearLayout(context);
        bottomBar.setGravity(Gravity.END);
        bottomBar.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams bottomParams = new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT
        );
        bottomParams.topMargin = dp(14);
        card.addView(bottomBar, bottomParams);

        Button selectAllButton = new Button(context);
        selectAllButton.setAllCaps(false);
        selectAllButton.setText(R.string.select_all);
        selectAllButton.setOnClickListener(v -> toggleSelectAll());
        bottomBar.addView(selectAllButton);

        copyAllButton = new Button(context);
        copyAllButton.setAllCaps(false);
        copyAllButton.setText(R.string.copy_all);
        copyAllButton.setOnClickListener(v -> listener.onCopyRequested(buildCopyText()));
        LinearLayout.LayoutParams copyParams = new LinearLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT
        );
        copyParams.leftMargin = dp(10);
        bottomBar.addView(copyAllButton, copyParams);
    }

    public View getDragHandleView() {
        return dragHandleView;
    }

    private View createRow(Context context, String lineText) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(8), 0, dp(8));

        CheckBox checkBox = new CheckBox(context);
        checkBoxes.add(checkBox);
        row.addView(checkBox);

        TextView textView = new TextView(context);
        textView.setText(lineText);
        textView.setTextColor(0xFF17212B);
        textView.setTextSize(15);
        textView.setLineSpacing(0, 1.2f);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
        row.addView(textView, textParams);

        Button copyButton = new Button(context);
        copyButton.setAllCaps(false);
        copyButton.setText(R.string.line_copy);
        copyButton.setBackgroundColor(Color.TRANSPARENT);
        copyButton.setOnClickListener(v -> listener.onCopyRequested(lineText));
        row.addView(copyButton);

        row.setOnClickListener(v -> {
            checkBox.setChecked(!checkBox.isChecked());
            updateCopyButtonLabel();
        });
        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> updateCopyButtonLabel());
        return row;
    }

    private void toggleSelectAll() {
        boolean selectAll = !areAllChecked();
        for (CheckBox checkBox : checkBoxes) {
            checkBox.setChecked(selectAll);
        }
        updateCopyButtonLabel();
    }

    private boolean areAllChecked() {
        if (checkBoxes.isEmpty()) {
            return false;
        }
        for (CheckBox checkBox : checkBoxes) {
            if (!checkBox.isChecked()) {
                return false;
            }
        }
        return true;
    }

    private String buildCopyText() {
        List<String> selected = new ArrayList<>();
        for (int i = 0; i < checkBoxes.size(); i++) {
            if (checkBoxes.get(i).isChecked()) {
                selected.add(lines.get(i));
            }
        }
        if (selected.isEmpty()) {
            return TextUtils.join("\n", lines);
        }
        return TextUtils.join("\n", selected);
    }

    private void updateCopyButtonLabel() {
        int selectedCount = 0;
        for (CheckBox checkBox : checkBoxes) {
            if (checkBox.isChecked()) {
                selectedCount++;
            }
        }
        if (selectedCount > 0) {
            copyAllButton.setText(getContext().getString(R.string.copy_selected_format, selectedCount));
        } else {
            copyAllButton.setText(R.string.copy_all);
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
