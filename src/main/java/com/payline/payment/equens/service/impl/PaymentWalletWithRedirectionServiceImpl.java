package com.payline.payment.equens.service.impl;

import com.payline.payment.equens.bean.configuration.RequestConfiguration;
import com.payline.payment.equens.exception.InvalidDataException;
import com.payline.payment.equens.exception.PluginException;
import com.payline.payment.equens.utils.Constants;
import com.payline.pmapi.bean.payment.request.WalletRedirectionPaymentRequest;
import com.payline.pmapi.bean.payment.response.PaymentResponse;
import com.payline.pmapi.service.PaymentWalletWithRedirectionService;

public class PaymentWalletWithRedirectionServiceImpl extends AbstractRedirectionServiceImpl implements PaymentWalletWithRedirectionService {

    @Override
    public PaymentResponse finalizeRedirectionPaymentWallet(final WalletRedirectionPaymentRequest redirectionPaymentRequest) {

        PaymentResponse paymentResponse;
        try {
            //Check if wallet payment is not empty.
            if (redirectionPaymentRequest.getWallet() == null
                    || redirectionPaymentRequest.getWallet().getPluginPaymentData() == null) {
                throw new InvalidDataException("Missing wallet information for request context");
            }

            // Retrieve payment ID from request context
            if (redirectionPaymentRequest.getRequestContext() == null
                    || redirectionPaymentRequest.getRequestContext().getRequestData() == null
                    || redirectionPaymentRequest.getRequestContext().getRequestData().get(Constants.RequestContextKeys.PAYMENT_ID) == null) {
                throw new InvalidDataException("Missing payment ID from request context");
            }
            final String paymentId = redirectionPaymentRequest.getRequestContext().getRequestData()
                    .get(Constants.RequestContextKeys.PAYMENT_ID);

            // check and update payment status
            paymentResponse = this.updatePaymentStatus(paymentId, new RequestConfiguration(
                    redirectionPaymentRequest.getContractConfiguration(), redirectionPaymentRequest.getEnvironment(),
                            redirectionPaymentRequest.getPartnerConfiguration()));
        } catch (final PluginException e) {
            paymentResponse = e.toPaymentResponseFailureBuilder().build();
        }
        return paymentResponse;
    }
}
