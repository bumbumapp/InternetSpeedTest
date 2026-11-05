package com.code.bumbumapps.netspeed.tools;

import java.util.ArrayList;
import java.util.List;

/** Rolling statistics for the latency monitor. A failed probe is recorded as a negative value. */
public class LatencyStats {
    private final int window;
    private final List<Double> samples = new ArrayList<>();
    private int total;
    private int lost;

    public LatencyStats(int window) {
        this.window = Math.max(1, window);
    }

    /** Add one probe: RTT in ms, or a negative number when the probe failed. */
    public synchronized void add(double rttMs) {
        total++;
        if (rttMs < 0 || Double.isNaN(rttMs)) {
            lost++;
            samples.add(-1d);
        } else {
            samples.add(rttMs);
        }
        while (samples.size() > window) samples.remove(0);
    }

    public synchronized void reset() {
        samples.clear();
        total = 0;
        lost = 0;
    }

    public synchronized List<Double> samples() {
        return new ArrayList<>(samples);
    }

    public synchronized int sent() { return total; }

    public synchronized int lostCount() { return lost; }

    /** Packet loss in percent (0..100) over all probes since the last reset. */
    public synchronized double lossPercent() {
        return total == 0 ? 0 : lost * 100.0 / total;
    }

    private List<Double> ok() {
        List<Double> r = new ArrayList<>();
        for (double d : samples) if (d >= 0) r.add(d);
        return r;
    }

    /** Minimum of successful probes in the window, or -1 when none. */
    public synchronized double min() {
        double m = -1;
        for (double d : ok()) if (m < 0 || d < m) m = d;
        return m;
    }

    public synchronized double max() {
        double m = -1;
        for (double d : ok()) if (d > m) m = d;
        return m;
    }

    public synchronized double avg() {
        List<Double> r = ok();
        if (r.isEmpty()) return -1;
        double s = 0;
        for (double d : r) s += d;
        return s / r.size();
    }

    /** Jitter = mean absolute difference between consecutive successful probes (RFC 3550 style). */
    public synchronized double jitter() {
        List<Double> r = ok();
        if (r.size() < 2) return -1;
        double s = 0;
        for (int i = 1; i < r.size(); i++) s += Math.abs(r.get(i) - r.get(i - 1));
        return s / (r.size() - 1);
    }

    /** Rough verdict for gaming/calls from the average latency, jitter and loss. */
    public synchronized String quality() {
        double a = avg();
        if (a < 0) return "poor";
        double j = Math.max(0, jitter());
        double loss = lossPercent();
        if (loss > 5 || a > 150 || j > 50) return "poor";
        if (loss > 1 || a > 80 || j > 25) return "fair";
        return "good";
    }
}
