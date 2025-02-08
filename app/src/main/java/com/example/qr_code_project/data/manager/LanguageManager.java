package com.example.qr_code_project.data.manager;

import android.content.Context;
import android.content.SharedPreferences;

public class LanguageManager {
    private static final String PREF_NAME = "Settings";
    private static final String LANGUAGE_KEY = "Language";

    public static void saveLanguage(Context context, String languageCode) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(LANGUAGE_KEY, languageCode);
        editor.apply();
    }

    public static String getLanguage(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(LANGUAGE_KEY, "en"); // Mặc định là tiếng Anh
    }
}

