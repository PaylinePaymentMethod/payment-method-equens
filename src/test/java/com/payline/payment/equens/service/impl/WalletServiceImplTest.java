package com.payline.payment.equens.service.impl;

import com.payline.payment.equens.MockUtils;
import com.payline.payment.equens.exception.PluginException;
import com.payline.payment.equens.service.BankService;
import com.payline.payment.equens.service.JsonService;
import com.payline.payment.equens.utils.security.RSAUtils;
import com.payline.pmapi.bean.payment.PaymentFormContext;
import com.payline.pmapi.bean.payment.Wallet;
import com.payline.pmapi.bean.paymentform.bean.form.BankTransferForm;
import com.payline.pmapi.bean.wallet.bean.WalletDisplay;
import com.payline.pmapi.bean.wallet.bean.field.WalletDisplayFieldText;
import com.payline.pmapi.bean.wallet.request.WalletCreateRequest;
import com.payline.pmapi.bean.wallet.request.WalletDisplayRequest;
import com.payline.pmapi.bean.wallet.response.WalletCreateResponse;
import com.payline.pmapi.bean.wallet.response.impl.WalletCreateResponseFailure;
import com.payline.pmapi.bean.wallet.response.impl.WalletCreateResponseSuccess;
import com.payline.pmapi.bean.wallet.response.impl.WalletDeleteResponseSuccess;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

class WalletServiceImplTest {

    private JsonService jsonService = JsonService.getInstance();

    @Mock
    RSAUtils rsaUtils;

    @Mock
    BankService bankService;

    @InjectMocks
    WalletServiceImpl underTest;

    private static final String BANK = "thisIsABank";
    private static final String IBAN = "thisIsAnIban";

