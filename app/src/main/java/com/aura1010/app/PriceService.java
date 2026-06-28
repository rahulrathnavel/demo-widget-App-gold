package com.aura1010.app;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class PriceService {
    interface Callback {
        void onSuccess(PricePoint point);

        void onError(String message);
    }

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    private PriceService() {
    }

    static void refreshAsync(Context context, Callback callback) {
        Context appContext = context.getApplicationContext();
        EXECUTOR.execute(() -> {
            try {
                AppPrefs.ensureDefaults(appContext);
                String apiKey = AppPrefs.priceApiKey(appContext);
                FetchResult gold = fetch(AppPrefs.goldUrl(appContext), apiKey);
                FetchResult silver = fetch(AppPrefs.silverUrl(appContext), apiKey);
                long timestamp = Math.max(gold.timestamp, silver.timestamp);
                if (timestamp <= 0) {
                    timestamp = System.currentTimeMillis();
                }
                PricePoint point = new PricePoint(timestamp, gold.price, silver.price, "gold-api.com");
                callback.onSuccess(point);
            } catch (Exception ex) {
                callback.onError(ex.getMessage() == null ? "Unable to update prices" : ex.getMessage());
            }
        });
    }

    private static FetchResult fetch(String urlText, String apiKey) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(urlText).openConnection();
        connection.setConnectTimeout(12000);
        connection.setReadTimeout(12000);
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("User-Agent", "Aura1010/1.0 Android");
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            String key = apiKey.trim();
            connection.setRequestProperty("x-access-token", key);
            connection.setRequestProperty("Authorization", "Bearer " + key);
        }

        int code = connection.getResponseCode();
        InputStream stream = code >= 200 && code < 300
                ? connection.getInputStream()
                : connection.getErrorStream();
        String body = readAll(stream);
        if (code < 200 || code >= 300) {
            throw new IllegalStateException("API returned " + code + ": " + body);
        }

        JSONObject object = new JSONObject(body);
        double price = findNumber(object);
        if (price <= 0) {
            throw new IllegalStateException("No usable price field found in API response.");
        }
        long timestamp = parseTimestamp(object);
        return new FetchResult(price, timestamp);
    }

    private static String readAll(InputStream stream) throws Exception {
        if (stream == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
        }
        return builder.toString();
    }

    private static double findNumber(JSONObject object) {
        String[] preferred = {"price", "rate", "value", "ask", "bid", "close"};
        for (String key : preferred) {
            if (object.has(key)) {
                double value = object.optDouble(key, -1);
                if (value > 0) {
                    return value;
                }
            }
        }

        Iterator<String> keys = object.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            Object value = object.opt(key);
            if (value instanceof JSONObject) {
                double nested = findNumber((JSONObject) value);
                if (nested > 0) {
                    return nested;
                }
            } else if (value instanceof JSONArray) {
                JSONArray array = (JSONArray) value;
                for (int i = 0; i < array.length(); i++) {
                    JSONObject nestedObject = array.optJSONObject(i);
                    if (nestedObject != null) {
                        double nested = findNumber(nestedObject);
                        if (nested > 0) {
                            return nested;
                        }
                    }
                }
            }
        }
        return -1;
    }

    private static long parseTimestamp(JSONObject object) {
        String[] keys = {"updatedAt", "timestamp", "time", "date"};
        for (String key : keys) {
            if (!object.has(key)) {
                continue;
            }
            Object value = object.opt(key);
            if (value instanceof Number) {
                long raw = ((Number) value).longValue();
                return raw < 10_000_000_000L ? raw * 1000L : raw;
            }
            if (value instanceof String) {
                String text = ((String) value).trim();
                try {
                    return Instant.parse(text).toEpochMilli();
                } catch (Exception ignored) {
                }
            }
        }
        return System.currentTimeMillis();
    }

    private static final class FetchResult {
        final double price;
        final long timestamp;

        FetchResult(double price, long timestamp) {
            this.price = price;
            this.timestamp = timestamp;
        }
    }
}
