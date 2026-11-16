package com.code.bumbumapps.netspeed;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import com.code.bumbumapps.netspeed.CustomAdapter.MyDatabaseHelper;
import com.code.bumbumapps.netspeed.tools.DataUsageActivity;
import com.code.bumbumapps.netspeed.tools.HistoryStatsActivity;
import com.code.bumbumapps.netspeed.tools.LanScannerActivity;
import com.code.bumbumapps.netspeed.tools.LatencyMonitorActivity;
import com.code.bumbumapps.netspeed.tools.NetToolboxActivity;
import com.code.bumbumapps.netspeed.tools.NetworkDetailsActivity;
import com.code.bumbumapps.netspeed.tools.WifiAnalyzerActivity;

/** Launch screen: speed-test hero plus a grid of every network tool in the app. */
public class HomeActivity extends AppCompatActivity {
    private TextView heroText;

    private static final class Tile {
        final int icon, title, desc, color;
        final Intent intent;

        Tile(int icon, int title, int desc, int color, Intent intent) {
            this.icon = icon;
            this.title = title;
            this.desc = desc;
            this.color = color;
            this.intent = intent;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        findViewById(R.id.home_start).setOnClickListener(v ->
                startActivity(new Intent(this, MainActivity.class)));
        findViewById(R.id.home_hero).setOnClickListener(v ->
                startActivity(new Intent(this, MainActivity.class)));
        findViewById(R.id.home_history).setOnClickListener(v ->
                startActivity(new Intent(this, History.class)));
        heroText = findViewById(R.id.home_hero_text);

        Tile[] tiles = {
                new Tile(R.drawable.ic_t_wifi, R.string.tool_wifi_title, R.string.tool_wifi_desc, 0xFF38BDF8,
                        new Intent(this, WifiAnalyzerActivity.class)),
                new Tile(R.drawable.ic_t_lan, R.string.tool_lan_title, R.string.tool_lan_desc, 0xFF34D399,
                        new Intent(this, LanScannerActivity.class)),
                new Tile(R.drawable.ic_t_ping, R.string.tool_ping_title, R.string.tool_ping_desc, 0xFFA78BFA,
                        toolbox(NetToolboxActivity.MODE_PING)),
                new Tile(R.drawable.ic_t_trace, R.string.tool_trace_title, R.string.tool_trace_desc, 0xFFFB923C,
                        toolbox(NetToolboxActivity.MODE_TRACE)),
                new Tile(R.drawable.ic_t_dns, R.string.tool_dns_title, R.string.tool_dns_desc, 0xFF2DD4BF,
                        toolbox(NetToolboxActivity.MODE_DNS)),
                new Tile(R.drawable.ic_t_port, R.string.tool_port_title, R.string.tool_port_desc, 0xFFF87171,
                        toolbox(NetToolboxActivity.MODE_PORT)),
                new Tile(R.drawable.ic_t_usage, R.string.tool_usage_title, R.string.tool_usage_desc, 0xFFFACC15,
                        new Intent(this, DataUsageActivity.class)),
                new Tile(R.drawable.ic_t_info, R.string.tool_network_title, R.string.tool_network_short, 0xFF38BDF8,
                        new Intent(this, NetworkDetailsActivity.class)),
                new Tile(R.drawable.ic_t_latency, R.string.tool_latency_title, R.string.tool_latency_short, 0xFF34D399,
                        new Intent(this, LatencyMonitorActivity.class)),
                new Tile(R.drawable.ic_t_history, R.string.tool_stats_title, R.string.tool_stats_short, 0xFFA78BFA,
                        new Intent(this, HistoryStatsActivity.class)),
        };
        LinearLayout grid = findViewById(R.id.home_grid);
        for (int i = 0; i < tiles.length; i += 2) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setBaselineAligned(false);
            LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(-1, -2);
            rlp.bottomMargin = dp(this, 10);
            grid.addView(row, rlp);
            for (int j = 0; j < 2; j++) {
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -1, 1f);
                if (j == 0) lp.setMarginEnd(dp(this, 6)); else lp.setMarginStart(dp(this, 6));
                row.addView(tile(tiles[i + j]), lp);
            }
        }
    }

    private Intent toolbox(String mode) {
        return new Intent(this, NetToolboxActivity.class).putExtra(NetToolboxActivity.EXTRA_MODE, mode);
    }

    private View tile(Tile t) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackgroundResource(R.drawable.bg_tile);
        box.setPadding(dp(this, 12), dp(this, 12), dp(this, 12), dp(this, 12));
        box.setClickable(true);
        box.setFocusable(true);
        box.setContentDescription(getString(t.title));

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        box.addView(top, new LinearLayout.LayoutParams(-1, -2));

        ImageView icon = new ImageView(this);
        icon.setImageResource(t.icon);
        icon.setColorFilter(t.color);
        icon.setPadding(dp(this, 8), dp(this, 8), dp(this, 8), dp(this, 8));
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(this, 12));
        bg.setColor((t.color & 0x00FFFFFF) | 0x2E000000);
        icon.setBackground(bg);
        top.addView(icon, new LinearLayout.LayoutParams(dp(this, 36), dp(this, 36)));

        TextView title = new TextView(this);
        title.setText(t.title);
        title.setTextColor(getResources().getColor(R.color.maintextcolor));
        title.setTextSize(14);
        title.setTypeface(font(R.font.jakarta_bold));
        title.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        title.setTextDirection(View.TEXT_DIRECTION_LOCALE);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0, -2, 1f);
        tlp.setMarginStart(dp(this, 10));
        top.addView(title, tlp);

        TextView desc = new TextView(this);
        desc.setText(t.desc);
        desc.setTextColor(getResources().getColor(R.color.subtext));
        desc.setTextSize(12);
        desc.setTypeface(font(R.font.jakarta_regular));
        desc.setTextDirection(View.TEXT_DIRECTION_LOCALE);
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(-1, -2);
        dlp.topMargin = dp(this, 8);
        box.addView(desc, dlp);

        box.setOnClickListener(v -> startActivity(t.intent));
        return box;
    }

    private Typeface font(int res) {
        Typeface f = ResourcesCompat.getFont(this, res);
        return f == null ? Typeface.DEFAULT : f;
    }

    private static int dp(Context c, int v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onResume() {
        super.onResume();
        String last = null;
        MyDatabaseHelper db = new MyDatabaseHelper(this);
        Cursor c = null;
        try {
            c = db.readAllData();
            if (c != null && c.moveToLast()) {
                last = String.format("%s %s  ·  %s %s  ·  %s %s",
                        getString(R.string.home_dl), c.getString(4), getString(R.string.home_ul), c.getString(5),
                        getString(R.string.home_ping), c.getString(3));
            }
        } catch (Exception ignored) {
        } finally {
            if (c != null) c.close();
            db.close();
        }
        heroText.setText(last != null ? last : getString(R.string.home_hero_empty));
    }
}
