package com.code.bumbumapps.netspeed.tools;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/** Draws every nearby network as an arc over the Wi-Fi channels of one band (2.4 or 5 GHz). */
public class ChannelGraphView extends View {
    private static final int[] PALETTE = {0xFF38BDF8, 0xFF34D399, 0xFFA78BFA, 0xFFFB923C, 0xFFF87171,
            0xFFFACC15, 0xFF2DD4BF, 0xFFF472B6};
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint grid = new Paint();
    private final Paint label = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private List<WifiLogic.Ap> aps = new ArrayList<>();
    private int band = 24;
    private String connected;

    public ChannelGraphView(Context c) { this(c, null); }

    public ChannelGraphView(Context c, AttributeSet a) {
        super(c, a);
        float d = getResources().getDisplayMetrics().density;
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(2f * d);
        fill.setStyle(Paint.Style.FILL);
        grid.setColor(0xFF272B38);
        grid.setStrokeWidth(1f * d);
        label.setColor(0xFF9AA1B2);
        label.setTextSize(11f * d);
        label.setTextAlign(Paint.Align.CENTER);
    }

    public void setData(List<WifiLogic.Ap> aps, int band, String connectedSsid) {
        this.aps = aps == null ? new ArrayList<>() : aps;
        this.band = band;
        this.connected = connectedSsid;
        invalidate();
    }

    private int[] labelChannels() {
        return band == 24 ? new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13}
                : new int[]{36, 52, 100, 116, 132, 149, 165};
    }

    private float freqOf(int ch) {
        return band == 24 ? 2407 + 5 * ch : 5000 + 5 * ch;
    }

    @Override
    protected void onDraw(Canvas c) {
        float d = getResources().getDisplayMetrics().density;
        float w = getWidth(), h = getHeight();
        float left = 8 * d, right = w - 8 * d, top = 8 * d, bottom = h - 22 * d;
        float f0 = band == 24 ? 2402 : 5160, f1 = band == 24 ? 2487 : 5840;
        float half = band == 24 ? 11 : 10;
        c.drawLine(left, bottom, right, bottom, grid);
        for (int i = 1; i <= 3; i++) {
            float y = bottom - (bottom - top) * i / 4f;
            c.drawLine(left, y, right, y, grid);
        }
        for (int ch : labelChannels()) {
            float x = left + (freqOf(ch) - f0) / (f1 - f0) * (right - left);
            c.drawText(String.valueOf(ch), x, h - 6 * d, label);
        }
        int i = 0;
        for (WifiLogic.Ap a : aps) {
            if (a.band() != band) continue;
            int col = PALETTE[(i++) % PALETTE.length];
            float cx = left + (a.freq - f0) / (f1 - f0) * (right - left);
            float hw = half / (f1 - f0) * (right - left);
            float peak = bottom - (bottom - top) * Math.max(0.08f, WifiLogic.percent(a.rssi) / 100f);
            path.reset();
            path.moveTo(cx - hw, bottom);
            path.cubicTo(cx - hw * 0.45f, bottom, cx - hw * 0.45f, peak, cx, peak);
            path.cubicTo(cx + hw * 0.45f, peak, cx + hw * 0.45f, bottom, cx + hw, bottom);
            boolean mine = connected != null && connected.equals(a.ssid);
            fill.setColor((col & 0x00FFFFFF) | (mine ? 0x66000000 : 0x2A000000));
            stroke.setColor(col);
            stroke.setStrokeWidth((mine ? 3f : 2f) * d);
            c.drawPath(path, fill);
            c.drawPath(path, stroke);
        }
    }
}
