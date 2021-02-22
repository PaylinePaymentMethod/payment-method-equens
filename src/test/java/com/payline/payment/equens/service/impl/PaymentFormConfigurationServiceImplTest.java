package com.payline.payment.equens.service.impl;

import com.payline.payment.equens.MockUtils;
import com.payline.payment.equens.bean.business.reachdirectory.Detail;
import com.payline.payment.equens.utils.i18n.I18nService;
import com.payline.pmapi.bean.common.FailureCause;
import com.payline.pmapi.bean.paymentform.bean.field.SelectOption;
import com.payline.pmapi.bean.paymentform.bean.form.AbstractPaymentForm;
import com.payline.pmapi.bean.paymentform.bean.form.BankTransferForm;
import com.payline.pmapi.bean.paymentform.request.PaymentFormConfigurationRequest;
import com.payline.pmapi.bean.paymentform.response.configuration.PaymentFormConfigurationResponse;
import com.payline.pmapi.bean.paymentform.response.configuration.impl.PaymentFormConfigurationResponseFailure;
import com.payline.pmapi.bean.paymentform.response.configuration.impl.PaymentFormConfigurationResponseSpecific;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;

class PaymentFormConfigurationServiceImplTest {

    @InjectMocks
    private PaymentFormConfigurationServiceImpl service;

    @Mock
    private I18nService i18n;

    private String aspspsJson = "{\"Application\":\"PIS\",\"ASPSP\":[" +
            "{\"AspspId\":\"1234\",\"Name\":[\"a Bank\"],\"CountryCode\":\"FR\",\"Details\":[{\"Api\":\"POST /payments\",\"FieldName\":\"PaymentProduct\",\"Value\":\"Normal|Instant\",\"ProtocolVersion\":\"STET_V_1_4_0_47\"}],\"BIC\":\"MOOBARBAZXX\"}," +
            "{\"AspspId\":\"4321\",\"Name\":[\"another Bank\"],\"CountryCode\":\"FR\",\"Details\":[{\"Api\":\"POST /payments\",\"FieldName\":\"PaymentProduct\",\"ProtocolVersion\":\"STET_V_1_4_0_47\"}],\"BIC\":\"FOOBARBA\"}," +
            "{\"AspspId\":\"1409\",\"Name\":[\"La Banque Postale\"],\"CountryCode\":\"FR\",\"Details\":[{\"Api\":\"POST /payments\",\"FieldName\":\"PaymentProduct\",\"Value\":\"Normal\",\"ProtocolVersion\":\"STET_V_1_4_0_47\"}],\"BIC\":\"PSSTFRPP\"}," +
            "{\"AspspId\":\"1601\",\"Name\":[\"BBVA\"],\"CountryCode\":\"ES\",\"Details\":[{\"Api\":\"POST /payments\",\"FieldName\":\"PaymentProduct\",\"Value\":\"Instant\",\"ProtocolVersion\":\"STET_V_1_4_0_47\"}],\"BIC\":\"BBVAESMM\"}," +
            "{\"AspspId\":\"1602\",\"Name\":[\"Santander\"],\"CountryCode\":\"ES\",\"Details\":[{\"Api\":\"POST /payments\",\"FieldName\":\"PaymentProduct\",\"Value\":\"Instant\",\"ProtocolVersion\":\"STET_V_1_4_0_47\"}],\"BIC\":\"ES140049\"}," +
            "{\"AspspId\":\"1603\",\"Name\":[\"Santander\"],\"CountryCode\":\"IT\",\"Details\":[{\"Api\":\"POST /payments\",\"FieldName\":\"PaymentProduct\",\"Value\":\"Instant\",\"ProtocolVersion\":\"STET_V_1_4_0_47\"}],\"BIC\":\"IT14004\"}," +
            "{\"AspspId\":\"224\",\"CountryCode\":\"DE\",\"Name\":[\"08/15direkt\"],\"Details\":[{\"Api\":\"POST /payments\",\"FieldName\":\"PaymentProduct\",\"Value\":\"Instant\",\"ProtocolVersion\":\"STET_V_1_4_0_47\"}]}" +
            "],\"MessageCreateDateTime\":\"2019-11-15T16:52:37.092+0100\",\"MessageId\":\"6f31954f-7ad6-4a63-950c-a2a363488e\"}";

