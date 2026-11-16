package com.code.bumbumapps.netspeed.tools;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.code.bumbumapps.netspeed.R;

/** Small view builders shared by the tool screens so they match the main screen's cards. */
final class ToolsUi {
    private ToolsUi() {}

    static void header(Activity a, int titleRes) {
        ((TextView) a.findViewById(R.id.tool_title)).setText(titleRes);
        a.findViewById(R.id.tool_back).setOnClickListener(v -> a.finish());
    }

    /** Align by the app language, not by the script of the text, so Latin words sit on the correct side in RTL. */
    static void dir(TextView t) {
        t.setTextDirection(View.TEXT_DIRECTION_LOCALE);
    }

    static Typeface font(Context c, int res) {
        Typeface f = androidx.core.content.res.ResourcesCompat.getFont(c, res);
        return f == null ? Typeface.DEFAULT : f;
    }

    /** Pill-style segmented control; the selected label is filled with the accent colour. */
    static LinearLayout segment(Context c, LinearLayout parent, String[] labels, int selected,
                                java.util.function.IntConsumer onSelect) {
        LinearLayout row = new LinearLayout(c);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setBackgroundResource(R.drawable.is_card);
        row.setPadding(dp(c, 4), dp(c, 4), dp(c, 4), dp(c, 4));
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(-1, -2);
        rlp.bottomMargin = dp(c, 12);
        parent.addView(row, rlp);
        TextView[] tabs = new TextView[labels.length];
        for (int i = 0; i < labels.length; i++) {
            TextView t = new TextView(c);
            t.setText(labels[i]);
            t.setGravity(Gravity.CENTER);
            t.setTextSize(14);
            t.setTypeface(font(c, R.font.jakarta_bold));
            t.setSingleLine(false);
            t.setMinHeight(dp(c, 40));
            t.setPadding(dp(c, 4), 0, dp(c, 4), 0);
            t.setClickable(true);
            t.setFocusable(true);
            final int idx = i;
            tabs[i] = t;
            t.setOnClickListener(v -> {
                paintSegments(c, tabs, idx);
                onSelect.accept(idx);
            });
            row.addView(t, new LinearLayout.LayoutParams(0, -2, 1f));
        }
        paintSegments(c, tabs, selected);
        return row;
    }

    static void paintSegments(Context c, TextView[] tabs, int selected) {
        for (int i = 0; i < tabs.length; i++) {
            boolean on = i == selected;
            tabs[i].setBackgroundResource(on ? R.drawable.seg_on : 0);
            tabs[i].setTextColor(c.getResources().getColor(on ? R.color.newbg : R.color.subtext));
            tabs[i].setSelected(on);
        }
    }

    static TextView text(Context c, LinearLayout parent, String s, int sp, int colorRes, boolean bold) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(c.getResources().getColor(colorRes));
        t.setTypeface(font(c, bold ? R.font.jakarta_bold : R.font.jakarta_regular));
        dir(t);
        parent.addView(t, new LinearLayout.LayoutParams(-1, -2));
        return t;
    }

    static int dp(Context c, int v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    static LinearLayout card(Context c, LinearLayout parent, String title) {
        LinearLayout card = new LinearLayout(c);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.is_card);
        card.setPadding(dp(c, 16), dp(c, 14), dp(c, 16), dp(c, 10));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = dp(c, 12);
        parent.addView(card, lp);
        if (title != null) {
            TextView t = new TextView(c);
            t.setText(title);
            t.setTextColor(c.getResources().getColor(R.color.cone));
            t.setTextSize(16);
            t.setTypeface(font(c, R.font.jakarta_bold));
            t.setPadding(0, 0, 0, dp(c, 6));
            dir(t);
            card.addView(t);
        }
        return card;
    }

    static void row(Context c, LinearLayout card, String label, String value) {
        LinearLayout r = new LinearLayout(c);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setPadding(0, dp(c, 5), 0, dp(c, 5));
        TextView l = new TextView(c);
        l.setText(label);
        l.setTextColor(c.getResources().getColor(R.color.subtext));
        l.setTextSize(14);
        l.setTypeface(font(c, R.font.jakarta_regular));
        TextView v = new TextView(c);
        v.setText(value);
        v.setTextColor(c.getResources().getColor(R.color.maintextcolor));
        v.setTextSize(14);
        v.setTypeface(font(c, R.font.jakarta_semibold));
        v.setGravity(Gravity.END);
        v.setTextIsSelectable(true);
        dir(l);
        dir(v);
        r.addView(l, new LinearLayout.LayoutParams(0, -2, 0.42f));
        r.addView(v, new LinearLayout.LayoutParams(0, -2, 0.58f));
        card.addView(r);
    }

    static void message(Context c, LinearLayout parent, String text) {
        TextView t = new TextView(c);
        t.setText(text);
        t.setTextColor(c.getResources().getColor(R.color.subtext));
        t.setTextSize(15);
        t.setGravity(Gravity.CENTER);
        t.setPadding(0, dp(c, 40), 0, 0);
        t.setVisibility(View.VISIBLE);
        dir(t);
        parent.addView(t);
    }
}
