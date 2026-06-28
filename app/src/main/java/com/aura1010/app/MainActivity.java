package com.aura1010.app;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public final class MainActivity extends Activity {
    private TextView statusText;
    private TextView heroGoldText;
    private TextView heroSilverText;
    private TextView goldDetailsText;
    private TextView silverDetailsText;
    private TextView insightText;
    private TextView monthStatsText;
    private TextView yearStatsText;
    private TextView chatOutputText;
    private SparklineView sparklineView;

    private EditText chatInput;
    private EditText goldUrlInput;
    private EditText silverUrlInput;
    private EditText priceKeyInput;
    private EditText buyLinkInput;
    private EditText nvidiaUrlInput;
    private EditText nvidiaModelInput;
    private EditText nvidiaKeyInput;
    private EditText updateHoursInput;
    private EditText alertPercentInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppPrefs.ensureDefaults(this);
        Notifier.ensureChannel(this);
        requestNotificationPermission();
        buildUi();
        bindSettings();
        render();
        PriceUpdateReceiver.schedule(this);
        if (AppPrefs.latest(this) == null) {
            refreshPrices(false);
        }
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 10);
        }
    }

    private void buildUi() {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(false);
        scrollView.setBackgroundResource(R.drawable.app_bg);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(20), dp(18), dp(28));
        scrollView.addView(root, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT
        ));

        root.addView(hero());
        root.addView(actionRow());
        root.addView(priceSection());
        root.addView(chartSection());
        root.addView(insightSection());
        root.addView(chatSection());
        root.addView(settingsSection());
        setContentView(scrollView);
    }

    private View hero() {
        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.HORIZONTAL);
        hero.setGravity(Gravity.CENTER_VERTICAL);
        hero.setPadding(0, 0, 0, dp(14));

        ImageView coin = new ImageView(this);
        coin.setImageResource(R.drawable.aura_coin);
        LinearLayout.LayoutParams coinParams = new LinearLayout.LayoutParams(dp(64), dp(64));
        coinParams.setMargins(0, 0, dp(14), 0);
        hero.addView(coin, coinParams);

        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        hero.addView(copy, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        TextView title = text("Aura 10/10", 28, Color.WHITE, true);
        copy.addView(title);
        TextView subtitle = text("Indicative Chennai rate\nசென்னை தங்கம் மற்றும் வெள்ளி விலை", 13, Color.rgb(226, 220, 255), false);
        subtitle.setLineSpacing(2, 1.05f);
        copy.addView(subtitle);

        statusText = text("Ready", 12, Color.rgb(244, 211, 126), true);
        statusText.setGravity(Gravity.RIGHT);
        hero.addView(statusText, new LinearLayout.LayoutParams(dp(92), LinearLayout.LayoutParams.WRAP_CONTENT));
        return hero;
    }

    private View actionRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 0, 0, dp(12));
        Button refresh = button("Refresh");
        refresh.setOnClickListener(v -> refreshPrices(true));
        row.addView(refresh, new LinearLayout.LayoutParams(0, dp(46), 1));
        Button buy = goldButton("Buy Now");
        buy.setOnClickListener(v -> openUrl(AppPrefs.buyLink(this)));
        LinearLayout.LayoutParams buyParams = new LinearLayout.LayoutParams(0, dp(46), 1);
        buyParams.setMargins(dp(10), 0, 0, 0);
        row.addView(buy, buyParams);
        return row;
    }

    private View priceSection() {
        LinearLayout box = card();
        TextView label = text("Live price cards", 18, Color.WHITE, true);
        box.addView(label);
        TextView hint = text("Per gram is shown first for quick reading. Ounce and pavan are below.", 12, Color.rgb(196, 191, 215), false);
        box.addView(hint);

        heroGoldText = text("Gold 24K: " + Formatters.money(0) + " / g", 25, Color.rgb(243, 199, 100), true);
        heroGoldText.setPadding(0, dp(14), 0, dp(4));
        box.addView(heroGoldText);
        goldDetailsText = text("", 14, Color.WHITE, false);
        goldDetailsText.setLineSpacing(5, 1.05f);
        box.addView(goldDetailsText);

        heroSilverText = text("Silver: " + Formatters.money(0) + " / g", 22, Color.rgb(218, 228, 238), true);
        heroSilverText.setPadding(0, dp(16), 0, dp(4));
        box.addView(heroSilverText);
        silverDetailsText = text("", 14, Color.WHITE, false);
        silverDetailsText.setLineSpacing(5, 1.05f);
        box.addView(silverDetailsText);
        return box;
    }

    private View chartSection() {
        LinearLayout box = card();
        box.addView(text("Stored trend", 18, Color.WHITE, true));
        box.addView(text("Gold line is yellow. Silver line is light.", 12, Color.rgb(196, 191, 215), false));
        sparklineView = new SparklineView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(160)
        );
        params.setMargins(0, dp(10), 0, 0);
        box.addView(sparklineView, params);
        return box;
    }

    private View insightSection() {
        LinearLayout box = card();
        box.addView(text("Insights", 18, Color.WHITE, true));
        insightText = text("", 14, Color.WHITE, false);
        insightText.setLineSpacing(5, 1.05f);
        insightText.setPadding(0, dp(10), 0, dp(12));
        box.addView(insightText);
        monthStatsText = text("", 13, Color.rgb(226, 220, 255), false);
        monthStatsText.setLineSpacing(5, 1.05f);
        box.addView(monthStatsText);
        yearStatsText = text("", 13, Color.rgb(226, 220, 255), false);
        yearStatsText.setLineSpacing(5, 1.05f);
        yearStatsText.setPadding(0, dp(8), 0, 0);
        box.addView(yearStatsText);
        return box;
    }

    private View chatSection() {
        LinearLayout box = card();
        box.addView(text("AI chat", 18, Color.WHITE, true));
        box.addView(text("Ask in English or Tamil. Answers use your stored price history.", 12, Color.rgb(196, 191, 215), false));
        chatInput = input("Example: Should I buy gold today?");
        chatInput.setMinLines(2);
        chatInput.setGravity(Gravity.TOP);
        box.addView(chatInput);
        Button ask = goldButton("Ask Aura");
        ask.setOnClickListener(v -> askAura());
        LinearLayout.LayoutParams askParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(46));
        askParams.setMargins(0, dp(8), 0, dp(8));
        box.addView(ask, askParams);
        chatOutputText = text("AI is ready. Without NVIDIA access, local insights answer safely.", 14, Color.WHITE, false);
        chatOutputText.setLineSpacing(5, 1.05f);
        box.addView(chatOutputText);
        return box;
    }

    private View settingsSection() {
        LinearLayout box = lightCard();
        box.addView(text("Settings", 18, Color.rgb(34, 24, 64), true));
        box.addView(text("Change APIs, model, update timing, and Buy Now link.", 12, Color.rgb(86, 77, 110), false));

        goldUrlInput = labeledInput(box, "Gold API URL", "https://api.gold-api.com/price/XAU/INR");
        silverUrlInput = labeledInput(box, "Silver API URL", "https://api.gold-api.com/price/XAG/INR");
        priceKeyInput = labeledInput(box, "Price API key optional", "Leave blank for free gold-api.com");
        buyLinkInput = labeledInput(box, "Buy Now link", "https://auragold.in/");
        nvidiaUrlInput = labeledInput(box, "NVIDIA endpoint", "https://integrate.api.nvidia.com/v1/chat/completions");
        nvidiaModelInput = labeledInput(box, "NVIDIA model", "sarvam-m");
        nvidiaKeyInput = labeledInput(box, "Custom NVIDIA key optional", "Blank uses bundled obfuscated key");
        nvidiaKeyInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        updateHoursInput = labeledInput(box, "Update every hours", "1");
        updateHoursInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        alertPercentInput = labeledInput(box, "Notify when gold moves percent", "1.0");
        alertPercentInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);

        Button save = button("Save Settings");
        save.setOnClickListener(v -> saveSettings());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(46));
        params.setMargins(0, dp(10), 0, 0);
        box.addView(save, params);
        return box;
    }

    private void bindSettings() {
        goldUrlInput.setText(AppPrefs.goldUrl(this));
        silverUrlInput.setText(AppPrefs.silverUrl(this));
        priceKeyInput.setText(AppPrefs.priceApiKey(this));
        buyLinkInput.setText(AppPrefs.buyLink(this));
        nvidiaUrlInput.setText(AppPrefs.nvidiaUrl(this));
        nvidiaModelInput.setText(AppPrefs.nvidiaModel(this));
        nvidiaKeyInput.setText("");
        updateHoursInput.setText(AppPrefs.getString(this, "update_hours", "1"));
        alertPercentInput.setText(AppPrefs.getString(this, "alert_threshold_percent", "1.0"));
    }

    private void render() {
        List<PricePoint> points = AppPrefs.history(this);
        PricePoint latest = points.isEmpty() ? null : points.get(points.size() - 1);
        if (latest == null) {
            statusText.setText("No data");
            goldDetailsText.setText("Tap Refresh to fetch gold data.");
            silverDetailsText.setText("Tap Refresh to fetch silver data.");
            insightText.setText(InsightEngine.quickInsight(points));
            monthStatsText.setText("");
            yearStatsText.setText("");
            sparklineView.setPoints(points);
            return;
        }

        statusText.setText(Formatters.shortTime(latest.timestamp));
        heroGoldText.setText("Gold 24K: " + Formatters.money(latest.gold24Gram()) + " / g");
        goldDetailsText.setText(
                "22K: " + Formatters.money(latest.gold22Gram()) + " / g\n"
                        + "18K: " + Formatters.money(latest.gold18Gram()) + " / g\n"
                        + "22K pavan: " + Formatters.money(latest.goldPavan22()) + "\n"
                        + "Ounce: " + Formatters.money(latest.goldOunceInr)
        );
        heroSilverText.setText("Silver: " + Formatters.money(latest.silverGram()) + " / g");
        silverDetailsText.setText(
                "8 gram: " + Formatters.money(latest.silverEightGram()) + "\n"
                        + "Ounce: " + Formatters.money(latest.silverOunceInr) + "\n"
                        + "Updated: " + Formatters.dateTime(latest.timestamp)
        );

        InsightEngine.Summary summary = InsightEngine.summarize(points);
        insightText.setText(InsightEngine.quickInsight(points) + "\n\n" + InsightEngine.tamilQuickInsight(points));
        monthStatsText.setText(
                "This month\n"
                        + "Gold average: " + Formatters.money(summary.goldMonth.average) + "/g\n"
                        + "Gold peak: " + Formatters.money(summary.goldMonth.high) + "/g\n"
                        + "Gold drop: " + Formatters.money(summary.goldMonth.low) + "/g\n"
                        + "Silver average: " + Formatters.money(summary.silverMonth.average) + "/g"
        );
        yearStatsText.setText(
                "This year\n"
                        + "Gold average: " + Formatters.money(summary.goldYear.average) + "/g\n"
                        + "Gold peak: " + Formatters.money(summary.goldYear.high) + "/g\n"
                        + "Gold low: " + Formatters.money(summary.goldYear.low) + "/g\n"
                        + "Stored records: " + points.size()
        );
        sparklineView.setPoints(points);
    }

    private void refreshPrices(boolean manual) {
        statusText.setText("Updating");
        PriceService.refreshAsync(this, new PriceService.Callback() {
            @Override
            public void onSuccess(PricePoint point) {
                runOnUiThread(() -> {
                    PricePoint previous = AppPrefs.latest(MainActivity.this);
                    AppPrefs.saveLatest(MainActivity.this, point);
                    Notifier.maybeNotify(MainActivity.this, previous, point);
                    MetalWidgetProvider.updateAll(MainActivity.this);
                    PriceUpdateReceiver.schedule(MainActivity.this);
                    render();
                    if (manual) {
                        toast("Price updated");
                    }
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    statusText.setText("Offline");
                    render();
                    toast(message == null || message.isEmpty() ? "Unable to update prices" : message);
                });
            }
        });
    }

    private void askAura() {
        String question = chatInput.getText().toString().trim();
        if (question.isEmpty()) {
            toast("Ask a question first");
            return;
        }
        chatOutputText.setText("Thinking...");
        AiService.ask(this, question, answer -> runOnUiThread(() -> chatOutputText.setText(answer)));
    }

    private void saveSettings() {
        AppPrefs.putString(this, "gold_url", goldUrlInput.getText().toString());
        AppPrefs.putString(this, "silver_url", silverUrlInput.getText().toString());
        AppPrefs.putString(this, "price_api_key", priceKeyInput.getText().toString());
        AppPrefs.putString(this, "buy_link", buyLinkInput.getText().toString());
        AppPrefs.putString(this, "nvidia_url", nvidiaUrlInput.getText().toString());
        AppPrefs.putString(this, "nvidia_model", nvidiaModelInput.getText().toString());
        String customNvidia = nvidiaKeyInput.getText().toString().trim();
        if (!customNvidia.isEmpty()) {
            AppPrefs.putString(this, "nvidia_key", customNvidia);
            nvidiaKeyInput.setText("");
        }
        AppPrefs.putString(this, "update_hours", updateHoursInput.getText().toString());
        AppPrefs.putString(this, "alert_threshold_percent", alertPercentInput.getText().toString());
        PriceUpdateReceiver.schedule(this);
        MetalWidgetProvider.updateAll(this);
        toast("Settings saved");
    }

    private EditText labeledInput(LinearLayout parent, String label, String hint) {
        TextView labelView = text(label, 12, Color.rgb(63, 54, 84), true);
        labelView.setPadding(0, dp(12), 0, dp(4));
        parent.addView(labelView);
        EditText editText = input(hint);
        editText.setSingleLine(false);
        editText.setMinLines(1);
        editText.setTextColor(Color.rgb(20, 18, 28));
        editText.setHintTextColor(Color.rgb(124, 119, 142));
        parent.addView(editText);
        return editText;
    }

    private LinearLayout card() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackgroundResource(R.drawable.card_bg);
        box.setPadding(dp(16), dp(16), dp(16), dp(16));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, dp(12));
        box.setLayoutParams(params);
        return box;
    }

    private LinearLayout lightCard() {
        LinearLayout box = card();
        box.setBackgroundResource(R.drawable.card_soft_bg);
        return box;
    }

    private TextView text(String value, int sp, int color, boolean bold) {
        TextView textView = new TextView(this);
        textView.setText(value);
        textView.setTextSize(sp);
        textView.setTextColor(color);
        textView.setIncludeFontPadding(true);
        if (bold) {
            textView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        }
        return textView;
    }

    private Button button(String label) {
        Button button = new Button(this);
        button.setAllCaps(false);
        button.setText(label);
        button.setTextColor(Color.WHITE);
        button.setTextSize(14);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setBackgroundResource(R.drawable.purple_button_bg);
        return button;
    }

    private Button goldButton(String label) {
        Button button = button(label);
        button.setTextColor(Color.rgb(34, 22, 0));
        button.setBackgroundResource(R.drawable.gold_button_bg);
        return button;
    }

    private EditText input(String hint) {
        EditText input = new EditText(this);
        input.setHint(hint);
        input.setTextSize(14);
        input.setSingleLine(false);
        input.setPadding(dp(12), dp(9), dp(12), dp(9));
        input.setBackgroundResource(R.drawable.input_bg);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, dp(8), 0, 0);
        input.setLayoutParams(params);
        return input;
    }

    private void openUrl(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception ex) {
            toast("Unable to open link");
        }
    }

    private void toast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
