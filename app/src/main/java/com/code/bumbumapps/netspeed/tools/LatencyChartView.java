package com.code.bumbumapps.netspeed.tools;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/** Minimal line chart of the latest latency samples; failed probes are drawn as red ticks. */
public class LatencyChartView extends View {
    private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint grid = new Paint();
    private final Paint lost = new Paint();
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private List<Double> data = new ArrayList<>();
    private int capacity = 60;

    public LatencyChartView(Context c) { this(c, null); }

    public LatencyChartView(Context c, AttributeSet a) {
        super(c, a);
        float d = getResources().getDisplayMetrics().density;
        line.setColor(0xFF38BDF8);
        line.setStyle(Paint.Style.STROKE);
        line.setStrokeWidth(2.5f * d);
        line.setStrokeJoin(Paint.Join.ROUND);
        grid.setColor(0xFF272B38);
        grid.setStrokeWidth(1f * d);
        lost.setColor(0xFFFF8A80);
        lost.setStrokeWidth(2f * d);
        text.setColor(0xFF9AA1B2);
        text.setTextSize(11f * d);
    }

    public void setData(List<Double> samples, int capacity) {
        this.data = samples;
        this.capacity = Math.max(2, capacity);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float d = getResources().getDisplayMetrics().density;
        float padL = 40 * d, padR = 12 * d, padT = 12 * d, padB = 12 * d;
        float w = getWidth() - padL - padR, h = getHeight() - padT - padB;
        double max = 50;
        for (double v : data) if (v > max) max = v;
        max = Math.ceil(max / 50) * 50;
        for (int i = 0; i <= 2; i++) {
            float y = padT + h * i / 2f;
            canvas.drawLine(padL, y, padL + w, y, grid);
            canvas.drawText(String.valueOf((int) (max - max * i / 2)), 4 * d, y + 4 * d, text);
        }
        path.reset();
        boolean open = false;
        float step = w / (capacity - 1);
        for (int i = 0; i < data.size(); i++) {
            float x = padL + step * (capacity - data.size() + i);
            double v = data.get(i);
            if (v < 0) {
                canvas.drawLine(x, padT, x, padT + h, lost);
                open = false;
                continue;
            }
            float y = (float) (padT + h - h * v / max);
            if (!open) { path.moveTo(x, y); open = true; } else path.lineTo(x, y);
        }
        canvas.drawPath(path, line);
    }
}
