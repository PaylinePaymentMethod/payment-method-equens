package com.payline.payment.equens.service.impl;

import com.google.gson.Gson;
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

    private final String aspspsJson = "{\"Application\":\"PIS\",\"ASPSP\":[" +
            // FR - Normal|Instant
            "{\"AspspId\":\"1234\",\"Name\":[\"a Bank\"],\"CountryCode\":\"FR\",\"Details\":[{\"Api\":\"POST /payments\",\"Fieldname\":\"PaymentProduct\",\"Type\":\"SUPPORTED\",\"Value\":\"Normal|Instant\",\"ProtocolVersion\":\"STET_V_1_4_0_47\"}],\"BIC\":\"MOOBARBAZXX\"}," +
            // FR - Normal|Instant
            "{\"AspspId\":\"4321\",\"Name\":[\"another Bank\"],\"CountryCode\":\"FR\",\"Details\":[{\"Api\":\"POST /payments\",\"Fieldname\":\"PaymentProduct\",\"Type\":\"SUPPORTED\",\"Value\":\"Normal|Instant\",\"ProtocolVersion\":\"STET_V_1_4_0_47\"}],\"BIC\":\"FOOBARBA\"}," +
            // FR - Normal
            "{\"AspspId\":\"1409\",\"Name\":[\"La Banque Postale\"],\"CountryCode\":\"FR\",\"Details\":[{\"Api\":\"POST /payments\",\"Fieldname\":\"PaymentProduct\",\"Type\":\"SUPPORTED\",\"Value\":\"Normal\",\"ProtocolVersion\":\"STET_V_1_4_0_47\"}],\"BIC\":\"PSSTFRPP\"}," +
            // ES - Instant
            "{\"AspspId\":\"1601\",\"Name\":[\"BBVA\"],\"CountryCode\":\"ES\",\"Details\":[{\"Api\":\"POST /payments\",\"Fieldname\":\"PaymentProduct\",\"Type\":\"SUPPORTED\",\"Value\":\"Instant\",\"ProtocolVersion\":\"STET_V_1_4_0_47\"}],\"BIC\":\"BBVAESMM\"}," +
            // ES - Instant
            "{\"AspspId\":\"1602\",\"Name\":[\"Santander\"],\"CountryCode\":\"ES\",\"Details\":[{\"Api\":\"POST /payments\",\"Fieldname\":\"PaymentProduct\",\"Type\":\"SUPPORTED\",\"Value\":\"Instant\",\"ProtocolVersion\":\"STET_V_1_4_0_47\"}],\"BIC\":\"ES140049\"}," +
            // IT - Instant
            "{\"AspspId\":\"1603\",\"Name\":[\"Santander\"],\"CountryCode\":\"IT\",\"Details\":[{\"Api\":\"POST /payments\",\"Fieldname\":\"PaymentProduct\",\"Type\":\"SUPPORTED\",\"Value\":\"Instant\",\"ProtocolVersion\":\"STET_V_1_4_0_47\"}],\"BIC\":\"IT14004\"}," +
            // DE - Instant
            "{\"AspspId\":\"224\",\"CountryCode\":\"DE\",\"Name\":[\"08/15direkt\"],\"Details\":[{\"Api\":\"POST /payments\",\"Fieldname\":\"PaymentProduct\",\"Type\":\"SUPPORTED\",\"Value\":\"Instant\",\"ProtocolVersion\":\"STET_V_1_4_0_47\"}]}" +
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
        // given: the plugin configuration contains 3 french banks and the locale is FRANCE
        final PaymentFormConfigurationRequest request = MockUtils.aPaymentFormConfigurationRequestBuilder()
                                                                 .withLocale(Locale.FRANCE)
                                                                 .withPluginConfiguration(aspspsJson)
                                                                 .build();

        // when: calling getPaymentFormConfiguration method
        final PaymentFormConfigurationResponse response = service.getPaymentFormConfiguration(request);

        // then: response is a success, the form is a BankTransferForm and the number of banks is correct
        assertEquals(PaymentFormConfigurationResponseSpecific.class, response.getClass());
        final AbstractPaymentForm form = ((PaymentFormConfigurationResponseSpecific) response).getPaymentForm();
        assertNotNull(form.getButtonText());
        assertNotNull(form.getDescription());
        assertEquals(BankTransferForm.class, form.getClass());
        final BankTransferForm bankTransferForm = (BankTransferForm) form;
        assertEquals(2, bankTransferForm.getBanks().size());
    }

    @Test
    void getPaymentFormConfiguration_invalidPluginConfiguration() {
        // given: the plugin configuration is invalid
        final PaymentFormConfigurationRequest request = MockUtils.aPaymentFormConfigurationRequestBuilder()
                                                                 .withPluginConfiguration("{not valid")
                                                                 .build();

        // when: calling getPaymentFormConfiguration method
        final PaymentFormConfigurationResponse response = service.getPaymentFormConfiguration(request);

        // then: response is a failure
        assertEquals(PaymentFormConfigurationResponseFailure.class, response.getClass());
        assertNotNull(((PaymentFormConfigurationResponseFailure) response).getErrorCode());
        assertNotNull(((PaymentFormConfigurationResponseFailure) response).getFailureCause());
    }

    @Test
    void getPaymentFormConfiguration_invalidCountry() {
        // given: the plugin configuration is invalid
        final PaymentFormConfigurationRequest request = MockUtils.aPaymentFormConfigurationRequestBuilder()
                                                                 .withContractConfiguration(MockUtils.aContractConfiguration(null))
                                                                 .build();

        // when: calling getPaymentFormConfiguration method
        final PaymentFormConfigurationResponse response = service.getPaymentFormConfiguration(request);

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
        final List<String> listCountry = new ArrayList<>();
        listCountry.add(Locale.GERMANY.getCountry());
        final List<SelectOption> result = service.getBanks(aspspsJson, listCountry, ConfigurationServiceImpl.PaymentProduct.INSTANT.getPaymentProduct());

        // then: the aspsp is ignered because there is no BIC
        assertTrue(result.isEmpty());
    }

    @Test
    void getBanks_filterAspspByCountryCode() {
        // @see: https://payline.atlassian.net/browse/PAYLAPMEXT-203

        // when: calling getBanks method
        final List<String> listCountry = new ArrayList<>();
        listCountry.add(Locale.FRANCE.getCountry());
        final List<SelectOption> result = service.getBanks(aspspsJson, listCountry, ConfigurationServiceImpl.PaymentProduct.INSTANT.getPaymentProduct());

        // then: there is only 1 bank choice at the end
        assertEquals(2, result.size());
    }

    @Test
    void getBanks_filterAspspByMultipleCountryCode() {
        // @see: https://payline.atlassian.net/browse/PAYLAPMEXT-203

        // when: calling getBanks method
        final List<String> listCountry = new ArrayList<>();
        listCountry.add(Locale.FRANCE.getCountry());
        listCountry.add("ES");
        final List<SelectOption> result = service.getBanks(aspspsJson, listCountry,
                ConfigurationServiceImpl.PaymentProduct.INSTANT.getPaymentProduct());

        // then: there is 2 banks choice at the end
        assertEquals(4, result.size());
    }

    @Test
    void isCompatibleNormalWithNullDetail() {
        Assertions.assertTrue(service.isCompatibleBank(null,
                ConfigurationServiceImpl.PaymentProduct.NORMAL.getPaymentProduct()));
    }

    @Test
    void shouldBeCompatibleNormalWithOneDetailsWithValueNormal() {
        final List<Detail> details = new ArrayList<>();
        details.add(detailWithPostPaymentNormal());

        Assertions.assertTrue(service.isCompatibleBank(details,
                ConfigurationServiceImpl.PaymentProduct.NORMAL.getPaymentProduct()));
    }

    @Test
    void shouldBeCompatibleNormalWithOneDetailsWithValueNormalAndInstant() {
        final List<Detail> details = new ArrayList<>();
        details.add(detailWithPostPaymentInstantAndNormal());

        Assertions.assertTrue(service.isCompatibleBank(details,
                ConfigurationServiceImpl.PaymentProduct.NORMAL.getPaymentProduct()));
    }

    @Test
    void shouldNotBeCompatibleNormalWithOneDetailsWithValueInstant() {
        final List<Detail> details = new ArrayList<>();
        details.add(detailWithPostPaymentInstant());

        Assertions.assertFalse(service.isCompatibleBank(details,
                ConfigurationServiceImpl.PaymentProduct.NORMAL.getPaymentProduct()));
    }

    @Test
    void shouldNotBeCompatibleInstantWithNullDetail() {
        Assertions.assertFalse(service.isCompatibleBank(null,
                ConfigurationServiceImpl.PaymentProduct.INSTANT.getPaymentProduct()));
    }

    @Test
    void shouldBeCompatibleInstantWithOneDetailsWithValueNormalAndInstant() {
        final List<Detail> details = new ArrayList<>();
        details.add(detailWithPostPaymentInstantAndNormal());

        Assertions.assertTrue(service.isCompatibleBank(details,
                ConfigurationServiceImpl.PaymentProduct.INSTANT.getPaymentProduct()));
    }

    @Test
    void shouldBeCompatibleInstantWithOneDetailsWithValueInstant() {
        final List<Detail> details = new ArrayList<>();
        details.add(detailWithPostPaymentInstant());

        Assertions.assertTrue(service.isCompatibleBank(details,
                ConfigurationServiceImpl.PaymentProduct.INSTANT.getPaymentProduct()));
    }

    @Test
    void shouldNotBeCompatibleWithTwoDetailsWithValuesButNoInstant() {
        final List<Detail> details = new ArrayList<>();
        details.add(detailWithPostPaymentRandomValue());
        details.add(detailWithPostPaymentRandomValue());

        Assertions.assertFalse(service.isCompatibleBank(details,
                ConfigurationServiceImpl.PaymentProduct.INSTANT.getPaymentProduct()));
    }

    private Detail detailWithNoPostPayment() {
        return detailFromJSON("{\n" +
                "          \"Api\": \"POST autreapi\",\n" +
                "          \"Fieldname\" : \"PaymentProduct\",\n" +
                "          \"Type\": \"SUPPORTED\",\n" +
                "          \"Value\": \"Normal|Instant\",\n" +
                "          \"ProtocolVersion\": \"STET_V_1_4_0_47\"\n" +
                "        }");
    }

    private Detail detailWithPostPaymentInstantAndNormal() {
        return detailFromJSON("{\n" +
                "          \"Api\": \"POST /payments\",\n" +
                "          \"Fieldname\" : \"PaymentProduct\",\n" +
                "          \"Type\": \"SUPPORTED\",\n" +
                "          \"Value\": \"Normal|Instant\",\n" +
                "          \"ProtocolVersion\": \"STET_V_1_4_0_47\"\n" +
                "        }");
    }

    private Detail detailWithPostPaymentInstant() {
        return detailFromJSON("{\n" +
                "          \"Api\": \"POST /payments\",\n" +
                "          \"Fieldname\" : \"PaymentProduct\",\n" +
                "          \"Type\": \"SUPPORTED\",\n" +
                "          \"Value\": \"Instant\",\n" +
                "          \"ProtocolVersion\": \"STET_V_1_4_0_47\"\n" +
                "        }");
    }

    private Detail detailWithPostPaymentNormal() {
        return detailFromJSON("{\n" +
                "          \"Api\": \"POST /payments\",\n" +
                "          \"Fieldname\" : \"PaymentProduct\",\n" +
                "          \"Type\": \"SUPPORTED\",\n" +
                "          \"Value\": \"Normal\",\n" +
                "          \"ProtocolVersion\": \"STET_V_1_4_0_47\"\n" +
                "        }");
    }

    private Detail detailWithPostPaymentRandomValue() {
        return detailFromJSON("{\n" +
                "          \"Api\": \"POST /payments\",\n" +
                "          \"Fieldname\" : \"PaymentProduct\",\n" +
                "          \"Type\": \"SUPPORTED\",\n" +
                "          \"Value\": \"Random\",\n" +
                "          \"ProtocolVersion\": \"STET_V_1_4_0_47\"\n" +
                "        }");
    }

    private Detail detailFromJSON(final String json) {
        final Gson gson = new Gson();
        return gson.fromJson(json, Detail.class);
    }
}
