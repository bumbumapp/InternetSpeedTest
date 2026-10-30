package com.code.bumbumapps.netspeed;

import static org.junit.Assert.assertEquals;

import java.text.DecimalFormat;

import org.junit.Test;

public class PingTextTest {
    private final DecimalFormat dec = new DecimalFormat("#.##");

    @Test
    public void realPingIsShown() {
        assertEquals("12.5 ms", MainActivity.pingText(dec, 12.5));
    }

    @Test
    public void failedOrGarbagePingShowsDash() {
        assertEquals("— ms", MainActivity.pingText(dec, 0));
        assertEquals("— ms", MainActivity.pingText(dec, 2.9475945332809E15));
        assertEquals("— ms", MainActivity.pingText(dec, Double.NaN));
    }
}
