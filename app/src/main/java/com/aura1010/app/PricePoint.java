package com.aura1010.app;

import org.json.JSONException;
import org.json.JSONObject;

final class PricePoint {
    static final double TROY_OUNCE_GRAMS = 31.1034768;

    final long timestamp;
    final double goldOunceInr;
    final double silverOunceInr;
    final String source;

    PricePoint(long timestamp, double goldOunceInr, double silverOunceInr, String source) {
        this.timestamp = timestamp;
        this.goldOunceInr = goldOunceInr;
        this.silverOunceInr = silverOunceInr;
        this.source = source;
    }

    double gold24Gram() {
        return goldOunceInr / TROY_OUNCE_GRAMS;
    }

    double gold22Gram() {
        return gold24Gram() * 22.0 / 24.0;
    }

    double gold18Gram() {
        return gold24Gram() * 18.0 / 24.0;
    }

    double silverGram() {
        return silverOunceInr / TROY_OUNCE_GRAMS;
    }

    double goldPavan22() {
        return gold22Gram() * 8.0;
    }

    double silverEightGram() {
        return silverGram() * 8.0;
    }

    JSONObject toJson() throws JSONException {
        JSONObject object = new JSONObject();
        object.put("timestamp", timestamp);
        object.put("goldOunceInr", goldOunceInr);
        object.put("silverOunceInr", silverOunceInr);
        object.put("source", source);
        return object;
    }

    static PricePoint fromJson(JSONObject object) {
        return new PricePoint(
                object.optLong("timestamp"),
                object.optDouble("goldOunceInr"),
                object.optDouble("silverOunceInr"),
                object.optString("source", "gold-api.com")
        );
    }
}
