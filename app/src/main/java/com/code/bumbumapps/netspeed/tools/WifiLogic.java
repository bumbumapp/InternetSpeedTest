package com.code.bumbumapps.netspeed.tools;

import java.util.List;

/** Pure Wi-Fi helpers: channel maths, security label and best-channel suggestion (unit tested). */
public final class WifiLogic {
    private WifiLogic() {}

    public static final int[] CHANNELS_24 = {1, 6, 11};
    public static final int[] CHANNELS_5 = {36, 40, 44, 48, 149, 153, 157, 161, 165};

    /** One nearby access point, reduced to what the analysis needs. */
    public static final class Ap {
        public final String ssid;
        public final int freq;
        public final int rssi;
        public final String security;

        public Ap(String ssid, int freq, int rssi, String security) {
            this.ssid = ssid;
            this.freq = freq;
            this.rssi = rssi;
            this.security = security;
        }

        public int channel() { return channelFromFreq(freq); }
        public int band() { return bandOf(freq); }
    }

    public static int channelFromFreq(int mhz) {
        if (mhz == 2484) return 14;
        if (mhz >= 2412 && mhz <= 2472) return (mhz - 2407) / 5;
        if (mhz >= 5160 && mhz <= 5885) return (mhz - 5000) / 5;
        if (mhz >= 5955 && mhz <= 7115) return (mhz - 5950) / 5;
        return -1;
    }

    /** 24, 5 or 6 (GHz band) or 0 when unknown. */
    public static int bandOf(int mhz) {
        if (mhz >= 2400 && mhz < 2500) return 24;
        if (mhz >= 5000 && mhz < 5900) return 5;
        if (mhz >= 5900 && mhz < 7200) return 6;
        return 0;
    }

    /** 0..4 bars from RSSI in dBm. */
    public static int level(int rssi) {
        if (rssi >= -55) return 4;
        if (rssi >= -67) return 3;
        if (rssi >= -75) return 2;
        if (rssi >= -85) return 1;
        return 0;
    }

    /** 0..100 for the live meter. */
    public static int percent(int rssi) {
        return Math.max(0, Math.min(100, 2 * (rssi + 100)));
    }

    public static String security(String capabilities) {
        String c = capabilities == null ? "" : capabilities.toUpperCase(java.util.Locale.US);
        if (c.contains("SAE")) return "WPA3";
        if (c.contains("WPA2") || c.contains("RSN")) return "WPA2";
        if (c.contains("WPA")) return "WPA";
        if (c.contains("WEP")) return "WEP";
        return "Open";
    }

    /** Interference this channel would suffer from the given networks (lower is better). */
    static double interference(int candidate, boolean band24, List<Ap> aps) {
        double sum = 0;
        for (Ap a : aps) {
            int ch = a.channel();
            if (ch < 0) continue;
            double w = Math.max(0, a.rssi + 100);
            w = w * w / 100.0;
            int d = Math.abs(ch - candidate);
            if (band24) {
                if (d < 5) sum += w * (1.0 - d / 5.0);
            } else if (d == 0) {
                sum += w;
            }
        }
        return sum;
    }

    /** Least crowded channel of the band (24 or 5), or -1 when the band has no scan data and no suggestion applies. */
    public static int bestChannel(List<Ap> all, int band) {
        java.util.ArrayList<Ap> inBand = new java.util.ArrayList<>();
        for (Ap a : all) if (a.band() == band) inBand.add(a);
        int[] cands = band == 24 ? CHANNELS_24 : band == 5 ? CHANNELS_5 : new int[0];
        int best = -1;
        double bestScore = Double.MAX_VALUE;
        for (int c : cands) {
            double s = interference(c, band == 24, inBand);
            if (s < bestScore - 1e-9) {
                bestScore = s;
                best = c;
            }
        }
        return best;
    }

    /** How many networks use each channel number (index = channel). */
    public static int countOnChannel(List<Ap> all, int band, int channel) {
        int n = 0;
        for (Ap a : all) if (a.band() == band && a.channel() == channel) n++;
        return n;
    }
}
