package com.payline.payment.equens.utils;

import com.payline.payment.equens.bean.business.reachdirectory.Aspsp;
import com.payline.payment.equens.bean.business.reachdirectory.GetAspspsResponse;
import com.payline.payment.equens.exception.PluginException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PluginUtilsTest {

    @Test
    void requestToString_get() {
        // given: a HTTP request with headers
        HttpGet request = new HttpGet("http://domain.test.fr/endpoint");
        request.setHeader("Authorization", "Basic sensitiveStringThatShouldNotAppear");
        request.setHeader("Other", "This is safe to display");

        // when: converting the request to String for display
        String result = PluginUtils.requestToString(request);

        // then: the result is as expected
        String ln = System.lineSeparator();
        String expected = "GET http://domain.test.fr/endpoint" + ln
                + "Authorization: Basic *****" + ln
                + "Other: This is safe to display";
        assertEquals(expected, result);
    }

    @Test
    void requestToString_post() {
        // given: a HTTP request with headers
        HttpPost request = new HttpPost("http://domain.test.fr/endpoint");
        request.setHeader("SomeHeader", "Header value");
        request.setEntity(new StringEntity("{\"name\":\"Jean Martin\",\"age\":\"28\"}", StandardCharsets.UTF_8));

        // when: converting the request to String for display
        String result = PluginUtils.requestToString(request);

        // then: the result is as expected
        String ln = System.lineSeparator();
        String expected = "POST http://domain.test.fr/endpoint" + ln
                + "SomeHeader: Header value" + ln
                + "{\"name\":\"Jean Martin\",\"age\":\"28\"}";
        assertEquals(expected, result);
    }

    @Test
    void truncate() {
        assertEquals("0123456789", PluginUtils.truncate("01234567890123456789", 10));
        assertEquals("01234567890123456789", PluginUtils.truncate("01234567890123456789", 60));
        assertEquals("", PluginUtils.truncate("", 30));
        assertNull(PluginUtils.truncate(null, 30));
    }


    @Test
    void getAspspIdFromBIC() {
        String aspspJson = "{\"Application\":\"PIS\",\"ASPSP\":[" +
                "{\"AspspId\":\"1234\",\"Name\":[\"a Bank\"],\"CountryCode\":\"FR\",\"BIC\":\"FOOBARBAZXX\"}," +
                "{\"AspspId\":\"1409\",\"Name\":[\"La Banque Postale\"],\"CountryCode\":\"FR\",\"BIC\":\"PSSTFRPP\"}," +
                "{\"AspspId\":\"1601\",\"Name\":[\"BBVA\"],\"CountryCode\":\"ES\",\"BIC\":\"BBVAESMM\"}" +
                "],\"MessageCreateDateTime\":\"2019-11-15T16:52:37.092+0100\",\"MessageId\":\"6f31954f-7ad6-4a63-950c-a2a363488e\"}";

        List<Aspsp> aspsps = GetAspspsResponse.fromJson(aspspJson).getAspsps();

        Assertions.assertEquals("1234", PluginUtils.getAspspIdFromBIC(aspsps, "FOOBARBAZXX"));
        Assertions.assertEquals("1409", PluginUtils.getAspspIdFromBIC(aspsps, "PSSTFRPP"));
        Assertions.assertEquals("1409", PluginUtils.getAspspIdFromBIC(aspsps, "PSSTFRPPXXX"));
        Assertions.assertThrows(PluginException.class, () -> PluginUtils.getAspspIdFromBIC(aspsps, "ABADBIC8"));
        Assertions.assertThrows(PluginException.class, () -> PluginUtils.getAspspIdFromBIC(aspsps, "BADBIC11"));
        Assertions.assertThrows(PluginException.class, () -> PluginUtils.getAspspIdFromBIC(aspsps, "BADBIC7"));
        Assertions.assertThrows(PluginException.class, () -> PluginUtils.getAspspIdFromBIC(aspsps, null));
    }

}
