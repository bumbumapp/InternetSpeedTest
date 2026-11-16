package com.code.bumbumapps.netspeed.tools;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

/** Rounded progress bar coloured by signal quality (red / amber / green). */
public class SignalBarView extends View {
    private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF();
    private int percent;

    public SignalBarView(Context c) { this(c, null); }

    public SignalBarView(Context c, AttributeSet a) {
        super(c, a);
        track.setColor(0xFF272B38);
    }

    public void setPercent(int p) {
        percent = Math.max(0, Math.min(100, p));
        invalidate();
    }

    @Override
    protected void onDraw(Canvas c) {
        float h = getHeight(), w = getWidth(), rad = h / 2f;
        r.set(0, 0, w, h);
        c.drawRoundRect(r, rad, rad, track);
        if (percent <= 0) return;
        fill.setColor(percent < 33 ? 0xFFF87171 : percent < 60 ? 0xFFFACC15 : 0xFF34D399);
        float fw = Math.max(h, w * percent / 100f);
        r.set(0, 0, fw, h);
        c.drawRoundRect(r, rad, rad, fill);
    }
}