    @BeforeEach
    void setUp() {
        underTest = new WalletServiceImpl();
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void deleteWallet() {
        assertEquals(WalletDeleteResponseSuccess.class, underTest.deleteWallet(null).getClass());
    }

    @Test
    void updateWallet() {
        // not used for now
        Assertions.assertNull(underTest.updateWallet(null));
    }

    @Test
    void createWallet() {
        String pluginPaymentData = jsonService.toJson( MockUtils.aPaymentData());
        doReturn(pluginPaymentData).when(rsaUtils).encrypt(anyString(), anyString());

        Map<String, String> paymentFormDataContext = new HashMap<>();
        paymentFormDataContext.put(BankTransferForm.BANK_KEY, BANK);
        Map<String, String> sensitivePaymentFormDataContext = new HashMap<>();
        sensitivePaymentFormDataContext.put(BankTransferForm.IBAN_KEY, IBAN);

        PaymentFormContext context = PaymentFormContext.PaymentFormContextBuilder
                .aPaymentFormContext()
                .withPaymentFormParameter(paymentFormDataContext)
                .withSensitivePaymentFormParameter(sensitivePaymentFormDataContext)
                .build();
        WalletCreateRequest request = WalletCreateRequest.builder()
                .paymentFormContext(context)
                .partnerConfiguration(MockUtils.aPartnerConfiguration())
                .build();
        WalletCreateResponse response = underTest.createWallet(request);

        assertEquals(WalletCreateResponseSuccess.class, response.getClass());
        WalletCreateResponseSuccess responseSuccess = (WalletCreateResponseSuccess) response;
        assertEquals(pluginPaymentData, responseSuccess.getPluginPaymentData());
        verify(rsaUtils, atLeastOnce()).encrypt("{\"bic\":\"thisIsABank\",\"iban\":\"thisIsAnIban\"}", "thisIsAKey");
    }

    @Test
    void createWalletBicNull() {
        String pluginPaymentData = jsonService.toJson( MockUtils.aPaymentDataBicNull());
        doReturn(pluginPaymentData).when(rsaUtils).encrypt(anyString(), anyString());

        Map<String, String> paymentFormDataContext = new HashMap<>();
        Map<String, String> sensitivePaymentFormDataContext = new HashMap<>();
        sensitivePaymentFormDataContext.put(BankTransferForm.IBAN_KEY, IBAN);

        PaymentFormContext context = PaymentFormContext.PaymentFormContextBuilder
                .aPaymentFormContext()
                .withPaymentFormParameter(paymentFormDataContext)
                .withSensitivePaymentFormParameter(sensitivePaymentFormDataContext)
                .build();
        WalletCreateRequest request = WalletCreateRequest.builder()
                .paymentFormContext(context)
                .partnerConfiguration(MockUtils.aPartnerConfiguration())
                .build();
        WalletCreateResponse response = underTest.createWallet(request);

        assertEquals(WalletCreateResponseSuccess.class, response.getClass());
        WalletCreateResponseSuccess responseSuccess = (WalletCreateResponseSuccess) response;
        assertEquals(pluginPaymentData, responseSuccess.getPluginPaymentData());
        verify(rsaUtils, atLeastOnce()).encrypt("{\"iban\":\"thisIsAnIban\"}", "thisIsAKey");
    }

    @Test
    void createWalletIbanNull() {
        String pluginPaymentData = jsonService.toJson( MockUtils.aPaymentDataIbanNull());
        doReturn(pluginPaymentData).when(rsaUtils).encrypt(anyString(), anyString());

        Map<String, String> paymentFormDataContext = new HashMap<>();
        paymentFormDataContext.put(BankTransferForm.BANK_KEY, BANK);
        Map<String, String> sensitivePaymentFormDataContext = new HashMap<>();

        PaymentFormContext context = PaymentFormContext.PaymentFormContextBuilder
                .aPaymentFormContext()
                .withPaymentFormParameter(paymentFormDataContext)
                .withSensitivePaymentFormParameter(sensitivePaymentFormDataContext)
                .build();
        WalletCreateRequest request = WalletCreateRequest.builder()
                .paymentFormContext(context)
                .partnerConfiguration(MockUtils.aPartnerConfiguration())
                .build();
        WalletCreateResponse response = underTest.createWallet(request);

        assertEquals(WalletCreateResponseSuccess.class, response.getClass());
        WalletCreateResponseSuccess responseSuccess = (WalletCreateResponseSuccess) response;
        assertEquals(pluginPaymentData, responseSuccess.getPluginPaymentData());
        verify(rsaUtils, atLeastOnce()).encrypt("{\"bic\":\"thisIsABank\"}", "thisIsAKey");

    }

    @Test
    void createWalletFailure() {

        Mockito.doThrow(new PluginException("foo")).when(rsaUtils).encrypt(anyString(), anyString());

        Map<String, String> paymentFormDataContext = new HashMap<>();
        paymentFormDataContext.put(BankTransferForm.BANK_KEY, "thisIsABank");

        PaymentFormContext context = PaymentFormContext.PaymentFormContextBuilder
                .aPaymentFormContext()
                .withPaymentFormParameter(paymentFormDataContext)
                .build();
        WalletCreateRequest request = WalletCreateRequest.builder()
                .paymentFormContext(context)
                .pluginConfiguration(MockUtils.aPluginConfiguration())
                .build();
        WalletCreateResponse response = underTest.createWallet(request);

        assertEquals(WalletCreateResponseFailure.class, response.getClass());
    }

    @Test
    void createWalletFailureNoPluginConfiguration() {

        Mockito.doThrow(new PluginException("foo")).when(rsaUtils).encrypt(anyString(), anyString());

        Map<String, String> paymentFormDataContext = new HashMap<>();
        paymentFormDataContext.put(BankTransferForm.BANK_KEY, "thisIsABank");

        PaymentFormContext context = PaymentFormContext.PaymentFormContextBuilder
                .aPaymentFormContext()
                .withPaymentFormParameter(paymentFormDataContext)
                .build();
        WalletCreateRequest request = WalletCreateRequest.builder()
                .paymentFormContext(context)
                .pluginConfiguration("")
                .build();
        WalletCreateResponse response = underTest.createWallet(request);

        assertEquals(WalletCreateResponseFailure.class, response.getClass());
    }

    @Nested
    public class displayWallet {

        @Test
        void displayWalletWithIBANAndAspspID() {
            final String pluginPaymentData = "{\"bic\":\"PSSTFRPP\",\"iban\":\"anIbanWithMoreThan8Charactere\",\"aspspId\":\"10\"}";;
            doReturn(MockUtils.anAspsp()).when(bankService).getAspsp(any(), eq("10"));
            doReturn(pluginPaymentData).when(rsaUtils).decrypt(anyString(), anyString());
            final Wallet wallet = Wallet.builder()
                    .pluginPaymentData(pluginPaymentData)
                    .build();

            final WalletDisplayRequest request = WalletDisplayRequest.builder()
                    .wallet(wallet)
                    .partnerConfiguration(MockUtils.aPartnerConfiguration())
                    .build();

            final WalletDisplay response = (WalletDisplay) underTest.displayWallet(request);
            assertNotNull(response.getWalletFields());
            assertEquals(2, response.getWalletFields().size());
            assertEquals("La banque Postale", ((WalletDisplayFieldText) response.getWalletFields().get(0)).getContent());
            assertEquals("anIbXXXXXXXXXXXXXXXXXXXXXtere", ((WalletDisplayFieldText) response.getWalletFields().get(1)).getContent());

        }

        @Test
        void displayWalletWithIBAN() {
            String pluginPaymentData = "{\"bic\":\"PSSTFRPP\",\"iban\":\"anIbanWithMoreThan8Charactere\"}";;
            doReturn(pluginPaymentData).when(rsaUtils).decrypt(anyString(), anyString());

            final Wallet wallet = Wallet.builder()
                    .pluginPaymentData(pluginPaymentData)
                    .build();

            final WalletDisplayRequest request = WalletDisplayRequest.builder()
                    .wallet(wallet)
                    .partnerConfiguration(MockUtils.aPartnerConfiguration())
                    .build();

            WalletDisplay response = (WalletDisplay) underTest.displayWallet(request);
            assertNotNull(response.getWalletFields());
            assertEquals(1, response.getWalletFields().size());
            assertEquals("anIbXXXXXXXXXXXXXXXXXXXXXtere", ((WalletDisplayFieldText) response.getWalletFields().get(0)).getContent());
        }

        @Test
        void displayWalletOnlyAspsp(){
            final String pluginPaymentData = "{\"aspspId\":\"10\"}";;
            doReturn(pluginPaymentData).when(rsaUtils).decrypt(anyString(), anyString());
            doReturn(MockUtils.anAspsp()).when(bankService).getAspsp(any(), eq("10"));

            Wallet wallet = Wallet.builder()
                    .pluginPaymentData(pluginPaymentData)
                    .build();

            WalletDisplayRequest request = WalletDisplayRequest.builder()
                    .wallet(wallet)
                    .partnerConfiguration(MockUtils.aPartnerConfiguration())
                    .build();

            WalletDisplay response = (WalletDisplay) underTest.displayWallet(request);
            assertNotNull(response.getWalletFields());
            assertEquals(1, response.getWalletFields().size());
            assertEquals("La banque Postale", ((WalletDisplayFieldText) response.getWalletFields().get(0)).getContent());
        }

        @Test
        void displayWalletFailure() {
            Mockito.doThrow(new PluginException("foo")).when(rsaUtils).decrypt(anyString(), anyString());

            Wallet wallet = Wallet.builder()
                    .pluginPaymentData("foo")
                    .build();

            WalletDisplayRequest request = WalletDisplayRequest.builder()
                    .wallet(wallet)
                    .partnerConfiguration(MockUtils.aPartnerConfiguration())
                    .build();

            WalletDisplay response = (WalletDisplay) underTest.displayWallet(request);
            assertNotNull(response.getWalletFields());
            assertEquals(0, response.getWalletFields().size());
        }
    }

}