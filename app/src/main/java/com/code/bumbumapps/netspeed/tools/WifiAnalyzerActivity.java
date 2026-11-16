package com.code.bumbumapps.netspeed.tools;

import android.Manifest;
import androidx.appcompat.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.code.bumbumapps.netspeed.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

/**
 * Wi-Fi analyzer: live signal meter of the connected network, nearby networks, a channel graph and a
 * best-channel suggestion. Location permission (needed by Android for scan results) is requested only
 * here, after an explanation; without it the connected-network meter still works.
 */
public class WifiAnalyzerActivity extends AppCompatActivity {
    private static final int REQ_LOC = 301;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private WifiManager wm;
    private LinearLayout content, listCard, noteBox;
    private TextView dbm, meterInfo;
    private SignalBarView bar;
    private ChannelGraphView graph;
    private TextView bestText;
    private Button action;
    private int band = 24;
    private List<WifiLogic.Ap> aps = new ArrayList<>();
    private String connectedSsid;
    private boolean registered;
    private boolean scanned;
    private boolean resumed;

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent i) {
            readResults();
        }
    };

    private final Runnable meterTick = new Runnable() {
        @Override
        public void run() {
            updateMeter();
            if (resumed) ui.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tool_list);
        ToolsUi.header(this, R.string.tool_wifi_title);
        wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        content = findViewById(R.id.tool_content);
        action = findViewById(R.id.tool_action);
        action.setText(R.string.wifi_scan);
        action.setOnClickListener(v -> onScanClicked());

        LinearLayout live = ToolsUi.card(this, content, getString(R.string.wifi_live));
        dbm = ToolsUi.text(this, live, "— dBm", 34, R.color.maintextcolor, true);
        dbm.setTypeface(ToolsUi.font(this, R.font.jakarta_extrabold));
        bar = new SignalBarView(this);
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(-1, ToolsUi.dp(this, 10));
        blp.topMargin = ToolsUi.dp(this, 8);
        blp.bottomMargin = ToolsUi.dp(this, 8);
        live.addView(bar, blp);
        meterInfo = ToolsUi.text(this, live, "", 14, R.color.subtext, false);

        noteBox = new LinearLayout(this);
        noteBox.setOrientation(LinearLayout.VERTICAL);
        content.addView(noteBox, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout gcard = ToolsUi.card(this, content, getString(R.string.wifi_channels));
        ToolsUi.segment(this, gcard, new String[]{"2.4 GHz", "5 GHz"}, 0, i -> {
            band = i == 0 ? 24 : 5;
            renderGraph();
        });
        graph = new ChannelGraphView(this);
        gcard.addView(graph, new LinearLayout.LayoutParams(-1, ToolsUi.dp(this, 170)));
        bestText = ToolsUi.text(this, gcard, "", 14, R.color.cone, true);
        bestText.setPadding(0, ToolsUi.dp(this, 8), 0, ToolsUi.dp(this, 4));

        listCard = ToolsUi.card(this, content, getString(R.string.wifi_nearby));
        showEmptyList(getString(R.string.wifi_tap_scan));
        scanned = hasLocation();
        renderGraph();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!registered) {
            try {
                registerReceiver(receiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
                registered = true;
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        resumed = true;
        ui.post(meterTick);
        if (hasLocation() && scanned) readResults();
    }

    @Override
    protected void onPause() {
        resumed = false;
        ui.removeCallbacks(meterTick);
        super.onPause();
    }

    @Override
    protected void onStop() {
        if (registered) {
            try { unregisterReceiver(receiver); } catch (Exception ignored) { }
            registered = false;
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        ui.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    private boolean hasLocation() {
        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void onScanClicked() {
        if (wm == null || !wm.isWifiEnabled()) {
            Toast.makeText(this, R.string.wifi_off, Toast.LENGTH_LONG).show();
            return;
        }
        if (hasLocation()) {
            scan();
            return;
        }
        new AlertDialog.Builder(this, R.style.ToolDialog)
                .setTitle(R.string.wifi_perm_title)
                .setMessage(R.string.wifi_perm_msg)
                .setPositiveButton(R.string.wifi_perm_continue,
                        (d, w) -> requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQ_LOC))
                .setNegativeButton(R.string.wifi_perm_later, null)
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] res) {
        super.onRequestPermissionsResult(code, perms, res);
        if (code != REQ_LOC) return;
        if (res.length > 0 && res[0] == PackageManager.PERMISSION_GRANTED) {
            scan();
        } else {
            noteBox.removeAllViews();
            LinearLayout n = ToolsUi.card(this, noteBox, getString(R.string.wifi_perm_denied_title));
            ToolsUi.text(this, n, getString(R.string.wifi_perm_denied_msg), 14, R.color.subtext, false);
            showEmptyList(getString(R.string.wifi_perm_needed));
        }
    }

    private void scan() {
        noteBox.removeAllViews();
        scanned = true;
        if (!locationServicesOn()) {
            LinearLayout n = ToolsUi.card(this, noteBox, getString(R.string.wifi_loc_off_title));
            ToolsUi.text(this, n, getString(R.string.wifi_loc_off_msg), 14, R.color.subtext, false);
            Button b = new Button(this);
            b.setText(R.string.wifi_open_settings);
            b.setAllCaps(false);
            b.setOnClickListener(v -> {
                try { startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)); } catch (Exception ignored) { }
            });
            n.addView(b);
        }
        boolean started = false;
        try {
            started = wm.startScan();
        } catch (SecurityException ignored) {
        }
        if (!started) Toast.makeText(this, R.string.wifi_throttled, Toast.LENGTH_SHORT).show();
        readResults();
    }

    private boolean locationServicesOn() {
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (lm == null) return true;
        try {
            return Build.VERSION.SDK_INT >= 28 ? lm.isLocationEnabled()
                    : lm.isProviderEnabled(LocationManager.GPS_PROVIDER) || lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception e) {
            return true;
        }
    }

    @SuppressWarnings("deprecation")
    private void readResults() {
        if (isFinishing() || isDestroyed() || wm == null || !hasLocation()) return;
        List<ScanResult> raw;
        try {
            raw = wm.getScanResults();
        } catch (SecurityException e) {
            return;
        }
        HashMap<String, WifiLogic.Ap> strongest = new HashMap<>();
        if (raw != null) {
            for (ScanResult r : raw) {
                String name = r.SSID == null || r.SSID.isEmpty() ? getString(R.string.wifi_hidden) : r.SSID;
                WifiLogic.Ap a = new WifiLogic.Ap(name, r.frequency, r.level, WifiLogic.security(r.capabilities));
                String key = name + "|" + WifiLogic.bandOf(r.frequency);
                WifiLogic.Ap old = strongest.get(key);
                if (old == null || old.rssi < a.rssi) strongest.put(key, a);
            }
        }
        aps = new ArrayList<>(strongest.values());
        Collections.sort(aps, (x, y) -> Integer.compare(y.rssi, x.rssi));
        renderList();
        renderGraph();
    }

    private void showEmptyList(String msg) {
        listCard.removeAllViews();
        TextView title = ToolsUi.text(this, listCard, getString(R.string.wifi_nearby), 16, R.color.cone, true);
        title.setPadding(0, 0, 0, ToolsUi.dp(this, 6));
        ToolsUi.text(this, listCard, msg, 14, R.color.subtext, false)
                .setPadding(0, ToolsUi.dp(this, 4), 0, ToolsUi.dp(this, 8));
    }

    private void renderList() {
        if (aps.isEmpty()) {
            showEmptyList(getString(R.string.wifi_none));
            return;
        }
        listCard.removeAllViews();
        TextView title = ToolsUi.text(this, listCard,
                getString(R.string.wifi_nearby) + " (" + aps.size() + ")", 16, R.color.cone, true);
        title.setPadding(0, 0, 0, ToolsUi.dp(this, 6));
        for (WifiLogic.Ap a : aps) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            row.setPadding(0, ToolsUi.dp(this, 8), 0, ToolsUi.dp(this, 8));
            LinearLayout text = new LinearLayout(this);
            text.setOrientation(LinearLayout.VERTICAL);
            row.addView(text, new LinearLayout.LayoutParams(0, -2, 1f));
            ToolsUi.text(this, text, a.ssid, 15, R.color.maintextcolor, true);
            ToolsUi.text(this, text, String.format(Locale.US, "%s  \u00B7  ch %d  \u00B7  %.1f GHz  \u00B7  %d dBm",
                    a.security, a.channel(), a.freq / 1000.0, a.rssi), 13, R.color.subtext, false);
            SignalBarView sb = new SignalBarView(this);
            sb.setPercent(WifiLogic.percent(a.rssi));
            LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(ToolsUi.dp(this, 56), ToolsUi.dp(this, 6));
            blp.setMarginStart(ToolsUi.dp(this, 12));
            row.addView(sb, blp);
            listCard.addView(row);
        }
    }

    private void renderGraph() {
        graph.setData(aps, band, connectedSsid);
        if (!scanned || aps.isEmpty()) {
            bestText.setText(R.string.wifi_best_after_scan);
            return;
        }
        int best = WifiLogic.bestChannel(aps, band);
        int crowded = best < 0 ? 0 : WifiLogic.countOnChannel(aps, band, best);
        bestText.setText(best < 0 ? getString(R.string.wifi_best_none)
                : getString(R.string.wifi_best, best, crowded));
    }

    @SuppressWarnings("deprecation")
    private void updateMeter() {
        if (wm == null) {
            dbm.setText("— dBm");
            bar.setPercent(0);
            meterInfo.setText(R.string.wifi_not_connected);
            return;
        }
        WifiInfo wi = null;
        try {
            wi = wm.getConnectionInfo();
        } catch (Exception ignored) {
        }
        int rssi = wi == null ? -127 : wi.getRssi();
        if (wi == null || rssi >= 0 || rssi <= -127 || wi.getNetworkId() == -1 && wi.getLinkSpeed() <= 0) {
            dbm.setText("— dBm");
            bar.setPercent(0);
            meterInfo.setText(wm.isWifiEnabled() ? R.string.wifi_not_connected : R.string.wifi_off);
            connectedSsid = null;
            return;
        }
        String ssid = wi.getSSID();
        if (ssid == null || ssid.contains("unknown ssid")) ssid = getString(R.string.wifi_ssid_hidden);
        else if (ssid.length() > 1 && ssid.startsWith("\"") && ssid.endsWith("\"")) ssid = ssid.substring(1, ssid.length() - 1);
        connectedSsid = ssid;
        dbm.setText(rssi + " dBm");
        bar.setPercent(WifiLogic.percent(rssi));
        int f = wi.getFrequency();
        StringBuilder sb = new StringBuilder(ssid);
        if (f > 0) sb.append("\n").append(String.format(Locale.US, "%.1f GHz  ·  ch %d", f / 1000.0, WifiLogic.channelFromFreq(f)));
        if (wi.getLinkSpeed() > 0) sb.append("  ·  ").append(wi.getLinkSpeed()).append(" Mbps");
        meterInfo.setText(sb.toString());
    }
}
