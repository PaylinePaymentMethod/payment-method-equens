package com.payline.payment.equens.service.impl;

import com.payline.payment.equens.MockUtils;
import com.payline.payment.equens.utils.Constants;
import com.payline.pmapi.bean.common.FailureCause;
import com.payline.pmapi.bean.payment.RequestContext;
import com.payline.pmapi.bean.payment.Wallet;
import com.payline.pmapi.bean.payment.request.WalletRedirectionPaymentRequest;
import com.payline.pmapi.bean.payment.response.PaymentResponse;
import com.payline.pmapi.bean.payment.response.buyerpaymentidentifier.impl.EmptyTransactionDetails;
import com.payline.pmapi.bean.payment.response.impl.PaymentResponseFailure;
import com.payline.pmapi.bean.payment.response.impl.PaymentResponseSuccess;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;


class PaymentWalletWithRedirectionServiceImplTest {

    @Spy
    @InjectMocks
    PaymentWalletWithRedirectionServiceImpl underTest;

    @BeforeEach
    void setup(){
        MockitoAnnotations.initMocks( this );
    }

    @Test
    public void testFinalizeRedirectionWithNoWallet() {
        WalletRedirectionPaymentRequest walletRedirectionPaymentRequest = WalletRedirectionPaymentRequest.builder().build();
        PaymentResponse paymentResponse = underTest.finalizeRedirectionPaymentWallet(walletRedirectionPaymentRequest);
        assertTrue(paymentResponse instanceof PaymentResponseFailure);
        assertEquals(FailureCause.INVALID_DATA, ((PaymentResponseFailure) paymentResponse).getFailureCause());
        assertEquals("Missing wallet information for request context", ((PaymentResponseFailure) paymentResponse).getErrorCode());

    }

    @Test
    public void testFinalizeRedirectionWithNoPluginPaymentData() {
        WalletRedirectionPaymentRequest walletRedirectionPaymentRequest = WalletRedirectionPaymentRequest.builder().wallet(Wallet.builder().build()).build();
        PaymentResponse paymentResponse = underTest.finalizeRedirectionPaymentWallet(walletRedirectionPaymentRequest);
        assertTrue(paymentResponse instanceof PaymentResponseFailure);
        assertEquals(FailureCause.INVALID_DATA, ((PaymentResponseFailure) paymentResponse).getFailureCause());
        assertEquals("Missing wallet information for request context", ((PaymentResponseFailure) paymentResponse).getErrorCode());

    }

    @Test
    public void testFinalizeRedirectionWithNoPaymentId() {
        final WalletRedirectionPaymentRequest walletRedirectionPaymentRequest = WalletRedirectionPaymentRequest.builder()
                .wallet(Wallet.builder().pluginPaymentData("dataEncoded").build()).build();
        PaymentResponse paymentResponse = underTest.finalizeRedirectionPaymentWallet(walletRedirectionPaymentRequest);
        assertTrue(paymentResponse instanceof PaymentResponseFailure);
        assertEquals(FailureCause.INVALID_DATA, ((PaymentResponseFailure) paymentResponse).getFailureCause());
        assertEquals("Missing payment ID from request context", ((PaymentResponseFailure) paymentResponse).getErrorCode());

    }

    @Test
    public void testFinalizeRedirection() {

        final Map<String, String> requestData = new HashMap();
        requestData.put(Constants.RequestContextKeys.PAYMENT_ID, "1234");
        final RequestContext requestContext = RequestContext.RequestContextBuilder.aRequestContext().withRequestData(
                requestData).build();
        final WalletRedirectionPaymentRequest walletRedirectionPaymentRequest = WalletRedirectionPaymentRequest.builder()
                .requestContext(requestContext)
                .contractConfiguration(MockUtils.aContractConfiguration())
                .environment(MockUtils.anEnvironment())
                .partnerConfiguration(MockUtils.aPartnerConfiguration())
                .wallet(Wallet.builder().pluginPaymentData("dataEncoded").build()).build();

        final PaymentResponseSuccess paymentResponseSuccess = PaymentResponseSuccess.PaymentResponseSuccessBuilder
                .aPaymentResponseSuccess().withPartnerTransactionId("123")
                .withTransactionDetails(new EmptyTransactionDetails()).build();
        doReturn(paymentResponseSuccess).when(underTest).updatePaymentStatus(eq("1234"), any());
        PaymentResponse paymentResponse = underTest.finalizeRedirectionPaymentWallet(walletRedirectionPaymentRequest);
        assertEquals(paymentResponseSuccess, paymentResponse);
    }

}