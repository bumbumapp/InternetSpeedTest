package com.code.bumbumapps.netspeed.tools;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Aggregates the saved speed test rows (values are stored as display text such as "12.5 Mbps"). */
public class HistoryStats {
    private static final Pattern NUM = Pattern.compile("^\\s*([0-9]+(?:[.,][0-9]+)?)\\s*(.*)$");

    public int count;
    public double bestDownload = -1, worstDownload = -1, avgDownload = -1;
    public double bestUpload = -1, avgUpload = -1;
    public double bestPing = -1, avgPing = -1;
    public int wifiCount, mobileCount;

    /** Speed text to Mbps; "MB/s" values are multiplied by 8. Returns -1 when unparsable. */
    public static double parseMbps(String s) {
        if (s == null) return -1;
        Matcher m = NUM.matcher(s);
        if (!m.find()) return -1;
        double v = Double.parseDouble(m.group(1).replace(',', '.'));
        String unit = m.group(2).toLowerCase(Locale.ROOT);
        if (unit.startsWith("mb/s")) v *= 8;
        else if (unit.startsWith("kb/s")) v = v * 8 / 1000;
        return v;
    }

    /** Ping text such as "12.5 ms" to ms, -1 when it is a dash or unparsable. */
    public static double parseMs(String s) {
        if (s == null) return -1;
        Matcher m = NUM.matcher(s);
        if (!m.find()) return -1;
        return Double.parseDouble(m.group(1).replace(',', '.'));
    }

    public static HistoryStats of(List<String> type, List<String> ping, List<String> down, List<String> up) {
        HistoryStats r = new HistoryStats();
        r.count = down.size();
        double sd = 0, su = 0, sp = 0;
        int nd = 0, nu = 0, np = 0;
        for (int i = 0; i < r.count; i++) {
            String t = i < type.size() && type.get(i) != null ? type.get(i).toUpperCase(Locale.ROOT) : "";
            if (t.contains("WIFI")) r.wifiCount++;
            else if (t.contains("MOBILE")) r.mobileCount++;
            double d = parseMbps(down.get(i));
            if (d > 0) {
                sd += d; nd++;
                if (d > r.bestDownload) r.bestDownload = d;
                if (r.worstDownload < 0 || d < r.worstDownload) r.worstDownload = d;
            }
            double u = i < up.size() ? parseMbps(up.get(i)) : -1;
            if (u > 0) {
                su += u; nu++;
                if (u > r.bestUpload) r.bestUpload = u;
            }
            double p = i < ping.size() ? parseMs(ping.get(i)) : -1;
            if (p > 0) {
                sp += p; np++;
                if (r.bestPing < 0 || p < r.bestPing) r.bestPing = p;
            }
        }
        if (nd > 0) r.avgDownload = sd / nd;
        if (nu > 0) r.avgUpload = su / nu;
        if (np > 0) r.avgPing = sp / np;
        return r;
    }

    /** RFC 4180 CSV escaping. */
    public static String csv(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    public static String toCsv(List<String> time, List<String> type, List<String> ping,
                               List<String> down, List<String> up) {
        StringBuilder sb = new StringBuilder("time,network,ping,download,upload\n");
        for (int i = 0; i < time.size(); i++) {
            sb.append(csv(time.get(i))).append(',').append(csv(type.get(i))).append(',')
                    .append(csv(ping.get(i))).append(',').append(csv(down.get(i))).append(',')
                    .append(csv(up.get(i))).append('\n');
        }
        return sb.toString();
    }
}
