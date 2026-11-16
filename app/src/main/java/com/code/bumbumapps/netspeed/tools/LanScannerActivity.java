package com.code.bumbumapps.netspeed.tools;

import android.content.Context;
import android.content.res.ColorStateList;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.RouteInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.code.bumbumapps.netspeed.R;

import java.io.IOException;
import java.net.ConnectException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Finds devices on the local /24 without root: each address is probed with short TCP connects (an
 * open port or an actively refused connection both prove a host is there) and an ICMP-style
 * reachability check, on a bounded background pool. Cancellable; stops when the screen is left.
 */
public class LanScannerActivity extends AppCompatActivity {
    private static final class Device {
        final String ip;
        long ms;
        String name;
        boolean gateway, self;

        Device(String ip, long ms) {
            this.ip = ip;
            this.ms = ms;
        }
    }

    private final Handler ui = new Handler(Looper.getMainLooper());
    private final List<Device> devices = new ArrayList<>();
    private LinearLayout content, statusCard, listCard;
    private TextView status;
    private ProgressBar progress;
    private Button action;
    private ExecutorService probes, names;
    private volatile int generation;
    private boolean scanning;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tool_list);
        ToolsUi.header(this, R.string.tool_lan_title);
        content = findViewById(R.id.tool_content);
        action = findViewById(R.id.tool_action);
        action.setText(R.string.lan_scan);
        action.setOnClickListener(v -> {
            if (scanning) stop(); else start();
        });

        statusCard = ToolsUi.card(this, content, getString(R.string.lan_status));
        status = ToolsUi.text(this, statusCard, getString(R.string.lan_tap_scan), 14, R.color.subtext, false);
        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setProgressTintList(ColorStateList.valueOf(0xFF38BDF8));
        progress.setProgressBackgroundTintList(ColorStateList.valueOf(0xFF272B38));
        progress.setVisibility(android.view.View.GONE);
        LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(-1, ToolsUi.dp(this, 8));
        plp.topMargin = ToolsUi.dp(this, 10);
        plp.bottomMargin = ToolsUi.dp(this, 6);
        statusCard.addView(progress, plp);
        listCard = ToolsUi.card(this, content, getString(R.string.lan_devices));
        listCard.setVisibility(android.view.View.GONE);
    }

    /** Local IPv4 address, prefix and gateway of the Wi-Fi/Ethernet link, or null. */
    private String[] localLink() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return null;
        try {
            for (Network n : cm.getAllNetworks()) {
                NetworkCapabilities caps = cm.getNetworkCapabilities(n);
                if (caps == null) continue;
                if (!caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                        && !caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) continue;
                LinkProperties lp = cm.getLinkProperties(n);
                if (lp == null) continue;
                for (LinkAddress a : lp.getLinkAddresses()) {
                    if (a.getAddress() instanceof Inet4Address && !a.getAddress().isLoopbackAddress()) {
                        String gw = "";
                        for (RouteInfo r : lp.getRoutes()) {
                            if (r.isDefaultRoute() && r.getGateway() instanceof Inet4Address) {
                                gw = r.getGateway().getHostAddress();
                            }
                        }
                        return new String[]{a.getAddress().getHostAddress(), String.valueOf(a.getPrefixLength()), gw};
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private void start() {
        String[] link = localLink();
        if (link == null) {
            status.setText(R.string.lan_need_wifi);
            return;
        }
        final String self = link[0], gateway = link[2];
        final List<String> hosts = LanLogic.hosts(self, Integer.parseInt(link[1]));
        if (hosts.isEmpty()) {
            status.setText(R.string.lan_need_wifi);
            return;
        }
        final int gen = ++generation;
        devices.clear();
        Device me = new Device(self, 0);
        me.self = true;
        me.name = getString(R.string.lan_this_phone);
        devices.add(me);
        scanning = true;
        action.setText(R.string.lan_stop);
        progress.setMax(hosts.size());
        progress.setProgress(0);
        progress.setVisibility(android.view.View.VISIBLE);
        listCard.setVisibility(android.view.View.VISIBLE);
        final String label = LanLogic.subnetLabel(self, Integer.parseInt(link[1]));
        status.setText(getString(R.string.lan_scanning, label, 0, hosts.size()));
        renderList();

        probes = Executors.newFixedThreadPool(40);
        names = Executors.newFixedThreadPool(6);
        final ExecutorService p = probes, nm = names;
        final AtomicInteger done = new AtomicInteger();
        for (final String ip : hosts) {
            p.execute(() -> {
                long ms = ip.equals(self) ? -1 : probe(ip);
                if (gen != generation) return;
                int d = done.incrementAndGet();
                final long fms = ms;
                ui.post(() -> {
                    if (gen != generation) return;
                    progress.setProgress(d);
                    status.setText(getString(R.string.lan_scanning, label, d, hosts.size()));
                    if (fms >= 0) {
                        Device dev = new Device(ip, fms);
                        dev.gateway = ip.equals(gateway);
                        devices.add(dev);
                        renderList();
                        try {
                            nm.execute(() -> resolveName(gen, dev));
                        } catch (Exception ignored) {
                        }
                    }
                    if (d >= hosts.size()) scanDone();
                });
            });
        }
    }

    private void resolveName(int gen, Device dev) {
        String n = null;
        try {
            String h = InetAddress.getByName(dev.ip).getCanonicalHostName();
            if (h != null && !h.equals(dev.ip)) n = h;
        } catch (Exception ignored) {
        }
        if (n == null || gen != generation) return;
        final String fn = n;
        ui.post(() -> {
            if (gen != generation) return;
            dev.name = fn;
            renderList();
        });
    }

    /** Milliseconds to a sign of life from this host, or -1 when nothing answered. */
    static long probe(String ip) {
        InetAddress addr;
        try {
            addr = InetAddress.getByName(ip);
        } catch (Exception e) {
            return -1;
        }
        for (int port : new int[]{80, 443, 53, 445}) {
            if (Thread.currentThread().isInterrupted()) return -1;
            long t0 = System.nanoTime();
            try (Socket s = new Socket()) {
                s.connect(new InetSocketAddress(addr, port), 250);
                return Math.max(1, (System.nanoTime() - t0) / 1_000_000);
            } catch (ConnectException e) {
                String m = String.valueOf(e.getMessage()).toLowerCase(Locale.ROOT);
                if (m.contains("refused")) return Math.max(1, (System.nanoTime() - t0) / 1_000_000);
                break;
            } catch (IOException ignored) {
            }
        }
        long t0 = System.nanoTime();
        try {
            if (addr.isReachable(300)) return Math.max(1, (System.nanoTime() - t0) / 1_000_000);
        } catch (IOException ignored) {
        }
        return -1;
    }

    private void scanDone() {
        scanning = false;
        action.setText(R.string.lan_scan);
        progress.setVisibility(android.view.View.GONE);
        status.setText(getString(R.string.lan_found, devices.size()));
        shutdown();
    }

    private void stop() {
        generation++;
        scanning = false;
        action.setText(R.string.lan_scan);
        progress.setVisibility(android.view.View.GONE);
        status.setText(getString(R.string.lan_stopped, devices.size()));
        shutdown();
    }

    private void shutdown() {
        if (probes != null) probes.shutdownNow();
        if (names != null) names.shutdownNow();
        probes = null;
        names = null;
    }

    private void renderList() {
        listCard.removeAllViews();
        TextView title = ToolsUi.text(this, listCard,
                getString(R.string.lan_devices) + " (" + devices.size() + ")", 16, R.color.cone, true);
        title.setPadding(0, 0, 0, ToolsUi.dp(this, 6));
        List<Device> sorted = new ArrayList<>(devices);
        Collections.sort(sorted, (a, b) -> Long.compare(LanLogic.sortKey(a.ip), LanLogic.sortKey(b.ip)));
        for (Device d : sorted) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(0, ToolsUi.dp(this, 8), 0, ToolsUi.dp(this, 8));
            ToolsUi.text(this, row, d.ip, 15, R.color.maintextcolor, true);
            StringBuilder sb = new StringBuilder();
            if (d.gateway) sb.append(getString(R.string.lan_router)).append("  \u00B7  ");
            if (d.self) sb.append(getString(R.string.lan_this_phone));
            else {
                if (d.name != null) sb.append(d.name).append("  \u00B7  ");
                else if (!d.gateway) sb.append(getString(R.string.lan_unknown)).append("  \u00B7  ");
                sb.append(d.ms).append(" ms");
            }
            TextView sub = ToolsUi.text(this, row, sb.toString(), 13, R.color.subtext, false);
            sub.setSingleLine(false);
            listCard.addView(row);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (scanning) stop();
    }

    @Override
    protected void onDestroy() {
        generation++;
        shutdown();
        ui.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
