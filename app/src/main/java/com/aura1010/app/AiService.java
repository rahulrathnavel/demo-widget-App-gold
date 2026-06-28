package com.aura1010.app;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class AiService {
    interface Callback {
        void onAnswer(String answer);
    }

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    private AiService() {
    }

    static void ask(Context context, String userQuestion, Callback callback) {
        Context appContext = context.getApplicationContext();
        List<PricePoint> points = AppPrefs.history(appContext);
        String fallback = InsightEngine.answerLocally(userQuestion, points);
        String key = configuredKey(appContext);
        if (key.isEmpty()) {
            callback.onAnswer(fallback);
            return;
        }

        EXECUTOR.execute(() -> {
            try {
                String answer = askRemote(appContext, key, userQuestion, points);
                callback.onAnswer(answer == null || answer.trim().isEmpty() ? fallback : answer.trim());
            } catch (Exception ignored) {
                callback.onAnswer(fallback);
            }
        });
    }

    private static String configuredKey(Context context) {
        String settingsKey = AppPrefs.nvidiaKey(context);
        if (settingsKey != null && !settingsKey.trim().isEmpty()) {
            return settingsKey.trim();
        }
        String bundled = NvidiaSecret.key();
        return bundled == null ? "" : bundled.trim();
    }

    private static String askRemote(Context context, String key, String question, List<PricePoint> points) throws Exception {
        InsightEngine.Summary summary = InsightEngine.summarize(points);
        JSONObject body = new JSONObject();
        body.put("model", AppPrefs.nvidiaModel(context));
        body.put("temperature", 0.25);
        body.put("max_tokens", 260);

        JSONArray messages = new JSONArray();
        messages.put(new JSONObject()
                .put("role", "system")
                .put("content", "You are Aura 10/10, a concise Chennai gold and silver assistant. Use only the supplied stored price facts. Mention that rates are indicative. Support English and Tamil. No emojis."));
        messages.put(new JSONObject()
                .put("role", "user")
                .put("content", "Question: " + question
                        + "\nStored facts: Gold 24K latest INR per gram " + summary.goldMonth.latest
                        + ", gold 24K month average " + summary.goldMonth.average
                        + ", gold month high " + summary.goldMonth.high
                        + ", gold month low " + summary.goldMonth.low
                        + ", silver latest INR per gram " + summary.silverMonth.latest
                        + ", silver month average " + summary.silverMonth.average
                        + ", records " + points.size()
                        + ". Give a clear answer in the user's language."));
        body.put("messages", messages);

        HttpURLConnection connection = (HttpURLConnection) new URL(AppPrefs.nvidiaUrl(context)).openConnection();
        connection.setConnectTimeout(20000);
        connection.setReadTimeout(30000);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Authorization", "Bearer " + key);
        connection.setDoOutput(true);
        try (OutputStream out = connection.getOutputStream()) {
            out.write(body.toString().getBytes(StandardCharsets.UTF_8));
        }

        int code = connection.getResponseCode();
        InputStream stream = code >= 200 && code < 300 ? connection.getInputStream() : connection.getErrorStream();
        String response = readAll(stream);
        if (code < 200 || code >= 300) {
            throw new IllegalStateException(response);
        }
        JSONObject object = new JSONObject(response);
        JSONArray choices = object.optJSONArray("choices");
        if (choices == null || choices.length() == 0) {
            return "";
        }
        JSONObject message = choices.optJSONObject(0).optJSONObject("message");
        return message == null ? "" : message.optString("content", "");
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
}
