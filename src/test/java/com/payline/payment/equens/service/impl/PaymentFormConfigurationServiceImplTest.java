package com.payline.payment.equens.service.impl;

import com.google.gson.JsonSyntaxException;
import com.payline.payment.equens.MockUtils;
import com.payline.payment.equens.bean.business.reachdirectory.Aspsp;
import com.payline.payment.equens.service.BankService;
import com.payline.payment.equens.utils.Constants;
import com.payline.payment.equens.utils.i18n.I18nService;
import com.payline.pmapi.bean.common.FailureCause;
import com.payline.pmapi.bean.paymentform.bean.field.PaymentFormDisplayFieldText;
import com.payline.pmapi.bean.paymentform.bean.field.PaymentFormField;
import com.payline.pmapi.bean.paymentform.bean.field.PaymentFormInputFieldSelect;
import com.payline.pmapi.bean.paymentform.bean.field.SelectOption;
import com.payline.pmapi.bean.paymentform.bean.field.specific.PaymentFormInputFieldIban;
import com.payline.pmapi.bean.paymentform.bean.form.AbstractPaymentForm;
import com.payline.pmapi.bean.paymentform.bean.form.BankTransferForm;
import com.payline.pmapi.bean.paymentform.bean.form.CustomForm;
import com.payline.pmapi.bean.paymentform.request.PaymentFormConfigurationRequest;
import com.payline.pmapi.bean.paymentform.response.configuration.PaymentFormConfigurationResponse;
import com.payline.pmapi.bean.paymentform.response.configuration.impl.PaymentFormConfigurationResponseFailure;
import com.payline.pmapi.bean.paymentform.response.configuration.impl.PaymentFormConfigurationResponseSpecific;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

class PaymentFormConfigurationServiceImplTest {

    @Mock
    private BankService bankService;

    @Mock
    private I18nService i18n;

    @InjectMocks
    private PaymentFormConfigurationServiceImpl underTest;

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
        underTest = new PaymentFormConfigurationServiceImpl();
        MockitoAnnotations.initMocks(this);

