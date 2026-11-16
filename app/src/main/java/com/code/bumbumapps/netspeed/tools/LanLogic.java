package com.code.bumbumapps.netspeed.tools;

import java.util.ArrayList;
import java.util.List;

/** Subnet maths for the device scanner (pure, unit tested). */
public final class LanLogic {
    private LanLogic() {}

    /** IPv4 text to int, or Integer.MIN_VALUE when invalid. */
    public static int toInt(String ip) {
        if (ip == null) return Integer.MIN_VALUE;
        String[] p = ip.split("\\.");
        if (p.length != 4) return Integer.MIN_VALUE;
        int v = 0;
        for (String s : p) {
            try {
                int n = Integer.parseInt(s);
                if (n < 0 || n > 255) return Integer.MIN_VALUE;
                v = (v << 8) | n;
            } catch (NumberFormatException e) {
                return Integer.MIN_VALUE;
            }
        }
        return v;
    }

    public static String toIp(int v) {
        return ((v >>> 24) & 255) + "." + ((v >>> 16) & 255) + "." + ((v >>> 8) & 255) + "." + (v & 255);
    }

    /**
     * Host addresses to probe: the device's own subnet, never larger than a /24 around the device
     * (so the sweep stays at 254 probes), without network and broadcast addresses.
     */
    public static List<String> hosts(String ip, int prefix) {
        ArrayList<String> out = new ArrayList<>();
        int a = toInt(ip);
        if (a == Integer.MIN_VALUE || prefix < 8 || prefix > 30) return out;
        int p = Math.max(prefix, 24);
        int mask = p == 0 ? 0 : -1 << (32 - p);
        int net = a & mask;
        int bcast = net | ~mask;
        for (int h = net + 1; h < bcast; h++) out.add(toIp(h));
        return out;
    }

    public static String subnetLabel(String ip, int prefix) {
        int a = toInt(ip);
        if (a == Integer.MIN_VALUE) return "";
        int p = Math.max(prefix, 24);
        int mask = -1 << (32 - p);
        return toIp(a & mask) + "/" + p;
    }

    /** Sort key so devices list in numeric IP order. */
    public static long sortKey(String ip) {
        int v = toInt(ip);
        return v == Integer.MIN_VALUE ? Long.MAX_VALUE : (v & 0xFFFFFFFFL);
    }
}
