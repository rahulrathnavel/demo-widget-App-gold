package com.aura1010.app;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.RemoteViews;

public final class MetalWidgetProvider extends AppWidgetProvider {
    static final String ACTION_TOGGLE = "com.aura1010.app.WIDGET_TOGGLE";
    static final String ACTION_REFRESH = "com.aura1010.app.WIDGET_REFRESH";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        AppPrefs.ensureDefaults(context);
        for (int widgetId : appWidgetIds) {
            appWidgetManager.updateAppWidget(widgetId, views(context));
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        AppPrefs.ensureDefaults(context);
        String action = intent == null ? "" : intent.getAction();
        if (ACTION_TOGGLE.equals(action)) {
            AppPrefs.toggleWidgetMetal(context);
            updateAll(context);
        } else if (ACTION_REFRESH.equals(action)) {
            PriceUpdateReceiver.refreshNow(context);
            updateAll(context);
        }
    }

    static void updateAll(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        ComponentName component = new ComponentName(context, MetalWidgetProvider.class);
        int[] ids = manager.getAppWidgetIds(component);
        for (int id : ids) {
            manager.updateAppWidget(id, views(context));
        }
    }

    private static RemoteViews views(Context context) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_metal_price);
        PricePoint latest = AppPrefs.latest(context);
        String metal = AppPrefs.widgetMetal(context);
        boolean gold = "gold".equals(metal);
        String title = gold ? "Gold 24K / தங்கம்" : "Silver / வெள்ளி";
        double price = latest == null ? 0 : gold ? latest.gold24Gram() : latest.silverGram();
        views.setTextViewText(R.id.widget_metal, title);
        views.setTextViewText(R.id.widget_price, Formatters.money(price) + " / g");
        views.setTextViewText(R.id.widget_updated, latest == null ? "Tap Refresh" : "Updated " + Formatters.shortTime(latest.timestamp));

        Intent openApp = new Intent(context, MainActivity.class);
        views.setOnClickPendingIntent(R.id.widget_root, PendingIntent.getActivity(
                context,
                10,
                openApp,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        ));

        Intent toggle = new Intent(context, MetalWidgetProvider.class).setAction(ACTION_TOGGLE);
        views.setOnClickPendingIntent(R.id.widget_price, PendingIntent.getBroadcast(
                context,
                11,
                toggle,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        ));

        Intent refresh = new Intent(context, MetalWidgetProvider.class).setAction(ACTION_REFRESH);
        views.setOnClickPendingIntent(R.id.widget_refresh, PendingIntent.getBroadcast(
                context,
                12,
                refresh,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        ));

        Intent buy = new Intent(Intent.ACTION_VIEW, Uri.parse(AppPrefs.buyLink(context)));
        views.setOnClickPendingIntent(R.id.widget_buy, PendingIntent.getActivity(
                context,
                13,
                buy,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        ));
        return views;
    }
}
