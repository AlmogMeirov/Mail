package com.example.gmailapplication.shared;

import android.content.Context;
import android.content.SharedPreferences;

public class TokenManager {
    private static final String PREFS_NAME = "auth_prefs";
    private static final String KEY_TOKEN = "auth_token";

    public static void save(Context context, String token) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_TOKEN, token).apply();
    }

    public static String load(Context context) {  // Changed from get() to load()
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_TOKEN, null);
    }

    public static String getCurrentUserEmail(Context context) {
        String token = load(context);
        if (token == null) return null;

        try {

            String[] parts = token.split("\\.");
            if (parts.length != 3) return null;
            String payload = new String(android.util.Base64.decode(parts[1], android.util.Base64.URL_SAFE));
            int start = payload.indexOf("\"email\":\"") + 9;
            int end = payload.indexOf("\"", start);

            return payload.substring(start, end);
        } catch (Exception e) {
            return null;
        }
    }

    public static String get(Context context) {  // Keep both for compatibility
        return load(context);
    }

    public static void clear(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().clear().apply();
    }

    public static boolean hasToken(Context context) {
        return load(context) != null;
    }
}