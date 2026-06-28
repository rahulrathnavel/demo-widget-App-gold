package com.aura1010.app;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

final class AppPrefs {
    private static final String PREFS = "aura_1010_prefs";
    private static final String HISTORY_JSON = "history_json";
    private static final int MAX_HISTORY_POINTS = 1200;

    private AppPrefs() {
    }

    static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    static void ensureDefaults(Context context) {
        SharedPreferences prefs = prefs(context);
        SharedPreferences.Editor edit = prefs.edit();
        putDefault(prefs, edit, "gold_url", "https://api.gold-api.com/price/XAU/INR");
        putDefault(prefs, edit, "silver_url", "https://api.gold-api.com/price/XAG/INR");
        putDefault(prefs, edit, "price_api_key", "");
        putDefault(prefs, edit, "buy_link", "https://auragold.in/");
        putDefault(prefs, edit, "nvidia_url", "https://integrate.api.nvidia.com/v1/chat/completions");
        putDefault(prefs, edit, "nvidia_model", "sarvam-m");
        putDefault(prefs, edit, "nvidia_key", "");
        putDefault(prefs, edit, "widget_metal", "gold");
        putDefault(prefs, edit, "alert_threshold_percent", "1.0");
        putDefault(prefs, edit, "update_hours", "1");
        edit.apply();
    }

    private static void putDefault(SharedPreferences prefs, SharedPreferences.Editor edit, String key, String value) {
        if (!prefs.contains(key)) {
            edit.putString(key, value);
        }
    }

    static String getString(Context context, String key, String fallback) {
        return prefs(context).getString(key, fallback);
    }

    static void putString(Context context, String key, String value) {
        prefs(context).edit().putString(key, value == null ? "" : value.trim()).apply();
    }

    static String goldUrl(Context context) {
        return getString(context, "gold_url", "https://api.gold-api.com/price/XAU/INR");
    }

    static String silverUrl(Context context) {
        return getString(context, "silver_url", "https://api.gold-api.com/price/XAG/INR");
    }

    static String priceApiKey(Context context) {
        return getString(context, "price_api_key", "");
    }

    static String buyLink(Context context) {
        return getString(context, "buy_link", "https://auragold.in/");
    }

    static String nvidiaKey(Context context) {
        return getString(context, "nvidia_key", "");
    }

    static String nvidiaModel(Context context) {
        return getString(context, "nvidia_model", "sarvam-m");
    }

    static String nvidiaUrl(Context context) {
        return getString(context, "nvidia_url", "https://integrate.api.nvidia.com/v1/chat/completions");
    }

    static double alertThreshold(Context context) {
        try {
            return Double.parseDouble(getString(context, "alert_threshold_percent", "1.0"));
        } catch (NumberFormatException ignored) {
            return 1.0;
        }
    }

    static long updateIntervalMillis(Context context) {
        try {
            int hours = Math.max(1, Integer.parseInt(getString(context, "update_hours", "1")));
            return hours * 60L * 60L * 1000L;
        } catch (NumberFormatException ignored) {
            return 60L * 60L * 1000L;
        }
    }

    static String widgetMetal(Context context) {
        return getString(context, "widget_metal", "gold");
    }

    static void toggleWidgetMetal(Context context) {
        String next = "gold".equals(widgetMetal(context)) ? "silver" : "gold";
        putString(context, "widget_metal", next);
    }

    static List<PricePoint> history(Context context) {
        List<PricePoint> points = new ArrayList<>();
        String raw = prefs(context).getString(HISTORY_JSON, "[]");
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.optJSONObject(i);
                if (object != null) {
                    points.add(PricePoint.fromJson(object));
                }
            }
        } catch (JSONException ignored) {
        }
        return points;
    }

    static PricePoint latest(Context context) {
        List<PricePoint> history = history(context);
        if (history.isEmpty()) {
            return null;
        }
        return history.get(history.size() - 1);
    }

    static PricePoint previous(Context context) {
        List<PricePoint> history = history(context);
        if (history.size() < 2) {
            return null;
        }
        return history.get(history.size() - 2);
    }

    static void saveLatest(Context context, PricePoint point) {
        List<PricePoint> history = history(context);
        if (!history.isEmpty()) {
            PricePoint last = history.get(history.size() - 1);
            boolean veryClose = Math.abs(last.timestamp - point.timestamp) < 60_000
                    && Math.abs(last.goldOunceInr - point.goldOunceInr) < 0.01
                    && Math.abs(last.silverOunceInr - point.silverOunceInr) < 0.01;
            if (veryClose) {
                history.remove(history.size() - 1);
            }
        }
        history.add(point);
        while (history.size() > MAX_HISTORY_POINTS) {
            history.remove(0);
        }
        JSONArray array = new JSONArray();
        for (PricePoint item : history) {
            try {
                array.put(item.toJson());
            } catch (JSONException ignored) {
            }
        }
        prefs(context).edit().putString(HISTORY_JSON, array.toString()).apply();
    }
}
