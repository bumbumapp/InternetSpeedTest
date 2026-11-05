package com.code.bumbumapps.netspeed.tools;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.RouteInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.code.bumbumapps.netspeed.R;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Shows what the device knows about the active connection, fully offline. */
public class NetworkDetailsActivity extends AppCompatActivity {
    private LinearLayout content;
    private final StringBuilder plain = new StringBuilder();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tool_list);
        ToolsUi.header(this, R.string.tool_network_title);
        content = findViewById(R.id.tool_content);
        Button action = findViewById(R.id.tool_action);
        action.setText(R.string.network_copy);
        action.setOnClickListener(v -> {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) {
                cm.setPrimaryClip(ClipData.newPlainText(getString(R.string.tool_network_title), plain.toString()));
                Toast.makeText(this, R.string.network_copied, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        render();
    }

    private void line(LinearLayout card, String k, String v) {
        ToolsUi.row(this, card, k, v);
        plain.append(k).append(": ").append(v).append('\n');
    }

    private static String join(List<String> l) {
        if (l.isEmpty()) return "—";
        StringBuilder sb = new StringBuilder();
        for (String s : l) {
            if (sb.length() > 0) sb.append('\n');
            sb.append(s);
        }
        return sb.toString();
    }

    private void render() {
        content.removeAllViews();
        plain.setLength(0);
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        Network net = cm == null ? null : cm.getActiveNetwork();
        NetworkCapabilities caps = net == null ? null : cm.getNetworkCapabilities(net);
        if (net == null || caps == null) {
            ToolsUi.message(this, content, getString(R.string.network_offline));
            return;
        }
        String yes = getString(R.string.yes), no = getString(R.string.no);

        LinearLayout c1 = ToolsUi.card(this, content, getString(R.string.network_connection));
        String type = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ? "Wi-Fi"
                : caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ? getString(R.string.network_mobile)
                : caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ? "Ethernet"
                : caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) ? "VPN" : getString(R.string.network_other);
        line(c1, getString(R.string.network_type), type);
        line(c1, getString(R.string.network_internet),
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) ? yes : no);
        line(c1, getString(R.string.network_metered),
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) ? no : yes);
        line(c1, getString(R.string.network_vpn),
                caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) ? yes : no);
        int down = caps.getLinkDownstreamBandwidthKbps(), up = caps.getLinkUpstreamBandwidthKbps();
        if (down > 0) line(c1, getString(R.string.network_est_down), kbps(down));
        if (up > 0) line(c1, getString(R.string.network_est_up), kbps(up));

        if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            WifiInfo wi = wm == null ? null : wm.getConnectionInfo();
            if (wi != null) {
                LinearLayout c2 = ToolsUi.card(this, content, "Wi-Fi");
                if (wi.getLinkSpeed() > 0) line(c2, getString(R.string.network_link_speed), wi.getLinkSpeed() + " Mbps");
                int rssi = wi.getRssi();
                if (rssi > -127 && rssi < 0) {
                    int level = WifiManager.calculateSignalLevel(rssi, 5);
                    line(c2, getString(R.string.network_signal), rssi + " dBm (" + level + "/4)");
                }
                int f = wi.getFrequency();
                if (f > 0) line(c2, getString(R.string.network_frequency),
                        String.format(Locale.US, "%.1f GHz", f / 1000.0));
            }
        }

        LinkProperties lp = cm.getLinkProperties(net);
        if (lp != null) {
            LinearLayout c3 = ToolsUi.card(this, content, getString(R.string.network_addresses));
            if (lp.getInterfaceName() != null) line(c3, getString(R.string.network_interface), lp.getInterfaceName());
            List<String> v4 = new ArrayList<>(), v6 = new ArrayList<>();
            for (LinkAddress a : lp.getLinkAddresses()) {
                String s = a.getAddress().getHostAddress() + "/" + a.getPrefixLength();
                if (a.getAddress().getHostAddress() != null && a.getAddress().getHostAddress().contains(":")) v6.add(s);
                else v4.add(s);
            }
            line(c3, "IPv4", join(v4));
            line(c3, "IPv6", join(v6));
            String gw = "—";
            for (RouteInfo r : lp.getRoutes()) {
                if (r.isDefaultRoute() && r.getGateway() != null && !r.getGateway().isAnyLocalAddress()) {
                    gw = r.getGateway().getHostAddress();
                    if (gw != null && !gw.contains(":")) break;
                }
            }
            line(c3, getString(R.string.network_gateway), gw);
            List<String> dns = new ArrayList<>();
            for (InetAddress d : lp.getDnsServers()) dns.add(d.getHostAddress());
            line(c3, "DNS", join(dns));
            if (lp.getMtu() > 0) line(c3, "MTU", String.valueOf(lp.getMtu()));
        }
    }

    private static String kbps(int k) {
        return k >= 1000 ? String.format(Locale.US, "%.0f Mbps", k / 1000.0) : k + " Kbps";
    }
}
