package com.aura1010.app;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class InsightEngine {
    private InsightEngine() {
    }

    static Summary summarize(List<PricePoint> points) {
        long now = System.currentTimeMillis();
        List<PricePoint> month = new ArrayList<>();
        List<PricePoint> year = new ArrayList<>();
        for (PricePoint point : points) {
            if (Formatters.sameMonth(point.timestamp, now)) {
                month.add(point);
            }
            if (Formatters.sameYear(point.timestamp, now)) {
                year.add(point);
            }
        }
        if (month.isEmpty()) {
            month.addAll(points);
        }
        if (year.isEmpty()) {
            year.addAll(points);
        }
        return new Summary(stats(month, true), stats(year, true), stats(month, false), stats(year, false));
    }

    private static Stats stats(List<PricePoint> points, boolean gold) {
        Stats stats = new Stats();
        if (points.isEmpty()) {
            return stats;
        }
        double sum = 0;
        stats.low = Double.MAX_VALUE;
        stats.high = -1;
        for (PricePoint point : points) {
            double value = gold ? point.gold24Gram() : point.silverGram();
            sum += value;
            if (value > stats.high) {
                stats.high = value;
                stats.highAt = point.timestamp;
            }
            if (value < stats.low) {
                stats.low = value;
                stats.lowAt = point.timestamp;
            }
        }
        stats.count = points.size();
        stats.average = sum / points.size();
        stats.latest = gold ? points.get(points.size() - 1).gold24Gram() : points.get(points.size() - 1).silverGram();
        stats.changeFromAveragePercent = stats.average <= 0 ? 0 : ((stats.latest - stats.average) / stats.average) * 100.0;
        return stats;
    }

    static String quickInsight(List<PricePoint> points) {
        if (points.isEmpty()) {
            return "Update prices once to build Chennai gold and silver insights.";
        }
        Summary summary = summarize(points);
        String trend = summary.goldMonth.changeFromAveragePercent >= 0
                ? "Gold is above this month's stored average."
                : "Gold is below this month's stored average.";
        return trend + " Gold 24K is " + Formatters.money(summary.goldMonth.latest)
                + "/g, monthly average is " + Formatters.money(summary.goldMonth.average)
                + "/g, and silver is " + Formatters.money(summary.silverMonth.latest) + "/g.";
    }

    static String tamilQuickInsight(List<PricePoint> points) {
        if (points.isEmpty()) {
            return "விலை புதுப்பித்த பிறகு சென்னை தங்கம் மற்றும் வெள்ளி தகவல்கள் கிடைக்கும்.";
        }
        Summary summary = summarize(points);
        String trend = summary.goldMonth.changeFromAveragePercent >= 0
                ? "இந்த மாத சராசரியை விட தங்கம் மேலே உள்ளது."
                : "இந்த மாத சராசரியை விட தங்கம் குறைவாக உள்ளது.";
        return trend + " 24K தங்கம் " + Formatters.money(summary.goldMonth.latest)
                + "/g, மாத சராசரி " + Formatters.money(summary.goldMonth.average)
                + "/g, வெள்ளி " + Formatters.money(summary.silverMonth.latest) + "/g.";
    }

    static String answerLocally(String question, List<PricePoint> points) {
        String q = question == null ? "" : question.toLowerCase(Locale.US);
        if (points.isEmpty()) {
            return "No stored price data yet. Tap Refresh, then ask again.";
        }
        Summary summary = summarize(points);
        boolean tamil = q.contains("tamil") || q.contains("தமிழ்") || q.contains("தங்க");
        if (q.contains("silver") || q.contains("வெள்ள")) {
            return tamil
                    ? "வெள்ளி இப்போது " + Formatters.money(summary.silverMonth.latest) + "/g. இந்த மாத சராசரி " + Formatters.money(summary.silverMonth.average) + "/g."
                    : "Silver is now " + Formatters.money(summary.silverMonth.latest) + "/g. This month's stored average is " + Formatters.money(summary.silverMonth.average) + "/g.";
        }
        if (q.contains("pavan") || q.contains("பவுன்") || q.contains("sov")) {
            PricePoint latest = points.get(points.size() - 1);
            return tamil
                    ? "22K ஒரு பவுன் மதிப்பு சுமார் " + Formatters.money(latest.goldPavan22()) + ". இது indicative Chennai rate."
                    : "One 22K pavan is about " + Formatters.money(latest.goldPavan22()) + ". This is an indicative Chennai rate.";
        }
        if (q.contains("high") || q.contains("peak") || q.contains("highest") || q.contains("அதிக")) {
            return tamil
                    ? "இந்த மாதத்தில் சேமிக்கப்பட்ட அதிக 24K தங்க விலை " + Formatters.money(summary.goldMonth.high) + "/g."
                    : "The highest stored 24K gold price this month is " + Formatters.money(summary.goldMonth.high) + "/g.";
        }
        if (q.contains("low") || q.contains("drop") || q.contains("lowest") || q.contains("குறை")) {
            return tamil
                    ? "இந்த மாதத்தில் சேமிக்கப்பட்ட குறைந்த 24K தங்க விலை " + Formatters.money(summary.goldMonth.low) + "/g."
                    : "The lowest stored 24K gold price this month is " + Formatters.money(summary.goldMonth.low) + "/g.";
        }
        return tamil ? tamilQuickInsight(points) : quickInsight(points);
    }

    static final class Summary {
        final Stats goldMonth;
        final Stats goldYear;
        final Stats silverMonth;
        final Stats silverYear;

        Summary(Stats goldMonth, Stats goldYear, Stats silverMonth, Stats silverYear) {
            this.goldMonth = goldMonth;
            this.goldYear = goldYear;
            this.silverMonth = silverMonth;
            this.silverYear = silverYear;
        }
    }

    static final class Stats {
        int count;
        double latest;
        double average;
        double high;
        double low;
        long highAt;
        long lowAt;
        double changeFromAveragePercent;
    }
}