    @BeforeEach
    void setup() {
        service = new PaymentFormConfigurationServiceImpl();
        MockitoAnnotations.initMocks(this);

        // We consider by default that i18n behaves normally
        doReturn("message")
                .when(i18n)
                .getMessage(anyString(), any(Locale.class));
    }

    @Test
    void getPaymentFormConfiguration_nominal() {
        // given: the plugin configuration contains 2 french banks and the locale is FRANCE
        PaymentFormConfigurationRequest request = MockUtils.aPaymentFormConfigurationRequestBuilder()
                .withLocale(Locale.FRANCE)
                .withPluginConfiguration(aspspsJson)
                .build();

        // when: calling getPaymentFormConfiguration method
        PaymentFormConfigurationResponse response = service.getPaymentFormConfiguration(request);

        // then: response is a success, the form is a BankTransferForm and the number of banks is correct
        assertEquals(PaymentFormConfigurationResponseSpecific.class, response.getClass());
        AbstractPaymentForm form = ((PaymentFormConfigurationResponseSpecific) response).getPaymentForm();
        assertNotNull(form.getButtonText());
        assertNotNull(form.getDescription());
        assertEquals(BankTransferForm.class, form.getClass());
        BankTransferForm bankTransferForm = (BankTransferForm) form;
        assertEquals(2, bankTransferForm.getBanks().size());
    }

    @Test
    void getPaymentFormConfiguration_invalidPluginConfiguration() {
        // given: the plugin configuration is invalid
        PaymentFormConfigurationRequest request = MockUtils.aPaymentFormConfigurationRequestBuilder()
                .withPluginConfiguration("{not valid")
                .build();

        // when: calling getPaymentFormConfiguration method
        PaymentFormConfigurationResponse response = service.getPaymentFormConfiguration(request);

        // then: response is a failure
        assertEquals(PaymentFormConfigurationResponseFailure.class, response.getClass());
        assertNotNull(((PaymentFormConfigurationResponseFailure) response).getErrorCode());
        assertNotNull(((PaymentFormConfigurationResponseFailure) response).getFailureCause());
    }

    @Test
    void getPaymentFormConfiguration_invalidCountry() {
        // given: the plugin configuration is invalid
        PaymentFormConfigurationRequest request = MockUtils.aPaymentFormConfigurationRequestBuilder()
                .withContractConfiguration(MockUtils.aContractConfiguration(null))
                .build();

        // when: calling getPaymentFormConfiguration method
        PaymentFormConfigurationResponse response = service.getPaymentFormConfiguration(request);

        // then: response is a failure
        assertEquals(PaymentFormConfigurationResponseFailure.class, response.getClass());
        assertNotNull(((PaymentFormConfigurationResponseFailure) response).getErrorCode());
        assertEquals("country must not be empty", ((PaymentFormConfigurationResponseFailure) response).getErrorCode());
        assertNotNull(((PaymentFormConfigurationResponseFailure) response).getFailureCause());
        assertEquals(FailureCause.INVALID_DATA, ((PaymentFormConfigurationResponseFailure) response).getFailureCause());
    }

    @Test
    void getBanks_aspspWithoutBic() {
        // @see https://payline.atlassian.net/browse/PAYLAPMEXT-204
        // @see https://payline.atlassian.net/browse/PAYLAPMEXT-219

        // when: calling getBanks method
        List<String> listCountry = new ArrayList<>();
        listCountry.add(Locale.GERMANY.getCountry());
        List<SelectOption> result = service.getBanks(aspspsJson, listCountry);

        // then: the aspsp is ignered because there is no BIC
        assertTrue(result.isEmpty());
    }

