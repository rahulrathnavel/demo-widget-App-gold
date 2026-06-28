package com.aura1010.app;

import android.text.format.DateFormat;

import java.util.Calendar;
import java.util.Locale;

final class Formatters {
    static final String RUPEE = "\u20B9";

    private Formatters() {
    }

    static String money(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value) || value <= 0) {
            return RUPEE + "0.00";
        }
        if (value >= 100000) {
            return RUPEE + String.format(Locale.US, "%,.0f", value);
        }
        return RUPEE + String.format(Locale.US, "%,.2f", value);
    }

    static String percent(double value) {
        return String.format(Locale.US, "%+.2f%%", value);
    }

    static String dateTime(long timestamp) {
        if (timestamp <= 0) {
            return "Not updated yet";
        }
        return DateFormat.format("dd MMM, hh:mm a", timestamp).toString();
    }

    static String shortTime(long timestamp) {
        if (timestamp <= 0) {
            return "No data";
        }
        return DateFormat.format("hh:mm a", timestamp).toString();
    }

    static boolean sameMonth(long timestamp, long now) {
        Calendar a = Calendar.getInstance();
        Calendar b = Calendar.getInstance();
        a.setTimeInMillis(timestamp);
        b.setTimeInMillis(now);
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR)
                && a.get(Calendar.MONTH) == b.get(Calendar.MONTH);
    }

    static boolean sameYear(long timestamp, long now) {
        Calendar a = Calendar.getInstance();
        Calendar b = Calendar.getInstance();
        a.setTimeInMillis(timestamp);
        b.setTimeInMillis(now);
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR);
    }
}
