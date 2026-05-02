package com.example.androidpumushibie;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class HistoryRepository {
    private static final String PREFS_NAME = "global_ocr_history";
    private static final String KEY_HISTORY = "history_items";
    private static final int MAX_HISTORY_SIZE = 100;
    private final SharedPreferences preferences;
    private final Gson gson = new Gson();
    private final Type listType = new TypeToken<ArrayList<String>>() {}.getType();

    public HistoryRepository(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public List<String> getHistory() {
        String json = preferences.getString(KEY_HISTORY, "[]");
        List<String> list = gson.fromJson(json, listType);
        return list == null ? new ArrayList<>() : list;
    }

    public void addRecord(String text) {
        if (TextUtils.isEmpty(text)) {
            return;
        }
        List<String> history = getHistory();
        history.add(0, text.trim());
        while (history.size() > MAX_HISTORY_SIZE) {
            history.remove(history.size() - 1);
        }
        preferences.edit().putString(KEY_HISTORY, gson.toJson(history, listType)).apply();
    }

    public void deleteRecord(int position) {
        List<String> history = getHistory();
        if (position >= 0 && position < history.size()) {
            history.remove(position);
            preferences.edit().putString(KEY_HISTORY, gson.toJson(history, listType)).apply();
        }
    }

    public void clearHistory() {
        preferences.edit().putString(KEY_HISTORY, "[]").apply();
    }
}
