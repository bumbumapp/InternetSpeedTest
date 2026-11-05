package com.code.bumbumapps.netspeed;

import static org.junit.Assert.assertEquals;

import com.code.bumbumapps.netspeed.tools.HistoryStats;
import com.code.bumbumapps.netspeed.tools.LatencyStats;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class ToolsLogicTest {
    @Test
    public void latencyStatsBasics() {
        LatencyStats s = new LatencyStats(10);
        s.add(10); s.add(20); s.add(-1); s.add(30);
        assertEquals(10, s.min(), 1e-9);
        assertEquals(30, s.max(), 1e-9);
        assertEquals(20, s.avg(), 1e-9);
        assertEquals(10, s.jitter(), 1e-9);
        assertEquals(25, s.lossPercent(), 1e-9);
        assertEquals("poor", s.quality());
    }

    @Test
    public void latencyWindowAndEmpty() {
        LatencyStats s = new LatencyStats(2);
        assertEquals(-1, s.avg(), 1e-9);
        assertEquals("poor", s.quality());
        s.add(100); s.add(200); s.add(300);
        assertEquals(2, s.samples().size());
        assertEquals(250, s.avg(), 1e-9);
        assertEquals(3, s.sent());
    }

    @Test
    public void parsesSpeedAndPing() {
        assertEquals(40, HistoryStats.parseMbps("5 MB/s"), 1e-9);
        assertEquals(12.5, HistoryStats.parseMbps("12.5 Mbps"), 1e-9);
        assertEquals(-1, HistoryStats.parseMbps("— Mbps"), 1e-9);
        assertEquals(12.5, HistoryStats.parseMs("12.5 ms"), 1e-9);
        assertEquals(-1, HistoryStats.parseMs("— ms"), 1e-9);
    }

    @Test
    public void aggregatesHistory() {
        List<String> type = Arrays.asList("WIFI", "MOBILE", "WIFI");
        List<String> ping = Arrays.asList("10 ms", "30 ms", "— ms");
        List<String> down = Arrays.asList("50 Mbps", "10 Mbps", "0 Mbps");
        List<String> up = Arrays.asList("5 Mbps", "1 Mbps", "0 Mbps");
        HistoryStats h = HistoryStats.of(type, ping, down, up);
        assertEquals(3, h.count);
        assertEquals(2, h.wifiCount);
        assertEquals(1, h.mobileCount);
        assertEquals(50, h.bestDownload, 1e-9);
        assertEquals(10, h.worstDownload, 1e-9);
        assertEquals(30, h.avgDownload, 1e-9);
        assertEquals(10, h.bestPing, 1e-9);
        assertEquals(20, h.avgPing, 1e-9);
    }

    @Test
    public void csvEscapes() {
        assertEquals("\"a,\"\"b\"\"\"", HistoryStats.csv("a,\"b\""));
        assertEquals("time,network,ping,download,upload\n09:00 AM,WIFI,10 ms,5 Mbps,1 Mbps\n",
                HistoryStats.toCsv(Arrays.asList("09:00 AM"), Arrays.asList("WIFI"),
                        Arrays.asList("10 ms"), Arrays.asList("5 Mbps"), Arrays.asList("1 Mbps")));
    }
}
