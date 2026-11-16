package com.code.bumbumapps.netspeed.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Input validation and command-output parsing for the network toolbox (pure, unit tested). */
public final class NetInput {
    private NetInput() {}

    /** Cleans "https://Host.com/path" to "host.com"; returns null if it is not a usable host name or IP. */
    public static String cleanHost(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        int scheme = s.indexOf("://");
        if (scheme >= 0) s = s.substring(scheme + 3);
        int slash = s.indexOf('/');
        if (slash >= 0) s = s.substring(0, slash);
        int at = s.lastIndexOf('@');
        if (at >= 0) s = s.substring(at + 1);
        if (s.startsWith("[") && s.contains("]")) {
            s = s.substring(1, s.indexOf(']'));
        } else if (s.indexOf(':') >= 0 && s.indexOf(':') == s.lastIndexOf(':')) {
            s = s.substring(0, s.indexOf(':'));
        }
        if (s.isEmpty() || s.length() > 253) return null;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            boolean ok = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
                    || c == '.' || c == '-' || c == '_' || c == ':';
            if (!ok) return null;
        }
        if (s.startsWith("-") || s.startsWith(".") || s.endsWith(".")) return null;
        return s.toLowerCase(Locale.ROOT);
    }

    /** Port 1..65535 or -1. */
    public static int parsePort(String raw) {
        if (raw == null) return -1;
        try {
            int p = Integer.parseInt(raw.trim());
            return p >= 1 && p <= 65535 ? p : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /** Round-trip time from a ping output line ("time=12.3 ms" / "time<1 ms"), or -1. */
    public static double parsePingTime(String out) {
        if (out == null) return -1;
        int i = out.indexOf("time=");
        if (i < 0) {
            return out.contains("time<") ? 0.5 : -1;
        }
        int j = i + 5;
        int k = j;
        while (k < out.length() && (Character.isDigit(out.charAt(k)) || out.charAt(k) == '.')) k++;
        try {
            double v = Double.parseDouble(out.substring(j, k));
            // Some emulators/NATs echo garbage round-trip times; anything over a minute is not a real reply.
            // A reply faster than 0.001 ms is also fake (emulated ICMP echoes 0.000).
            return v > 0.001 && v <= 60_000 ? v : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /** Address of the router that answered "Time to live exceeded", or null. */
    public static String parseHopAddress(String out) {
        if (out == null) return null;
        String lower = out.toLowerCase(Locale.ROOT);
        if (!lower.contains("time to live exceeded") && !lower.contains("ttl exceeded")
                && !lower.contains("ttl expired")) return null;
        int f = out.indexOf("From ");
        if (f < 0) f = out.indexOf("from ");
        if (f < 0) return null;
        int s = f + 5;
        int e = s;
        while (e < out.length() && !Character.isWhitespace(out.charAt(e)) && out.charAt(e) != ':'
                && out.charAt(e) != '(') e++;
        String ip = out.substring(s, e).trim();
        return ip.isEmpty() ? null : ip;
    }

    /** True when the ping reached the destination (a normal echo reply). */
    public static boolean isEchoReply(String out) {
        return out != null && out.contains("bytes from") && out.contains("icmp_seq") && parsePingTime(out) >= 0
                && !out.toLowerCase(Locale.ROOT).contains("exceeded");
    }

    public static List<String> commonPorts() {
        ArrayList<String> l = new ArrayList<>();
        for (int p : new int[]{80, 443, 22, 53, 25, 3389}) l.add(String.valueOf(p));
        return l;
    }

    /** Well-known service name for a port, or "" when unknown. */
    public static String serviceName(int port) {
        switch (port) {
            case 21: return "FTP";
            case 22: return "SSH";
            case 23: return "Telnet";
            case 25: return "SMTP";
            case 53: return "DNS";
            case 80: return "HTTP";
            case 110: return "POP3";
            case 143: return "IMAP";
            case 443: return "HTTPS";
            case 445: return "SMB";
            case 993: return "IMAPS";
            case 3306: return "MySQL";
            case 3389: return "RDP";
            case 5432: return "PostgreSQL";
            case 8080: return "HTTP-alt";
            default: return "";
        }
    }

    /** Destroys a child process (system ping) that is still running after the given time. */
    public static void killAfter(final Process p, final long ms) {
        Thread t = new Thread(() -> {
            try {
                Thread.sleep(ms);
                p.destroy();
            } catch (InterruptedException ignored) {
            }
        }, "proc-watchdog");
        t.setDaemon(true);
        t.start();
    }
}
