package com.payline.payment.equens.service.impl;

import com.payline.pmapi.bean.common.Amount;
import com.payline.pmapi.bean.common.Buyer;
import com.payline.pmapi.bean.configuration.PartnerConfiguration;
import com.payline.pmapi.bean.payment.ContractConfiguration;
import com.payline.pmapi.bean.payment.Environment;
import com.payline.pmapi.bean.payment.Order;
import com.payline.pmapi.bean.refund.request.RefundRequest;
import com.payline.pmapi.bean.refund.response.RefundResponse;
import com.payline.pmapi.bean.refund.response.impl.RefundResponseSuccess;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import java.math.BigInteger;
import java.util.Currency;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RefundServiceImplTest {

    @InjectMocks
    private RefundServiceImpl underTest;

    @BeforeEach
    void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void testCapture() {
        final RefundRequest captureRequest = RefundRequest.RefundRequestBuilder.aRefundRequest()
                .withPartnerTransactionId("partnerTransactionId")
                .withAmount(new Amount(BigInteger.valueOf(22), Currency.getInstance("EUR")))
                .withBuyer(Buyer.BuyerBuilder.aBuyer().build())
                .withOrder(Order.OrderBuilder.anOrder().build())
                .withContractConfiguration(new ContractConfiguration("EQUENS", new HashMap<>()))
                .withEnvironment(new Environment("notifUrl", "redirectionUrl", "cancelUrl", true))
                .withTransactionId("transactionId")
                .withPartnerTransactionId("partnerTransactionId")
                .withSoftDescriptor("SoftDescriptor")
                .withPartnerConfiguration(new PartnerConfiguration(new HashMap<>(), new HashMap<>()))
                .build();
        final RefundResponse refundResponse = underTest.refundRequest(captureRequest);
        assertNotNull(refundResponse);
        assertTrue(refundResponse instanceof RefundResponseSuccess);
        final RefundResponseSuccess refundResponseSuccess = (RefundResponseSuccess) refundResponse;
        assertEquals("partnerTransactionId", refundResponseSuccess.getPartnerTransactionId());
        assertEquals(RefundServiceImpl.OK_STATUS_CODE, refundResponseSuccess.getStatusCode());
    }

    @Test
    void testCapturePartial() {
        assertFalse(underTest.canPartial());
    }

    @Test
    void testCaptureMultiple() {
        assertFalse(underTest.canMultiple());
    }

}