    @Test
    void getBanks_filterAspspByCountryCode() {
        // @see: https://payline.atlassian.net/browse/PAYLAPMEXT-203

        // when: calling getBanks method
        List<String> listCountry = new ArrayList<>();
        listCountry.add(Locale.FRANCE.getCountry());
        List<SelectOption> result = service.getBanks(aspspsJson, listCountry);

        // then: there is only 1 bank choice at the end
        assertEquals(2, result.size());
    }
    @Test
    void getBanks_withoutBic() {
        String json = "{\"Application\":\"PIS\",\"ASPSP\":[" +
                "{\"AspspId\":\"1234\",\"Name\":[\"a Bank\"],\"CountryCode\":\"FR\",\"Details\":[{\"Api\":\"POST /payments\",\"FieldName\":\"PaymentProduct\",\"Value\":\"Normal|Instant\",\"ProtocolVersion\":\"STET_V_1_4_0_47\"}],\"BIC\":\"MOOBARBAZXX\"}]}";

        // when: calling getBanks method
        List<String> listCountry = new ArrayList<>();
        listCountry.add(Locale.FRANCE.getCountry());
        List<SelectOption> result = service.getBanks(json, listCountry);

        // then: The Bic is not contained in the name of the bank
        assertEquals("a Bank",result.get(0).getValue());
        assertEquals("MOOBARBAZXX",result.get(0).getKey());
    }

    @Test
    void getBanks_filterAspspByMultipleCountryCode() {
        // @see: https://payline.atlassian.net/browse/PAYLAPMEXT-203

        // when: calling getBanks method
        List<String> listCountry = new ArrayList<>();
        listCountry.add(Locale.FRANCE.getCountry());
        listCountry.add("ES");
        List<SelectOption> result = service.getBanks(aspspsJson, listCountry);

        // then: there is 2 banks choice at the end
        assertEquals(4, result.size());
    }

    @Test
    void isCompatibleNormal(){
        List<Detail> details = new ArrayList<>();
        details.add(new Detail("API", "foo", "MANDATORY", null, "protocol1"));
        details.add(new Detail("API", "PaymentProduct", "SUPPORTED", "Normal", "protocol1"));
        details.add(new Detail("API", "foo2", "MANDATORY", null, "protocol1"));

        Assertions.assertFalse( service.isCompatible(details));
    }

    @Test
    void isCompatibleNormalAndInstant(){
        List<Detail> details = new ArrayList<>();
        details.add(new Detail("API", "foo", "MANDATORY", null, "protocol1"));
        details.add(new Detail("API", "PaymentProduct", "SUPPORTED", "Normal|Instant", "protocol1"));
        details.add(new Detail("API", "foo2", "MANDATORY", null, "protocol1"));

        Assertions.assertTrue( service.isCompatible(details));
    }

    @Test
    void isCompatibleInstant(){
        List<Detail> details = new ArrayList<>();
        details.add(new Detail("API", "foo", "MANDATORY", null, "protocol1"));
        details.add(new Detail("API", "PaymentProduct", "SUPPORTED", "Instant", "protocol1"));
        details.add(new Detail("API", "foo2", "MANDATORY", null, "protocol1"));

        Assertions.assertTrue( service.isCompatible(details));
    }

    @Test
    void isCompatibleNone(){
        List<Detail> details = new ArrayList<>();
        details.add(new Detail("API", "foo", "MANDATORY", null, "protocol1"));
        details.add(new Detail("API", "foo2", "MANDATORY", null, "protocol1"));

        Assertions.assertTrue( service.isCompatible(details));
    }

    @Test
    void isCompatibleNull(){
        Assertions.assertTrue( service.isCompatible(null));
    }
}
