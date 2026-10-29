package com.code.bumbumapps.netspeed;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class GetSpeedTestHostsHandlerTest {
    @Test
    public void decodesXmlEntitiesInServerNames() {
        assertEquals("H&B Communications", GetSpeedTestHostsHandler.unescapeXml("H&amp;B Communications"));
        assertEquals("O'Brien \"Net\"", GetSpeedTestHostsHandler.unescapeXml("O&apos;Brien &quot;Net&quot;"));
        assertEquals("Plain", GetSpeedTestHostsHandler.unescapeXml("Plain"));
    }
}
