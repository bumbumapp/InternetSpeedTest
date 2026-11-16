package com.code.bumbumapps.netspeed.tools;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

/** Stacked column chart: Wi-Fi (blue) under mobile (orange) per interval. */
public class UsageChartView extends View {
    private final Paint wifi = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mobile = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint grid = new Paint();
    private final Paint label = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF();
    private long[] w = new long[0], m = new long[0];
    private String[] labels = new String[0];

    public UsageChartView(Context c) { this(c, null); }

    public UsageChartView(Context c, AttributeSet a) {
        super(c, a);
        float d = getResources().getDisplayMetrics().density;
        wifi.setColor(0xFF38BDF8);
        mobile.setColor(0xFFA78BFA);
        grid.setColor(0xFF272B38);
        grid.setStrokeWidth(d);
        label.setColor(0xFF9AA1B2);
        label.setTextSize(10.5f * d);
        label.setTextAlign(Paint.Align.CENTER);
    }

    public void setData(long[] wifiBytes, long[] mobileBytes, String[] labels) {
        this.w = wifiBytes;
        this.m = mobileBytes;
        this.labels = labels;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas c) {
        float d = getResources().getDisplayMetrics().density;
        int n = w.length;
        float left = 4 * d, right = getWidth() - 4 * d, top = 6 * d, bottom = getHeight() - 20 * d;
        c.drawLine(left, bottom, right, bottom, grid);
        c.drawLine(left, top, right, top, grid);
        if (n == 0) return;
        long max = 1;
        for (int i = 0; i < n; i++) max = Math.max(max, w[i] + m[i]);
        float slot = (right - left) / n;
        float bw = Math.max(2 * d, slot * 0.62f);
        for (int i = 0; i < n; i++) {
            float x = left + slot * i + (slot - bw) / 2f;
            float hw = (bottom - top) * w[i] / max;
            float hm = (bottom - top) * m[i] / max;
            float rad = Math.min(bw / 2f, 4 * d);
            if (hw > 0) {
                r.set(x, bottom - hw, x + bw, bottom);
                c.drawRoundRect(r, rad, rad, wifi);
            }
            if (hm > 0) {
                r.set(x, bottom - hw - hm, x + bw, bottom - hw);
                c.drawRoundRect(r, rad, rad, mobile);
            }
            if (i < labels.length && labels[i] != null && !labels[i].isEmpty()) {
                c.drawText(labels[i], x + bw / 2f, getHeight() - 5 * d, label);
            }
        }
    }
}
