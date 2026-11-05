package com.code.bumbumapps.netspeed.tools;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.code.bumbumapps.netspeed.R;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Locale;

/** Probes a host once per second (TCP connect time) and charts latency, jitter and loss. */
public class LatencyMonitorActivity extends AppCompatActivity {
    private static final int WINDOW = 60;
    private static final String[] HOSTS = {"1.1.1.1", "8.8.8.8", "9.9.9.9", null};

    private final LatencyStats stats = new LatencyStats(WINDOW);
    private final Handler ui = new Handler(Looper.getMainLooper());
    private volatile Thread worker;
    private volatile boolean running;
    private Spinner target;
    private EditText custom;
    private Button action;
    private TextView current, quality, details;
    private LatencyChartView chart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_latency);
        ToolsUi.header(this, R.string.tool_latency_title);
        target = findViewById(R.id.latency_target);
        custom = findViewById(R.id.latency_custom);
        action = findViewById(R.id.tool_action);
        current = findViewById(R.id.latency_current);
        quality = findViewById(R.id.latency_quality);
        details = findViewById(R.id.latency_stats);
        chart = findViewById(R.id.latency_chart);

        ArrayAdapter<String> ad = new ArrayAdapter<>(this, R.layout.spinner_item,
                new String[]{"Cloudflare (1.1.1.1)", "Google (8.8.8.8)", "Quad9 (9.9.9.9)",
                        getString(R.string.latency_custom)});
        ad.setDropDownViewResource(R.layout.spinner_item);
        target.setAdapter(ad);
        target.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                custom.setVisibility(HOSTS[pos] == null ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onNothingSelected(AdapterView<?> p) {}
        });
        action.setOnClickListener(v -> {
            if (running) stop(); else start();
        });
        refresh();
    }

    private void start() {
        String host = HOSTS[target.getSelectedItemPosition()];
        if (host == null) host = custom.getText().toString().trim();
        if (host.isEmpty() || host.length() > 253 || host.contains(" ") || host.contains("/")) {
            Toast.makeText(this, R.string.latency_bad_host, Toast.LENGTH_SHORT).show();
            return;
        }
        stats.reset();
        running = true;
        action.setText(R.string.latency_stop);
        target.setEnabled(false);
        custom.setEnabled(false);
        final String h = host;
        Thread t = new Thread(() -> probeLoop(h), "latency-monitor");
        worker = t;
        t.start();
        refresh();
    }

    private void stop() {
        running = false;
        Thread t = worker;
        if (t != null) t.interrupt();
        action.setText(R.string.latency_start);
        target.setEnabled(true);
        custom.setEnabled(true);
    }

    private void probeLoop(String host) {
        InetAddress addr = null;
        while (running && !Thread.currentThread().isInterrupted()) {
            double rtt = -1;
            if (addr == null) {
                try { addr = InetAddress.getByName(host); } catch (Exception ignored) { }
            }
            if (addr != null) rtt = connect(addr);
            stats.add(rtt);
            ui.post(this::refresh);
            try { Thread.sleep(1000); } catch (InterruptedException e) { return; }
        }
    }

    private static double connect(InetAddress a) {
        for (int port : new int[]{443, 80}) {
            long t0 = System.nanoTime();
            try (Socket s = new Socket()) {
                s.connect(new InetSocketAddress(a, port), 2000);
                return (System.nanoTime() - t0) / 1e6;
            } catch (Exception ignored) {
            }
        }
        return -1;
    }

    private static String fmt(double v) {
        return v < 0 ? "—" : String.format(Locale.US, "%.1f ms", v);
    }

    private void refresh() {
        if (isFinishing() || isDestroyed()) return;
        java.util.List<Double> s = stats.samples();
        chart.setData(s, WINDOW);
        double last = s.isEmpty() ? -1 : s.get(s.size() - 1);
        current.setText(s.isEmpty() ? "— ms" : last < 0 ? getString(R.string.latency_timeout) : fmt(last));
        String q = stats.quality();
        quality.setText(s.isEmpty() ? "" : getString("good".equals(q) ? R.string.latency_good
                : "fair".equals(q) ? R.string.latency_fair : R.string.latency_poor));
        details.setText(String.format(Locale.US,
                "%s: %s\n%s: %s\n%s: %s\n%s: %s\n%s: %.0f%% (%d/%d)",
                getString(R.string.stats_average), fmt(stats.avg()),
                getString(R.string.latency_min), fmt(stats.min()),
                getString(R.string.latency_max), fmt(stats.max()),
                getString(R.string.latency_jitter), fmt(stats.jitter()),
                getString(R.string.latency_loss), stats.lossPercent(), stats.lostCount(), stats.sent()));
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (running) stop();
    }

    @Override
    protected void onDestroy() {
        running = false;
        ui.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
