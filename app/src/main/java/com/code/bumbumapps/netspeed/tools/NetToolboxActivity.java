package com.code.bumbumapps.netspeed.tools;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.code.bumbumapps.netspeed.R;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.ConnectException;
import java.util.Locale;

/** Ping, traceroute (TTL probes), DNS lookup and TCP port check in one screen; every run is cancellable. */
public class NetToolboxActivity extends AppCompatActivity {
    public static final String EXTRA_MODE = "mode";
    public static final String MODE_PING = "ping", MODE_TRACE = "trace", MODE_DNS = "dns", MODE_PORT = "port";
    private static final String[] MODES = {MODE_PING, MODE_TRACE, MODE_DNS, MODE_PORT};

    private final Handler ui = new Handler(Looper.getMainLooper());
    private LinearLayout content, results;
    private EditText host, port;
    private TextView hostLabel;
    private Button action;
    private TextView title;
    private int mode;
    private volatile int generation;
    private volatile Process proc;
    private boolean running;
    private final String[] hostByMode = {"8.8.8.8", "8.8.8.8", "example.com", "example.com"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tool_list);
        title = findViewById(R.id.tool_title);
        findViewById(R.id.tool_back).setOnClickListener(v -> finish());
        content = findViewById(R.id.tool_content);
        action = findViewById(R.id.tool_action);
        String m = getIntent().getStringExtra(EXTRA_MODE);
        for (int i = 0; i < MODES.length; i++) if (MODES[i].equals(m)) mode = i;
        if (savedInstanceState != null) {
            mode = savedInstanceState.getInt("mode", mode);
            String[] h = savedInstanceState.getStringArray("hosts");
            if (h != null && h.length == hostByMode.length) System.arraycopy(h, 0, hostByMode, 0, h.length);
        }

        ToolsUi.segment(this, content, new String[]{getString(R.string.tool_ping_title),
                getString(R.string.mode_trace), getString(R.string.mode_dns), getString(R.string.mode_port)},
                mode, i -> {
                    stop();
                    hostByMode[mode] = host.getText().toString();
                    mode = i;
                    applyMode();
                });

        LinearLayout in = new LinearLayout(this);
        in.setOrientation(LinearLayout.VERTICAL);
        content.addView(in, new LinearLayout.LayoutParams(-1, -2));
        hostLabel = ToolsUi.text(this, in, getString(R.string.nt_host), 13, R.color.subtext, false);
        host = new EditText(this);
        styleInput(host, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI, getString(R.string.nt_host_hint));
        host.setImeOptions(EditorInfo.IME_ACTION_GO);
        host.setOnEditorActionListener((v, a, e) -> {
            if (!running) start();
            return true;
        });
        in.addView(host, inputLp(0));
        port = new EditText(this);
        styleInput(port, InputType.TYPE_CLASS_NUMBER, getString(R.string.nt_port_hint));
        port.setText("443");
        in.addView(port, inputLp(8));

        results = new LinearLayout(this);
        results.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(-1, -2);
        rlp.topMargin = ToolsUi.dp(this, 16);
        content.addView(results, rlp);

