package com.payline.payment.equens.business.impl;

import com.payline.payment.equens.MockUtils;
import com.payline.payment.equens.bean.business.payment.PaymentStatus;
import com.payline.pmapi.bean.common.FailureCause;
import com.payline.pmapi.bean.common.OnHoldCause;
import com.payline.pmapi.bean.payment.response.PaymentResponse;
import com.payline.pmapi.bean.payment.response.buyerpaymentidentifier.impl.BankTransfer;
import com.payline.pmapi.bean.payment.response.impl.PaymentResponseFailure;
import com.payline.pmapi.bean.payment.response.impl.PaymentResponseOnHold;
import com.payline.pmapi.bean.payment.response.impl.PaymentResponseSuccess;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentBusinessImplTest {

    private PaymentBusinessImpl underTest = new PaymentBusinessImpl();

    private static final String PAYMENT_ID = "paymentId";
    private static final String TRANSACTION_ADDITIONAL_DATA = "transactionAdditionalData";
    private static final String MERCHANT_IBAN = "merchantIBAN";

    @Nested
    class testFetchPaymentResponse {

        @ParameterizedTest
        @EnumSource(value = PaymentStatus.class, names = {"OPEN", "AUTHORISED", "PENDING"})
        void withPaymentResponseInProgress(final PaymentStatus status) {
            final PaymentResponse paymentResponse = underTest.fetchPaymentResponse(MockUtils.aPaymentStatusResponse(status), PAYMENT_ID, TRANSACTION_ADDITIONAL_DATA, MERCHANT_IBAN);
            assertNotNull(paymentResponse);
            assertTrue(paymentResponse instanceof PaymentResponseOnHold);
            final PaymentResponseOnHold paymentResponseOnHold = (PaymentResponseOnHold) paymentResponse;
            assertEquals(PAYMENT_ID, paymentResponseOnHold.getPartnerTransactionId());
            assertEquals(status.name(), paymentResponseOnHold.getStatusCode());
            assertEquals(OnHoldCause.INPROGRESS_PARTNER, paymentResponseOnHold.getOnHoldCause());
        }

        @ParameterizedTest
        @EnumSource(value = PaymentStatus.class, names = {"SETTLEMENT_IN_PROCESS", "SETTLEMENT_COMPLETED"})
        void withPaymentResponseSuccess(final PaymentStatus status) {
            final PaymentResponse paymentResponse = underTest.fetchPaymentResponse(MockUtils.aPaymentStatusResponse(status), PAYMENT_ID, TRANSACTION_ADDITIONAL_DATA, MERCHANT_IBAN);
            assertNotNull(paymentResponse);
            assertTrue(paymentResponse instanceof PaymentResponseSuccess);
            final PaymentResponseSuccess paymentResponseSuccess = (PaymentResponseSuccess) paymentResponse;
            assertEquals(PAYMENT_ID, paymentResponseSuccess.getPartnerTransactionId());
            assertEquals(status.name(), paymentResponseSuccess.getStatusCode());
            assertEquals(TRANSACTION_ADDITIONAL_DATA, paymentResponseSuccess.getTransactionAdditionalData());
            assertTrue(paymentResponseSuccess.getTransactionDetails() instanceof BankTransfer);
            final BankTransfer bankTransfer = (BankTransfer) paymentResponseSuccess.getTransactionDetails();
            assertEquals("AT880000000000000001", bankTransfer.getOwner().getIban());
            assertEquals("BNPADEFF", bankTransfer.getOwner().getBic());
            assertEquals("merchantIBAN", bankTransfer.getReceiver().getIban());
        }
    }



    @ParameterizedTest
    @MethodSource("withErrorPaymentResponseArgs")
    void fetchErrorPaymentResponse(PaymentStatus paymentStatus, FailureCause expectedFailureCause ,String message) {
        final PaymentResponse paymentResponse = underTest.fetchPaymentResponse(MockUtils.aPaymentStatusResponse(paymentStatus), PAYMENT_ID, TRANSACTION_ADDITIONAL_DATA, MERCHANT_IBAN);
        assertNotNull(paymentResponse);
        assertTrue(paymentResponse instanceof PaymentResponseFailure);

        final PaymentResponseFailure paymentResponseFailure = (PaymentResponseFailure) paymentResponse;
        assertEquals(PAYMENT_ID, paymentResponseFailure.getPartnerTransactionId());
        assertEquals(expectedFailureCause, paymentResponseFailure.getFailureCause());
        assertEquals(TRANSACTION_ADDITIONAL_DATA, paymentResponseFailure.getTransactionAdditionalData());
        assertEquals(message, paymentResponseFailure.getErrorCode());
    }

    static Stream<Arguments> withErrorPaymentResponseArgs(){
        return Stream.of(
                Arguments.of( PaymentStatus.CANCELLED, FailureCause.CANCEL, "Payment not approved by PSU or insufficient funds" ),
                Arguments.of( PaymentStatus.EXPIRED, FailureCause.SESSION_EXPIRED, "Consent approval has expired" ),
                Arguments.of( PaymentStatus.ERROR, FailureCause.REFUSED, "Payment was rejected due to an error"));

    }

    @ParameterizedTest
    @MethodSource("withErrorLastPaymentResponseArgs")
    void fetchErrorLastStatusResponse(PaymentStatus paymentStatus, FailureCause expectedFailureCause ,String message) {
        final PaymentResponse paymentResponse = underTest.fetchLastStatusPaymentResponse(MockUtils.aPaymentStatusResponse(paymentStatus), PAYMENT_ID, TRANSACTION_ADDITIONAL_DATA, MERCHANT_IBAN);
        assertNotNull(paymentResponse);
        assertTrue(paymentResponse instanceof PaymentResponseFailure);

        final PaymentResponseFailure paymentResponseFailure = (PaymentResponseFailure) paymentResponse;
        assertEquals(PAYMENT_ID, paymentResponseFailure.getPartnerTransactionId());
        assertEquals(expectedFailureCause, paymentResponseFailure.getFailureCause());
        assertEquals(TRANSACTION_ADDITIONAL_DATA, paymentResponseFailure.getTransactionAdditionalData());
        assertEquals(message, paymentResponseFailure.getErrorCode());
    }

    static Stream<Arguments> withErrorLastPaymentResponseArgs(){
        return Stream.of(
                Arguments.of(PaymentStatus.CANCELLED, FailureCause.CANCEL, "Payment not approved by PSU or insufficient funds"),
                Arguments.of(PaymentStatus.EXPIRED, FailureCause.SESSION_EXPIRED, "Consent approval has expired"),
                Arguments.of(PaymentStatus.ERROR, FailureCause.REFUSED, "Payment was rejected due to an error"),
                Arguments.of(PaymentStatus.OPEN, FailureCause.REFUSED, "Payment is rejected"));
    }

    @ParameterizedTest
    @EnumSource(value = PaymentStatus.class, names = {"SETTLEMENT_IN_PROCESS", "SETTLEMENT_COMPLETED", "PENDING", "AUTHORISED"})
    void fetchLastStatusResponseSuccess(final PaymentStatus status) {
        final PaymentResponse paymentResponse = underTest.fetchLastStatusPaymentResponse(MockUtils.aPaymentStatusResponse(status), PAYMENT_ID, TRANSACTION_ADDITIONAL_DATA, MERCHANT_IBAN);
        assertNotNull(paymentResponse);
        assertTrue(paymentResponse instanceof PaymentResponseSuccess);
        final PaymentResponseSuccess paymentResponseSuccess = (PaymentResponseSuccess) paymentResponse;
        assertEquals(PAYMENT_ID, paymentResponseSuccess.getPartnerTransactionId());
        assertEquals(status.name(), paymentResponseSuccess.getStatusCode());
        assertEquals(TRANSACTION_ADDITIONAL_DATA, paymentResponseSuccess.getTransactionAdditionalData());
        assertTrue(paymentResponseSuccess.getTransactionDetails() instanceof BankTransfer);
        final BankTransfer bankTransfer = (BankTransfer) paymentResponseSuccess.getTransactionDetails();
        assertEquals("AT880000000000000001", bankTransfer.getOwner().getIban());
        assertEquals("BNPADEFF", bankTransfer.getOwner().getBic());
        assertEquals("merchantIBAN", bankTransfer.getReceiver().getIban());
    }

}
