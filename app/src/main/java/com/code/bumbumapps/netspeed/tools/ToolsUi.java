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
            t.setTypeface(Typeface.DEFAULT_BOLD);
            t.setPadding(0, 0, 0, dp(c, 6));
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
        TextView v = new TextView(c);
        v.setText(value);
        v.setTextColor(c.getResources().getColor(R.color.maintextcolor));
        v.setTextSize(14);
        v.setGravity(Gravity.END);
        v.setTextIsSelectable(true);
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
        parent.addView(t);
    }
}
