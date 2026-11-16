package com.code.bumbumapps.netspeed.tools;

import android.app.AppOpsManager;
import android.app.usage.NetworkStats;
import android.app.usage.NetworkStatsManager;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.TrafficStats;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.code.bumbumapps.netspeed.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Data usage per hour / day for Wi-Fi vs mobile data. Needs the special "usage access" permission,
 * which is requested only here; without it the tool explains why and shows since-boot totals.
 */
public class DataUsageActivity extends AppCompatActivity {
    private final Handler ui = new Handler(Looper.getMainLooper());
    private LinearLayout content, body;
    private Button action;
    private int range = UsageMath.WEEK;
    private volatile int generation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tool_list);
        ToolsUi.header(this, R.string.tool_usage_title);
        content = findViewById(R.id.tool_content);
        action = findViewById(R.id.tool_action);
        if (savedInstanceState != null) range = savedInstanceState.getInt("range", range);
        ToolsUi.segment(this, content, new String[]{getString(R.string.usage_today), getString(R.string.usage_week),
                getString(R.string.usage_month)}, range, i -> {
            range = i;
            refresh();
        });
        body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        content.addView(body, new LinearLayout.LayoutParams(-1, -2));
        action.setText(R.string.usage_grant);
        action.setOnClickListener(v -> openSettings());
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putInt("range", range);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    @Override
    protected void onDestroy() {
        generation++;
        ui.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    private boolean hasAccess() {
        try {
            AppOpsManager ops = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
            int mode = ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), getPackageName());
            return mode == AppOpsManager.MODE_ALLOWED;
        } catch (Exception e) {
            return false;
        }
    }

    private void openSettings() {
        try {
            startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS, Uri.parse("package:" + getPackageName())));
        } catch (Exception e) {
            try {
                startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
            } catch (Exception ignored) {
            }
        }
    }

    private void refresh() {
        final int gen = ++generation;
        body.removeAllViews();
        if (!hasAccess()) {
            action.setVisibility(View.VISIBLE);
            showFallback();
            return;
        }
        action.setVisibility(View.GONE);
        ToolsUi.message(this, body, getString(R.string.usage_loading));
        final int r = range;
        new Thread(() -> {
            final List<long[]> iv = UsageMath.intervals(r, System.currentTimeMillis());
            final long[] w = new long[iv.size()], m = new long[iv.size()];
            boolean mobileOk = true;
            NetworkStatsManager nsm = (NetworkStatsManager) getSystemService(Context.NETWORK_STATS_SERVICE);
            for (int i = 0; i < iv.size() && gen == generation; i++) {
                w[i] = query(nsm, ConnectivityManager.TYPE_WIFI, iv.get(i));
                long mv = query(nsm, ConnectivityManager.TYPE_MOBILE, iv.get(i));
                m[i] = Math.max(0, mv);
                if (mv < 0) mobileOk = false;
                w[i] = Math.max(0, w[i]);
            }
            final boolean fm = mobileOk;
            ui.post(() -> {
                if (gen != generation || isFinishing() || isDestroyed()) return;
                render(r, iv, w, m, fm);
            });
        }, "data-usage").start();
    }

    /** Total rx+tx bytes of one network type in the interval, or -1 when the system refuses. */
    private static long query(NetworkStatsManager nsm, int type, long[] iv) {
        try {
            NetworkStats.Bucket b = nsm.querySummaryForDevice(type, null, iv[0], iv[1]);
            return b.getRxBytes() + b.getTxBytes();
        } catch (Exception e) {
            return -1;
        }
    }

    private void render(int r, List<long[]> iv, long[] w, long[] m, boolean mobileOk) {
        body.removeAllViews();
        long tw = 0, tm = 0;
        for (int i = 0; i < w.length; i++) {
            tw += w[i];
            tm += m[i];
        }
        LinearLayout total = ToolsUi.card(this, body, getString(R.string.usage_total));
        TextView big = ToolsUi.text(this, total, UsageMath.bytes(tw + tm), 34, R.color.maintextcolor, true);
        big.setTypeface(ToolsUi.font(this, R.font.jakarta_extrabold));

        // split bar
        LinearLayout split = new LinearLayout(this);
        split.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(-1, ToolsUi.dp(this, 10));
        slp.topMargin = ToolsUi.dp(this, 10);
        slp.bottomMargin = ToolsUi.dp(this, 10);
        total.addView(split, slp);
        float sw = tw + tm == 0 ? 1f : (float) tw / (tw + tm);
        View vw = new View(this), vm = new View(this);
        vw.setBackgroundColor(0xFF38BDF8);
        vm.setBackgroundColor(0xFFA78BFA);
        if (tw + tm == 0) vw.setBackgroundColor(0xFF272B38);
        split.addView(vw, new LinearLayout.LayoutParams(0, -1, Math.max(0.0001f, sw)));
        split.addView(vm, new LinearLayout.LayoutParams(0, -1, Math.max(0.0001f, 1f - sw)));
        ToolsUi.row(this, total, "Wi-Fi", UsageMath.bytes(tw));
        ToolsUi.row(this, total, getString(R.string.network_mobile), mobileOk ? UsageMath.bytes(tm) : "—");

        LinearLayout chartCard = ToolsUi.card(this, body, getString(r == UsageMath.TODAY ? R.string.usage_by_hour
                : R.string.usage_by_day));
        UsageChartView chart = new UsageChartView(this);
        chartCard.addView(chart, new LinearLayout.LayoutParams(-1, ToolsUi.dp(this, 180)));
        String[] labels = new String[iv.size()];
        SimpleDateFormat day = new SimpleDateFormat(r == UsageMath.WEEK ? "EEE" : "d", Locale.getDefault());
        for (int i = 0; i < labels.length; i++) {
            if (r == UsageMath.TODAY) labels[i] = i % 6 == 0 ? String.valueOf(i) : "";
            else if (r == UsageMath.WEEK) labels[i] = day.format(new Date(iv.get(i)[0]));
            else labels[i] = (i % 5 == 0 || i == labels.length - 1) ? day.format(new Date(iv.get(i)[0])) : "";
        }
        chart.setData(w, m, labels);
        android.text.SpannableStringBuilder sp = new android.text.SpannableStringBuilder();
        int a0 = sp.length();
        sp.append("\u25CF ");
        sp.setSpan(new android.text.style.ForegroundColorSpan(0xFF38BDF8), a0, sp.length(), 0);
        sp.append("Wi-Fi      ");
        int b0 = sp.length();
        sp.append("\u25CF ");
        sp.setSpan(new android.text.style.ForegroundColorSpan(0xFFA78BFA), b0, sp.length(), 0);
        sp.append(getString(R.string.network_mobile));
        TextView legend = ToolsUi.text(this, chartCard, "", 13, R.color.subtext, false);
        legend.setText(sp);
        legend.setPadding(0, ToolsUi.dp(this, 6), 0, ToolsUi.dp(this, 4));
        if (!mobileOk) {
            ToolsUi.text(this, chartCard, getString(R.string.usage_mobile_na), 13, R.color.subtext, false);
        }
    }

    private void showFallback() {
        LinearLayout why = ToolsUi.card(this, body, getString(R.string.usage_need_title));
        ToolsUi.text(this, why, getString(R.string.usage_need_msg), 14, R.color.subtext, false);
        long mobile = Math.max(0, TrafficStats.getMobileRxBytes()) + Math.max(0, TrafficStats.getMobileTxBytes());
        long total = Math.max(0, TrafficStats.getTotalRxBytes()) + Math.max(0, TrafficStats.getTotalTxBytes());
        LinearLayout boot = ToolsUi.card(this, body, getString(R.string.usage_since_boot));
        ToolsUi.row(this, boot, "Wi-Fi", UsageMath.bytes(Math.max(0, total - mobile)));
        ToolsUi.row(this, boot, getString(R.string.network_mobile), UsageMath.bytes(mobile));
        ToolsUi.row(this, boot, getString(R.string.usage_total), UsageMath.bytes(total));
    }
}
