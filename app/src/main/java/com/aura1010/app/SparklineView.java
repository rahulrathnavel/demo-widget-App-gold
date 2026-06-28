package com.aura1010.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public final class SparklineView extends View {
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint goldPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint silverPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final List<PricePoint> points = new ArrayList<>();

    public SparklineView(Context context) {
        super(context);
        init();
    }

    public SparklineView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        gridPaint.setColor(Color.argb(58, 255, 255, 255));
        gridPaint.setStrokeWidth(1f);
        goldPaint.setColor(Color.rgb(243, 199, 100));
        goldPaint.setStrokeWidth(4f);
        goldPaint.setStyle(Paint.Style.STROKE);
        goldPaint.setStrokeCap(Paint.Cap.ROUND);
        silverPaint.setColor(Color.rgb(205, 218, 232));
        silverPaint.setStrokeWidth(4f);
        silverPaint.setStyle(Paint.Style.STROKE);
        silverPaint.setStrokeCap(Paint.Cap.ROUND);
    }

    void setPoints(List<PricePoint> newPoints) {
        points.clear();
        int start = Math.max(0, newPoints.size() - 40);
        points.addAll(newPoints.subList(start, newPoints.size()));
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();
        int pad = 18;
        canvas.drawLine(pad, height - pad, width - pad, height - pad, gridPaint);
        canvas.drawLine(pad, pad, pad, height - pad, gridPaint);
        if (points.size() < 2) {
            Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
            text.setColor(Color.argb(180, 255, 255, 255));
            text.setTextSize(32f);
            canvas.drawText("Refresh prices to draw trend", pad, height / 2f, text);
            return;
        }
        drawLine(canvas, true, goldPaint, pad, width, height);
        drawLine(canvas, false, silverPaint, pad, width, height);
    }

    private void drawLine(Canvas canvas, boolean gold, Paint paint, int pad, int width, int height) {
        double min = Double.MAX_VALUE;
        double max = -1;
        for (PricePoint point : points) {
            double value = gold ? point.gold24Gram() : point.silverGram();
            min = Math.min(min, value);
            max = Math.max(max, value);
        }
        if (max <= min) {
            max = min + 1;
        }
        Path path = new Path();
        for (int i = 0; i < points.size(); i++) {
            double value = gold ? points.get(i).gold24Gram() : points.get(i).silverGram();
            float x = pad + ((width - pad * 2f) * i / (points.size() - 1));
            float y = (float) (height - pad - ((height - pad * 2f) * (value - min) / (max - min)));
            if (i == 0) {
                path.moveTo(x, y);
            } else {
                path.lineTo(x, y);
            }
        }
        canvas.drawPath(path, paint);
    }
}
