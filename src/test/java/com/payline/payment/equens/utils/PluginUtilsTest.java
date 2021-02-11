package com.payline.payment.equens.utils;

import com.payline.payment.equens.MockUtils;
import com.payline.payment.equens.bean.business.reachdirectory.Aspsp;
import com.payline.payment.equens.bean.business.reachdirectory.GetAspspsResponse;
import com.payline.payment.equens.exception.InvalidDataException;
import com.payline.payment.equens.exception.PluginException;
import com.payline.payment.equens.service.JsonService;
import com.payline.payment.equens.service.impl.ConfigurationServiceImpl;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PluginUtilsTest {
    private JsonService jsonService = JsonService.getInstance();

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
    void isNumeric(){
        Assertions.assertFalse(PluginUtils.isNumeric(null));
        Assertions.assertFalse(PluginUtils.isNumeric(""));
        Assertions.assertTrue(PluginUtils.isNumeric("1"));
        Assertions.assertTrue(PluginUtils.isNumeric("123456789012"));
    }


    @Test
    void getAspspIdFromBIC() {
        String aspspJson = "{\"Application\":\"PIS\",\"ASPSP\":[" +
                "{\"AspspId\":\"1234\",\"Name\":[\"a Bank\"],\"CountryCode\":\"FR\",\"Details\":[{\"Api\":\"POST /payments\",\"Fieldname\":\"PaymentProduct\",\"Value\":\"Normal|Instant\",\"ProtocolVersion\":\"STET_V_1_4_0_47\"}],\"BIC\":\"FOOBARBAZXX\"}," +
                "{\"AspspId\":\"4321\",\"Name\":[\"another Bank\"],\"CountryCode\":\"FR\",\"Details\":[{\"Api\":\"POST /payments\",\"Fieldname\":\"PaymentProduct\",\"ProtocolVersion\":\"STET_V_1_4_0_47\"}],\"BIC\":\"FOOBARBAZZZ\"}," +
                "{\"AspspId\":\"1409\",\"Name\":[\"La Banque Postale\"],\"CountryCode\":\"FR\",\"Details\":[{\"Api\":\"POST /payments\",\"Fieldname\":\"PaymentProduct\",\"Value\":\"Normal\",\"ProtocolVersion\":\"STET_V_1_4_0_47\"}],\"BIC\":\"PSSTFRPP\"}," +
                "{\"AspspId\":\"1601\",\"Name\":[\"BBVA\"],\"CountryCode\":\"ES\",\"Details\":[{\"Api\":\"POST /payments\",\"Fieldname\":\"PaymentProduct\",\"Value\":\"Instant\",\"ProtocolVersion\":\"STET_V_1_4_0_47\"}],\"BIC\":\"BBVAESMM\"}" +
                "],\"MessageCreateDateTime\":\"2019-11-15T16:52:37.092+0100\",\"MessageId\":\"6f31954f-7ad6-4a63-950c-a2a363488e\"}";

        List<Aspsp> aspsps = jsonService.fromJson(aspspJson, GetAspspsResponse.class).getAspsps();

        Assertions.assertEquals("1234", PluginUtils.getAspspIdFromBIC(aspsps, "FOOBARBAZXX"));
        Assertions.assertEquals("1409", PluginUtils.getAspspIdFromBIC(aspsps, "PSSTFRPP"));
        Assertions.assertEquals("1409", PluginUtils.getAspspIdFromBIC(aspsps, "PSSTFRPPXXX"));
        Assertions.assertThrows(PluginException.class, () -> PluginUtils.getAspspIdFromBIC(aspsps, "ABADBIC8"));
        Assertions.assertThrows(PluginException.class, () -> PluginUtils.getAspspIdFromBIC(aspsps, "BADBIC11"));
        Assertions.assertThrows(PluginException.class, () -> PluginUtils.getAspspIdFromBIC(aspsps, "BADBIC7"));
        Assertions.assertThrows(PluginException.class, () -> PluginUtils.getAspspIdFromBIC(aspsps, null));
    }

    @Test
    void getCountryCodeFromBIC() {
        String aspspJson = "{\"Application\":\"PIS\",\"ASPSP\":[" +
                "{\"AspspId\":\"1234\",\"Name\":[\"a Bank\"],\"CountryCode\":\"UK\",\"Details\":[{\"Api\":\"POST /payments\",\"Fieldname\":\"PaymentProduct\",\"Value\":\"Normal|Instant\",\"ProtocolVersion\":\"STET_V_1_4_0_47\"}],\"BIC\":\"MOOBARBAZXX\"}," +
                "{\"AspspId\":\"4321\",\"Name\":[\"another Bank\"],\"CountryCode\":\"FR\",\"Details\":[{\"Api\":\"POST /payments\",\"Fieldname\":\"PaymentProduct\",\"ProtocolVersion\":\"STET_V_1_4_0_47\"}],\"BIC\":\"FOOBARBA\"}," +
                "{\"AspspId\":\"1409\",\"Name\":[\"La Banque Postale\"],\"CountryCode\":\"FR\",\"Details\":[{\"Api\":\"POST /payments\",\"Fieldname\":\"PaymentProduct\",\"Value\":\"Normal\",\"ProtocolVersion\":\"STET_V_1_4_0_47\"}],\"BIC\":\"PSSTFRPP\"}," +
                "{\"AspspId\":\"1601\",\"Name\":[\"BBVA\"],\"CountryCode\":\"ES\",\"Details\":[{\"Api\":\"POST /payments\",\"Fieldname\":\"PaymentProduct\",\"Value\":\"Instant\",\"ProtocolVersion\":\"STET_V_1_4_0_47\"}],\"BIC\":\"BBVAESMM\"}," +
                "{\"AspspId\":\"1602\",\"Name\":[\"Santander\"],\"CountryCode\":\"ES\",\"Details\":[{\"Api\":\"POST /payments\",\"Fieldname\":\"PaymentProduct\",\"Value\":\"Instant\",\"ProtocolVersion\":\"STET_V_1_4_0_47\"}],\"BIC\":\"ES140049\"}," +
                "{\"AspspId\":\"1603\",\"Name\":[\"Santander\"],\"CountryCode\":\"IT\",\"Details\":[{\"Api\":\"POST /payments\",\"Fieldname\":\"PaymentProduct\",\"Value\":\"Instant\",\"ProtocolVersion\":\"STET_V_1_4_0_47\"}],\"BIC\":\"IT14004\"}" +
                "],\"MessageCreateDateTime\":\"2019-11-15T16:52:37.092+0100\",\"MessageId\":\"6f31954f-7ad6-4a63-950c-a2a363488e\"}";

        List<Aspsp> aspsps = jsonService.fromJson(aspspJson, GetAspspsResponse.class).getAspsps();
        Assertions.assertEquals("FR", PluginUtils.getCountryCodeFromBIC(aspsps, "PSSTFRPP"));
        Assertions.assertEquals("ES", PluginUtils.getCountryCodeFromBIC(aspsps, "BBVAESMM"));
        Assertions.assertEquals("FR", PluginUtils.getCountryCodeFromBIC(aspsps, "FOOBARBA"));
        Assertions.assertEquals("UK", PluginUtils.getCountryCodeFromBIC(aspsps, "MOOBARBAZXX"));
        Assertions.assertEquals("ES", PluginUtils.getCountryCodeFromBIC(aspsps, "ES140049000"));

        Assertions.assertThrows(InvalidDataException.class,
                () -> PluginUtils.getCountryCodeFromBIC(aspsps, "IT14004"),
                "Can't find a country for this BIC IT14004"
        );

        Assertions.assertThrows(InvalidDataException.class,
                () -> PluginUtils.getCountryCodeFromBIC(aspsps, "ANINVALIDBIC"),
                "Can't find a country for this BIC ANINVALIDBIC"
        );
    }

    @Test
    void getCountryCodeFromBICWithSomeNullBIC() {
        String aspspJson = "{\"Application\":\"PIS\",\"ASPSP\":[" +
                "{\"AspspId\":\"1234\",\"Name\":[\"a Bank\"],\"CountryCode\":\"FR\",\"BIC\":\"FOOBARBA\"}," +
                "{\"AspspId\":\"1409\",\"Name\":[\"La Banque Postale\"],\"CountryCode\":\"FR\"}" +
                "],\"MessageCreateDateTime\":\"2019-11-15T16:52:37.092+0100\",\"MessageId\":\"6f31954f-7ad6-4a63-950c-a2a363488e\"}";

        List<Aspsp> aspsps = jsonService.fromJson(aspspJson, GetAspspsResponse.class).getAspsps();
        Assertions.assertEquals("FR", PluginUtils.getCountryCodeFromBIC(aspsps, "FOOBARBAZ"));
    }

    @Test
    void createListCountry_OneCountry() {
        List<String> expected = new ArrayList<String>() {
        };
        expected.add(ConfigurationServiceImpl.CountryCode.FR.name());

        List<String> listCountry = PluginUtils.createListCountry(ConfigurationServiceImpl.CountryCode.FR.name());

        Assertions.assertEquals(expected, listCountry);

    }

    @Test
    void createListCountry_TOUSCountry() {
        List<String> expected = new ArrayList<>();
        expected.add(ConfigurationServiceImpl.CountryCode.FR.name());
        expected.add(ConfigurationServiceImpl.CountryCode.ES.name());

        List<String> listCountry = PluginUtils.createListCountry(ConfigurationServiceImpl.CountryCode.ALL.name());

        Assertions.assertEquals(expected, listCountry);
    }

    @Test
    void createListCountry_NullCountry() {
        Assertions.assertThrows(InvalidDataException.class,
                () -> PluginUtils.createListCountry(""),
                "Country in ContractConfiguration should not be empty"
        );
    }

    @Test
    void correctIban() {
        String iban = "FR1234M6789";
        List<String> listCountry = new ArrayList<>();
        listCountry.add(ConfigurationServiceImpl.CountryCode.FR.name());

        Assertions.assertTrue(PluginUtils.correctIban(listCountry, iban));
    }

    @Test
    void correctIban_emptyListCountry() {
        String iban = MockUtils.getIbanFR();
        List<String> countries = new ArrayList<>();
        Assertions.assertThrows(InvalidDataException.class,
                () -> PluginUtils.correctIban(countries, iban),
                "listCountry should not be empty"
        );
    }

    @Test
    void hideIban() {
        String ibanWithSpaces = "FR51 3265 1245 41AZ 1325 4598 145";
        String ibanWithoutSpaces = "FR513265124541AZ13254598145";

        Assertions.assertEquals("FR51 XXXX XXXX XXXX XXXX XXX8 145", PluginUtils.hideIban(ibanWithSpaces));
        Assertions.assertEquals("FR51XXXXXXXXXXXXXXXXXXX8145", PluginUtils.hideIban(ibanWithoutSpaces));
    }
}
