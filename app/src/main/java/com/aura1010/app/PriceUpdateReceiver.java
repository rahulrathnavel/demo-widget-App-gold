package com.aura1010.app;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public final class PriceUpdateReceiver extends BroadcastReceiver {
    static final String ACTION_REFRESH = "com.aura1010.app.REFRESH_PRICES";
    static final String ACTION_SCHEDULE = "com.aura1010.app.SCHEDULE_REFRESH";

    @Override
    public void onReceive(Context context, Intent intent) {
        AppPrefs.ensureDefaults(context);
        String action = intent == null ? "" : intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action) || ACTION_SCHEDULE.equals(action)) {
            schedule(context);
        }
        refreshNow(context);
    }

    static void refreshNow(Context context) {
        Context appContext = context.getApplicationContext();
        PriceService.refreshAsync(appContext, new PriceService.Callback() {
            @Override
            public void onSuccess(PricePoint point) {
                PricePoint previous = AppPrefs.latest(appContext);
                AppPrefs.saveLatest(appContext, point);
                MetalWidgetProvider.updateAll(appContext);
                Notifier.maybeNotify(appContext, previous, point);
                schedule(appContext);
            }

            @Override
            public void onError(String message) {
                MetalWidgetProvider.updateAll(appContext);
                schedule(appContext);
            }
        });
    }

    static void schedule(Context context) {
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, PriceUpdateReceiver.class).setAction(ACTION_REFRESH);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                20,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        long when = System.currentTimeMillis() + AppPrefs.updateIntervalMillis(context);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when, pendingIntent);
        } else {
            manager.set(AlarmManager.RTC_WAKEUP, when, pendingIntent);
        }
    }
}
