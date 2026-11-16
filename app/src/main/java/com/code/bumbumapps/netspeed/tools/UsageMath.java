package com.code.bumbumapps.netspeed.tools;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/** Time-range and formatting helpers for the data usage monitor (pure, unit tested). */
public final class UsageMath {
    private UsageMath() {}

    public static final int TODAY = 0, WEEK = 1, MONTH = 2;

    /** Interval boundaries {start,end} pairs covering the selected range, oldest first. */
    public static List<long[]> intervals(int range, long nowMs) {
        ArrayList<long[]> out = new ArrayList<>();
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(nowMs);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        if (range == TODAY) {
            for (int h = 0; h <= c.getActualMaximum(Calendar.HOUR_OF_DAY) && h < 24; h++) {
                Calendar s = (Calendar) c.clone();
                s.set(Calendar.HOUR_OF_DAY, h);
                Calendar e = (Calendar) s.clone();
                e.add(Calendar.HOUR_OF_DAY, 1);
                if (s.getTimeInMillis() > nowMs) break;
                out.add(new long[]{s.getTimeInMillis(), Math.min(e.getTimeInMillis(), nowMs)});
            }
            return out;
        }
        int days;
        Calendar first = (Calendar) c.clone();
        if (range == WEEK) {
            days = 7;
            first.add(Calendar.DAY_OF_YEAR, -6);
        } else {
            first.set(Calendar.DAY_OF_MONTH, 1);
            days = c.get(Calendar.DAY_OF_MONTH);
        }
        for (int i = 0; i < days; i++) {
            Calendar s = (Calendar) first.clone();
            s.add(Calendar.DAY_OF_YEAR, i);
            Calendar e = (Calendar) s.clone();
            e.add(Calendar.DAY_OF_YEAR, 1);
            out.add(new long[]{s.getTimeInMillis(), Math.min(e.getTimeInMillis(), nowMs)});
        }
        return out;
    }

    public static String bytes(long b) {
        if (b < 0) b = 0;
        if (b < 1024) return b + " B";
        double v = b / 1024.0;
        String[] u = {"KB", "MB", "GB", "TB"};
        int i = 0;
        while (v >= 1024 && i < u.length - 1) {
            v /= 1024;
            i++;
        }
        return String.format(Locale.US, v >= 100 ? "%.0f %s" : v >= 10 ? "%.1f %s" : "%.2f %s", v, u[i]);
    }
}
