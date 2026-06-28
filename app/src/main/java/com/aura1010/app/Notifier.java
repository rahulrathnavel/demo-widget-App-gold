package com.aura1010.app;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

final class Notifier {
    private static final String CHANNEL_ID = "price_alerts";

    private Notifier() {
    }

    static void ensureChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Aura price alerts",
                NotificationManager.IMPORTANCE_DEFAULT
        );
        channel.setDescription("Gold and silver price movement alerts");
        manager.createNotificationChannel(channel);
    }

    static void maybeNotify(Context context, PricePoint previous, PricePoint latest) {
        if (previous == null || latest == null) {
            return;
        }
        double oldGold = previous.gold24Gram();
        double newGold = latest.gold24Gram();
        if (oldGold <= 0) {
            return;
        }
        double change = ((newGold - oldGold) / oldGold) * 100.0;
        if (Math.abs(change) < AppPrefs.alertThreshold(context)) {
            return;
        }
        show(context, "Aura 10/10 price alert", "Gold 24K is " + Formatters.money(newGold) + "/g (" + Formatters.percent(change) + ").");
    }

    private static void show(Context context, String title, String message) {
        if (Build.VERSION.SDK_INT >= 33
                && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        ensureChannel(context);
        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                91,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        android.app.Notification notification = new android.app.Notification.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build();
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(1010, notification);
    }
}
