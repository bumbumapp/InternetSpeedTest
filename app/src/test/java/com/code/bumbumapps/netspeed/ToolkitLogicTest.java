package com.code.bumbumapps.netspeed;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.code.bumbumapps.netspeed.tools.LanLogic;
import com.code.bumbumapps.netspeed.tools.NetInput;
import com.code.bumbumapps.netspeed.tools.UsageMath;
import com.code.bumbumapps.netspeed.tools.WifiLogic;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import org.junit.Test;

public class ToolkitLogicTest {
    // ---- Wi-Fi
    @Test
    public void channelsAndBands() {
        assertEquals(1, WifiLogic.channelFromFreq(2412));
        assertEquals(6, WifiLogic.channelFromFreq(2437));
        assertEquals(13, WifiLogic.channelFromFreq(2472));
        assertEquals(14, WifiLogic.channelFromFreq(2484));
        assertEquals(36, WifiLogic.channelFromFreq(5180));
        assertEquals(149, WifiLogic.channelFromFreq(5745));
        assertEquals(1, WifiLogic.channelFromFreq(5955));
        assertEquals(-1, WifiLogic.channelFromFreq(1234));
        assertEquals(24, WifiLogic.bandOf(2437));
        assertEquals(5, WifiLogic.bandOf(5180));
        assertEquals(6, WifiLogic.bandOf(5955));
        assertEquals(0, WifiLogic.bandOf(900));
    }

    @Test
    public void signalLevels() {
        assertEquals(4, WifiLogic.level(-40));
        assertEquals(3, WifiLogic.level(-60));
        assertEquals(2, WifiLogic.level(-70));
        assertEquals(1, WifiLogic.level(-80));
        assertEquals(0, WifiLogic.level(-95));
        assertEquals(100, WifiLogic.percent(-30));
        assertEquals(0, WifiLogic.percent(-120));
        assertEquals(60, WifiLogic.percent(-70));
    }

    @Test
    public void securityLabels() {
        assertEquals("WPA3", WifiLogic.security("[WPA2-PSK-CCMP][RSN-SAE-CCMP][ESS]"));
        assertEquals("WPA2", WifiLogic.security("[WPA2-PSK-CCMP][ESS]"));
        assertEquals("WPA", WifiLogic.security("[WPA-PSK-TKIP][ESS]"));
        assertEquals("WEP", WifiLogic.security("[WEP][ESS]"));
        assertEquals("Open", WifiLogic.security("[ESS]"));
        assertEquals("Open", WifiLogic.security(null));
    }

    @Test
    public void bestChannelAvoidsCrowdedOnes() {
        List<WifiLogic.Ap> aps = Arrays.asList(
                new WifiLogic.Ap("a", 2437, -45, "WPA2"),   // ch 6 strong
                new WifiLogic.Ap("b", 2437, -50, "WPA2"),
                new WifiLogic.Ap("c", 2412, -60, "WPA2"),   // ch 1
                new WifiLogic.Ap("d", 5180, -50, "WPA2"));  // 5 GHz, ignored for 2.4
        assertEquals(11, WifiLogic.bestChannel(aps, 24));
        assertEquals(2, WifiLogic.countOnChannel(aps, 24, 6));
        // 5 GHz: channel 36 is taken, so another one is suggested
        int best5 = WifiLogic.bestChannel(aps, 5);
        assertTrue(best5 != 36 && best5 > 0);
    }

    @Test
    public void bestChannelEmptyScan() {
        assertEquals(1, WifiLogic.bestChannel(Arrays.<WifiLogic.Ap>asList(), 24));
        assertEquals(-1, WifiLogic.bestChannel(Arrays.<WifiLogic.Ap>asList(), 6));
    }

    // ---- LAN
    @Test
    public void subnetHosts() {
        List<String> h = LanLogic.hosts("192.168.1.42", 24);
        assertEquals(254, h.size());
        assertEquals("192.168.1.1", h.get(0));
        assertEquals("192.168.1.254", h.get(253));
        // bigger networks are limited to the /24 around the device
        assertEquals(254, LanLogic.hosts("10.0.5.9", 16).size());
        assertEquals("10.0.5.1", LanLogic.hosts("10.0.5.9", 16).get(0));
        // smaller networks keep their own range
        List<String> s = LanLogic.hosts("192.168.1.70", 26);
        assertEquals(62, s.size());
        assertEquals("192.168.1.65", s.get(0));
        assertTrue(s.size() > 0);
        assertEquals(0, LanLogic.hosts("garbage", 24).size());
        assertEquals(0, LanLogic.hosts("192.168.1.1", 31).size());
    }