        action.setOnClickListener(v -> {
            if (running) stop(); else start();
        });
        applyMode();
    }

    private LinearLayout.LayoutParams inputLp(int topDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, ToolsUi.dp(this, 52));
        lp.topMargin = ToolsUi.dp(this, topDp == 0 ? 6 : topDp);
        return lp;
    }

    private void styleInput(EditText e, int type, String hint) {
        e.setBackgroundResource(R.drawable.bg_input);
        e.setInputType(type);
        e.setHint(hint);
        e.setMaxLines(1);
        e.setSingleLine(true);
        e.setImeOptions(EditorInfo.IME_ACTION_DONE);
        e.setImportantForAutofill(android.view.View.IMPORTANT_FOR_AUTOFILL_NO);
        e.setPadding(ToolsUi.dp(this, 16), 0, ToolsUi.dp(this, 16), 0);
        e.setTextColor(getResources().getColor(R.color.maintextcolor));
        e.setHintTextColor(getResources().getColor(R.color.subtext));
        e.setTextSize(16);
        e.setTypeface(ToolsUi.font(this, R.font.jakarta_medium));
    }

    private void applyMode() {
        int[] titles = {R.string.tool_ping_title, R.string.tool_trace_title, R.string.tool_dns_title,
                R.string.tool_port_title};
        title.setText(titles[mode]);
        host.setText(hostByMode[mode]);
        port.setVisibility(mode == 3 ? android.view.View.VISIBLE : android.view.View.GONE);
        action.setText(running ? R.string.nt_stop : R.string.nt_run);
        results.removeAllViews();
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        hostByMode[mode] = host.getText().toString();
        out.putInt("mode", mode);
        out.putStringArray("hosts", hostByMode);
    }

    private void start() {
        String h = NetInput.cleanHost(host.getText().toString());
        if (h == null) {
            host.setError(getString(R.string.nt_bad_host));
            return;
        }
        int p = -1;
        if (mode == 3) {
            p = NetInput.parsePort(port.getText().toString());
            if (p < 0) {
                port.setError(getString(R.string.nt_bad_port));
                return;
            }
        }
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(host.getWindowToken(), 0);
        results.removeAllViews();
        final int gen = ++generation;
        running = true;
        action.setText(R.string.nt_stop);
        final String fh = h;
        final int fp = p;
        final int m = mode;
        Thread t = new Thread(() -> {
            try {
                switch (m) {
                    case 0: runPing(gen, fh); break;
                    case 1: runTrace(gen, fh); break;
                    case 2: runDns(gen, fh); break;
                    default: runPort(gen, fh, fp); break;
                }
            } catch (Exception e) {
                line(gen, getString(R.string.nt_error, String.valueOf(e.getMessage())), R.color.subtext);
            } finally {
                ui.post(() -> {
                    if (gen == generation) finishRun();
                });
            }
        }, "net-toolbox");
        t.start();
    }

    private void finishRun() {
        running = false;
        action.setText(R.string.nt_run);
    }

    private void stop() {
        generation++;
        Process p = proc;
        if (p != null) p.destroy();
        if (running) finishRun();
    }

    private void line(int gen, String text, int colorRes) {
        ui.post(() -> {
            if (gen != generation || isFinishing() || isDestroyed()) return;
            TextView t = new TextView(this);
            t.setText(text);
            t.setTextSize(14);
            t.setTextColor(getResources().getColor(colorRes));
            t.setTypeface(ToolsUi.font(this, R.font.jakarta_medium));
            t.setTextIsSelectable(true);
            ToolsUi.dir(t);
            t.setPadding(ToolsUi.dp(this, 16), ToolsUi.dp(this, 6), ToolsUi.dp(this, 16), ToolsUi.dp(this, 6));
            if (results.getChildCount() == 0) {
                results.setBackgroundResource(R.drawable.is_card);
                results.setPadding(0, ToolsUi.dp(this, 8), 0, ToolsUi.dp(this, 8));
            }
            results.addView(t);
        });
    }

    private boolean live(int gen) {
        return gen == generation && !Thread.currentThread().isInterrupted();
    }

    private static InetAddress resolve(String h) throws Exception {
        InetAddress[] all = InetAddress.getAllByName(h);
        for (InetAddress a : all) if (a instanceof Inet4Address) return a;
        return all[0];
    }

    // ---------------------------------------------------------------- ping
    private void runPing(int gen, String h) throws Exception {
        InetAddress a;
        try {
            a = resolve(h);
        } catch (Exception e) {
            line(gen, getString(R.string.nt_unresolved, h), R.color.subtext);
            return;
        }
        line(gen, "PING " + h + " (" + a.getHostAddress() + ")", R.color.cone);
        boolean icmp = a instanceof Inet4Address;
        boolean noted = false;
        int sent = 0, ok = 0;
        double sum = 0, min = Double.MAX_VALUE, max = 0;
        for (int i = 1; i <= 10 && live(gen); i++) {
            double ms = icmp ? icmpPing(a.getHostAddress(), 1, 2) : -1;
            if (ms < 0 && i == 1 && icmp) {
                double tcp = tcpPing(a);
                if (tcp >= 0) {
                    icmp = false;
                    ms = tcp;
                }
            } else if (ms < 0 && !icmp) {
                ms = tcpPing(a);
            }
            if (!live(gen)) break;
            if (!icmp && !noted) {
                line(gen, getString(R.string.nt_tcp_note), R.color.subtext);
                noted = true;
            }
            sent++;
            if (ms >= 0) {
                ok++;
                sum += ms;
                min = Math.min(min, ms);
                max = Math.max(max, ms);
                line(gen, String.format(Locale.US, "#%d   %.1f ms", i, ms), R.color.maintextcolor);
            } else {
                line(gen, String.format(Locale.US, "#%d   %s", i, getString(R.string.latency_timeout)), R.color.subtext);
            }
            try { Thread.sleep(500); } catch (InterruptedException e) { return; }
        }
        if (sent > 0 && live(gen)) {
            line(gen, String.format(Locale.US, "%s: %d/%d  (%.0f%% %s)", getString(R.string.nt_received), ok, sent,
                    100.0 * (sent - ok) / sent, getString(R.string.latency_loss).toLowerCase(Locale.getDefault())), R.color.cone);
            if (ok > 0) {
                line(gen, String.format(Locale.US, "min %.1f / avg %.1f / max %.1f ms", min, sum / ok, max), R.color.cone);
            }
        }
    }

    /** Round-trip time in ms from the system ping binary, or -1. */
    private double icmpPing(String ip, int ttlOrCount, int waitSec) {
        String out = runPingProcess(new String[]{"ping", "-c", "1", "-W", String.valueOf(waitSec), ip});
        return NetInput.isEchoReply(out) ? NetInput.parsePingTime(out) : -1;
    }

    private String runPingProcess(String[] cmd) {
        Process p = null;
        try {
            p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
            proc = p;
            NetInput.killAfter(p, 6000);
            StringBuilder sb = new StringBuilder();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String l;
                while ((l = r.readLine()) != null && sb.length() < 4000) sb.append(l).append('\n');
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        } finally {
            if (p != null) p.destroy();
            proc = null;
        }
    }

    private static double tcpPing(InetAddress a) {
        for (int port : new int[]{443, 80}) {
            long t0 = System.nanoTime();
            try (Socket s = new Socket()) {
                s.connect(new InetSocketAddress(a, port), 2000);
                return (System.nanoTime() - t0) / 1e6;
            } catch (ConnectException e) {
                if (String.valueOf(e.getMessage()).toLowerCase(Locale.ROOT).contains("refused")) {
                    return (System.nanoTime() - t0) / 1e6;
                }
            } catch (Exception ignored) {
            }
        }
        return -1;
    }

    // ---------------------------------------------------------------- traceroute
    private void runTrace(int gen, String h) throws Exception {
        InetAddress a;
        try {
            a = resolve(h);
        } catch (Exception e) {
            line(gen, getString(R.string.nt_unresolved, h), R.color.subtext);
            return;
        }
        if (!(a instanceof Inet4Address)) {
            line(gen, getString(R.string.nt_trace_v4), R.color.subtext);
            return;
        }
        String ip = a.getHostAddress();
        line(gen, getString(R.string.nt_trace_head, h, ip, 30), R.color.cone);
        int silent = 0, answered = 0;
        for (int ttl = 1; ttl <= 30 && live(gen); ttl++) {
            long t0 = System.nanoTime();
            String out = runPingProcess(new String[]{"ping", "-c", "1", "-t", String.valueOf(ttl), "-W", "2", ip});
            double wall = (System.nanoTime() - t0) / 1e6;
            if (!live(gen)) return;
            String hop = NetInput.parseHopAddress(out);
            if (NetInput.isEchoReply(out)) {
                line(gen, String.format(Locale.US, "%2d   %s   %.1f ms", ttl, ip, NetInput.parsePingTime(out)), R.color.maintextcolor);
                line(gen, getString(R.string.nt_trace_done, ttl), R.color.cone);
                return;
            } else if (hop != null) {
                answered++;
                silent = 0;
                line(gen, String.format(Locale.US, "%2d   %s   ~%.0f ms", ttl, hop, Math.min(wall, 2000)), R.color.maintextcolor);
            } else {
                silent++;
                line(gen, String.format(Locale.US, "%2d   * * *", ttl), R.color.subtext);
                if (silent >= 8) break;
            }
        }
        if (live(gen)) {
            line(gen, getString(answered == 0 ? R.string.nt_trace_blocked : R.string.nt_trace_incomplete), R.color.subtext);
        }
    }

    // ---------------------------------------------------------------- DNS
    private void runDns(int gen, String h) {
        long t0 = System.nanoTime();
        InetAddress[] all;
        try {
            all = InetAddress.getAllByName(h);
        } catch (Exception e) {
            line(gen, getString(R.string.nt_unresolved, h), R.color.subtext);
            return;
        }
        double ms = (System.nanoTime() - t0) / 1e6;
        if (gen != generation) return;
        line(gen, getString(R.string.nt_dns_head, h), R.color.cone);
        int n4 = 0, n6 = 0;
        for (InetAddress a : all) {
            if (a instanceof Inet4Address) {
                line(gen, "A      " + a.getHostAddress(), R.color.maintextcolor);
                n4++;
            } else if (a instanceof Inet6Address) {
                line(gen, "AAAA  " + a.getHostAddress(), R.color.maintextcolor);
                n6++;
            }
        }
        line(gen, String.format(Locale.US, "%s: %.0f ms  (IPv4 %d, IPv6 %d)", getString(R.string.nt_dns_time), ms, n4, n6), R.color.cone);
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        try {
            Network net = cm == null ? null : cm.getActiveNetwork();
            LinkProperties lp = net == null ? null : cm.getLinkProperties(net);
            if (lp != null && !lp.getDnsServers().isEmpty()) {
                StringBuilder sb = new StringBuilder(getString(R.string.nt_dns_servers)).append(": ");
                for (InetAddress d : lp.getDnsServers()) sb.append(d.getHostAddress()).append("  ");
                line(gen, sb.toString().trim(), R.color.subtext);
            }
        } catch (Exception ignored) {
        }
    }

    // ---------------------------------------------------------------- port check
    private void runPort(int gen, String h, int p) {
        InetAddress a;
        try {
            a = resolve(h);
        } catch (Exception e) {
            line(gen, getString(R.string.nt_unresolved, h), R.color.subtext);
            return;
        }
        String svc = NetInput.serviceName(p);
        line(gen, h + ":" + p + (svc.isEmpty() ? "" : "  (" + svc + ")"), R.color.cone);
        long t0 = System.nanoTime();
        String res;
        int col = R.color.subtext;
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(a, p), 3000);
            res = String.format(Locale.US, "%s  -  %.0f ms", getString(R.string.nt_port_open),
                    (System.nanoTime() - t0) / 1e6);
            col = R.color.maintextcolor;
        } catch (SocketTimeoutException e) {
            res = getString(R.string.nt_port_filtered);
        } catch (ConnectException e) {
            res = String.valueOf(e.getMessage()).toLowerCase(Locale.ROOT).contains("refused")
                    ? getString(R.string.nt_port_closed) : getString(R.string.nt_port_unreachable);
        } catch (Exception e) {
            res = getString(R.string.nt_port_unreachable);
        }
        line(gen, res, col);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (running) stop();
    }

    @Override
    protected void onDestroy() {
        generation++;
        Process p = proc;
        if (p != null) p.destroy();
        ui.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