        // We consider by default that i18n behaves normally
        doReturn("message")
                .when(i18n)
                .getMessage(anyString(), any(Locale.class));
    }

    @Test
    void getPaymentFormConfiguration_nominal() {
        doReturn(Arrays.asList(MockUtils.anAspsp(), MockUtils.anAspsp()))
                .when(bankService).fetchBanks(eq(aspspsJson), any(), any());
        // given: the plugin configuration contains 3 french banks and the locale is FRANCE
        final PaymentFormConfigurationRequest request = MockUtils.aPaymentFormConfigurationRequestBuilder()
                                                                 .withLocale(Locale.FRANCE)
                                                                 .withPluginConfiguration(aspspsJson)
                                                                 .build();

        // when: calling getPaymentFormConfiguration method
        final PaymentFormConfigurationResponse response = underTest.getPaymentFormConfiguration(request);

        // then: response is a success, the form is a BankTransferForm and the number of banks is correct
        assertEquals(PaymentFormConfigurationResponseSpecific.class, response.getClass());
        final AbstractPaymentForm form = ((PaymentFormConfigurationResponseSpecific) response).getPaymentForm();
        assertNotNull(form.getButtonText());
        assertNotNull(form.getDescription());
        assertEquals(CustomForm.class, form.getClass());

        final CustomForm customForm = (CustomForm) form;
        final List<PaymentFormField> customFields = customForm.getCustomFields();
        assertEquals(3, customFields.size());
        assertTrue(customFields.get(0) instanceof PaymentFormInputFieldSelect);
        assertTrue(customFields.get(1) instanceof PaymentFormInputFieldSelect);
        assertTrue(customFields.get(2) instanceof PaymentFormInputFieldIban);

        final PaymentFormInputFieldSelect selectFields = (PaymentFormInputFieldSelect)customFields.get(0);
        assertEquals(Constants.FormKeys.ASPSP_ID, selectFields.getKey());
        assertNotNull(selectFields.getSelectOptions());
        assertEquals(2, selectFields.getSelectOptions().size());

        final PaymentFormInputFieldSelect selectSubsidiariesFields = (PaymentFormInputFieldSelect)customFields.get(1);
        assertEquals(Constants.FormKeys.SUB_ASPSP_ID, selectSubsidiariesFields.getKey());
        assertNotNull(selectSubsidiariesFields.getSelectOptions());
        assertEquals(0, selectSubsidiariesFields.getSelectOptions().size());

        final PaymentFormInputFieldIban ibanField = (PaymentFormInputFieldIban)customFields.get(2);
        assertEquals(BankTransferForm.IBAN_KEY, ibanField.getKey());
    }

    @Test
    void getPaymentFormConfiguration_invalidPluginConfiguration() {
        doThrow(JsonSyntaxException.class)
                .when(bankService).fetchBanks(any(), any(), any());
        // given: the plugin configuration is invalid
        final PaymentFormConfigurationRequest request = MockUtils.aPaymentFormConfigurationRequestBuilder()
                                                                 .withPluginConfiguration("{not valid")
                                                                 .build();

        // when: calling getPaymentFormConfiguration method
        final PaymentFormConfigurationResponse response = underTest.getPaymentFormConfiguration(request);

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
        final PaymentFormConfigurationResponse response = underTest.getPaymentFormConfiguration(request);

        // then: response is a failure
        assertEquals(PaymentFormConfigurationResponseFailure.class, response.getClass());
        assertNotNull(((PaymentFormConfigurationResponseFailure) response).getErrorCode());
        assertEquals("country must not be empty", ((PaymentFormConfigurationResponseFailure) response).getErrorCode());
        assertNotNull(((PaymentFormConfigurationResponseFailure) response).getFailureCause());
        assertEquals(FailureCause.INVALID_DATA, ((PaymentFormConfigurationResponseFailure) response).getFailureCause());
    }

    @Nested
    class bankScript {

        @Test
        void withEmptyBankList() {
            String result = underTest.buildBankScript(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
            assertEquals("", result);
        }

        @Test
        void buildBankWithoutSubsidiary() {
            final String expectedResult = "{ id : 'Axa Banque',\n" +
                    " iban : false,  subList : []},{ id : 'La Banque Postale',\n" +
                    " iban : false,  subList : []}";
            final Aspsp bank1 = new Aspsp();
            bank1.setCountryCode("FR");
            bank1.setName(Collections.singletonList("Axa Banque"));
            bank1.setAspspId("1");

            final Aspsp bank2 = new Aspsp();
            bank2.setCountryCode("FR");
            bank2.setName(Collections.singletonList("La Banque Postale"));
            bank2.setAspspId("2");

            final List<Aspsp> aspspList = Arrays.asList(bank1, bank2);
            final List<SelectOption> primaryList = new ArrayList<>();
            final List<SelectOption> subsidiaryList = new ArrayList<>();
            final String result = underTest.buildBankScript(aspspList, primaryList, subsidiaryList);
            assertTrue(subsidiaryList.isEmpty());
            assertEquals(expectedResult, result);

            final SelectOption bank1Option = primaryList.get(0);
            final SelectOption bank2Option = primaryList.get(1);

            assertEquals("Axa Banque", bank1Option.getValue());
            assertEquals("La Banque Postale", bank2Option.getValue());
            assertEquals("1", bank1Option.getKey());
            assertEquals("2", bank2Option.getKey());
        }

        @Test
        void buildBankWithSubsidiary() {
            final String expectedResult = "{ id : 'Axa Banque',\n" +
                    " iban : false,  subList : []},{ id : 'Crédit Agricole',\n" +
                    " iban : false,  subList : [{aspspId : '123', label : 'Crédit Agricole PACA',  iban : false}," +
                    "{aspspId : '456', label : 'Crédit Agricole Paris',  iban : false}]}";
            final Aspsp bank1 = new Aspsp();
            bank1.setCountryCode("FR");
            bank1.setName(Collections.singletonList("Axa Banque"));
            bank1.setAspspId("1");

            final Aspsp subAspsp = new Aspsp();
            subAspsp.setName(Collections.singletonList("Crédit Agricole PACA"));
            subAspsp.setAspspId("123");

            final Aspsp subAspsp2 = new Aspsp();
            subAspsp2.setName(Collections.singletonList("Crédit Agricole Paris"));
            subAspsp2.setAspspId("456");

            final Aspsp bank2 = new Aspsp();
            bank2.setCountryCode("FR");
            bank2.setName(Collections.singletonList("Crédit Agricole"));
            bank2.setAspspId("");
            bank2.setSubsidiariesList(Arrays.asList(subAspsp, subAspsp2));

            final List<Aspsp> aspspList = Arrays.asList(bank1, bank2);
            final List<SelectOption> primaryList = new ArrayList<>();
            final List<SelectOption> subsidiaryList = new ArrayList<>();
            final String result = underTest.buildBankScript(aspspList, primaryList, subsidiaryList);
            assertFalse(subsidiaryList.isEmpty());
            assertEquals(expectedResult, result);

            final SelectOption bank1Option = primaryList.get(0);
            final SelectOption bank2Option = primaryList.get(1);

            assertEquals("Axa Banque", bank1Option.getValue());
            assertEquals("Crédit Agricole", bank2Option.getValue());
            assertEquals("1", bank1Option.getKey());
            assertEquals("", bank2Option.getKey());

            final SelectOption subsidiary1Option = subsidiaryList.get(0);
            final SelectOption subsidiary2Option = subsidiaryList.get(1);

            assertEquals("Crédit Agricole PACA", subsidiary1Option.getValue());
            assertEquals("Crédit Agricole Paris", subsidiary2Option.getValue());
            assertEquals("123", subsidiary1Option.getKey());
            assertEquals("456", subsidiary2Option.getKey());
        }
    }
}