    @Test
    public void ipConversions() {
        assertEquals("1.2.3.4", LanLogic.toIp(LanLogic.toInt("1.2.3.4")));
        assertEquals("255.255.255.255", LanLogic.toIp(LanLogic.toInt("255.255.255.255")));
        assertEquals(Integer.MIN_VALUE, LanLogic.toInt("300.1.1.1"));
        assertEquals(Integer.MIN_VALUE, LanLogic.toInt("1.2.3"));
        assertTrue(LanLogic.sortKey("192.168.1.9") < LanLogic.sortKey("192.168.1.10"));
        assertTrue(LanLogic.sortKey("10.0.0.1") < LanLogic.sortKey("192.168.0.1"));
        assertEquals("192.168.1.0/24", LanLogic.subnetLabel("192.168.1.42", 24));
    }

    // ---- Toolbox input
    @Test
    public void hostCleaning() {
        assertEquals("example.com", NetInput.cleanHost("  https://Example.com/path?a=b "));
        assertEquals("8.8.8.8", NetInput.cleanHost("8.8.8.8"));
        assertEquals("example.com", NetInput.cleanHost("example.com:8080"));
        assertEquals("example.com", NetInput.cleanHost("user@example.com"));
        assertEquals("2001:db8::1", NetInput.cleanHost("[2001:db8::1]:443"));
        assertNull(NetInput.cleanHost(""));
        assertNull(NetInput.cleanHost(null));
        assertNull(NetInput.cleanHost("bad host"));
        assertNull(NetInput.cleanHost("a;rm -rf"));
        assertNull(NetInput.cleanHost("-oProxy"));
        assertNull(NetInput.cleanHost("exa$mple.com"));
    }

    @Test
    public void portParsing() {
        assertEquals(443, NetInput.parsePort(" 443 "));
        assertEquals(-1, NetInput.parsePort("0"));
        assertEquals(-1, NetInput.parsePort("70000"));
        assertEquals(-1, NetInput.parsePort("abc"));
        assertEquals(-1, NetInput.parsePort(null));
        assertEquals("HTTPS", NetInput.serviceName(443));
        assertEquals("", NetInput.serviceName(12345));
    }

    @Test
    public void pingOutputParsing() {
        String reply = "PING 8.8.8.8 (8.8.8.8) 56(84) bytes of data.\n64 bytes from 8.8.8.8: icmp_seq=1 ttl=117 time=14.2 ms\n";
        assertEquals(14.2, NetInput.parsePingTime(reply), 1e-9);
        assertTrue(NetInput.isEchoReply(reply));
        assertNull(NetInput.parseHopAddress(reply));
        String hop = "PING 8.8.8.8 (8.8.8.8) 56(84) bytes of data.\nFrom 192.168.1.1: icmp_seq=1 Time to live exceeded\n";
        assertEquals("192.168.1.1", NetInput.parseHopAddress(hop));
        assertFalse(NetInput.isEchoReply(hop));
        assertEquals(-1, NetInput.parsePingTime(hop), 1e-9);
        String hop2 = "From 10.20.30.1 icmp_seq=1 Time to live exceeded";
        assertEquals("10.20.30.1", NetInput.parseHopAddress(hop2));
        assertNull(NetInput.parseHopAddress("100% packet loss"));
        assertFalse(NetInput.isEchoReply(""));
        assertFalse(NetInput.isEchoReply(null));
        // garbage time from an emulated network must not be shown as a result
        String junk = "64 bytes from 8.8.8.8: icmp_seq=0 ttl=255 time=4265289363275805 ms";
        assertEquals(-1, NetInput.parsePingTime(junk), 1e-9);
        assertFalse(NetInput.isEchoReply(junk));
        assertEquals(-1, NetInput.parsePingTime("64 bytes from 8.8.8.8: icmp_seq=0 ttl=255 time=0.000 ms"), 1e-9);
        assertEquals(0.5, NetInput.parsePingTime("64 bytes from x: icmp_seq=1 ttl=64 time<1 ms"), 1e-9);
    }

    // ---- Usage
    @Test
    public void byteFormatting() {
        assertEquals("0 B", UsageMath.bytes(-5));
        assertEquals("512 B", UsageMath.bytes(512));
        assertEquals("1.00 KB", UsageMath.bytes(1024));
        assertEquals("1.50 MB", UsageMath.bytes(1572864));
        assertEquals("4.82 GB", UsageMath.bytes((long) (4.82 * 1024 * 1024 * 1024)));
        assertEquals("12.0 GB", UsageMath.bytes(12L * 1024 * 1024 * 1024));
    }

    @Test
    public void usageIntervals() {
        Calendar c = Calendar.getInstance();
        c.set(2026, Calendar.NOVEMBER, 16, 14, 30, 0);
        long now = c.getTimeInMillis();
        assertEquals(7, UsageMath.intervals(UsageMath.WEEK, now).size());
        assertEquals(16, UsageMath.intervals(UsageMath.MONTH, now).size());
        List<long[]> today = UsageMath.intervals(UsageMath.TODAY, now);
        assertEquals(15, today.size());
        long[] last = today.get(today.size() - 1);
        assertEquals(now, last[1]);
        for (long[] iv : UsageMath.intervals(UsageMath.WEEK, now)) assertTrue(iv[0] < iv[1]);
    }
}
