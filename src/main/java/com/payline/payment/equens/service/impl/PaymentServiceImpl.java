package com.payline.payment.equens.service.impl;

import com.payline.payment.equens.bean.GenericPaymentRequest;
import com.payline.payment.equens.bean.business.reachdirectory.GetAspspsResponse;
import com.payline.payment.equens.service.JsonService;
import com.payline.payment.equens.bean.business.payment.WalletPaymentData;
import com.payline.payment.equens.exception.PluginException;
import com.payline.payment.equens.service.Payment;
import com.payline.pmapi.bean.common.FailureCause;
import com.payline.pmapi.bean.payment.request.PaymentRequest;
import com.payline.pmapi.bean.payment.response.PaymentResponse;
import com.payline.pmapi.bean.payment.response.impl.PaymentResponseFailure;
import com.payline.pmapi.bean.paymentform.bean.form.BankTransferForm;
import com.payline.pmapi.logger.LogManager;
import com.payline.pmapi.service.PaymentService;
import org.apache.logging.log4j.Logger;

public class PaymentServiceImpl implements PaymentService {
    private Payment payment = Payment.getInstance();
    private JsonService jsonService = JsonService.getInstance();
    private static final Logger LOGGER = LogManager.getLogger(PaymentServiceImpl.class);

    @Override
    public PaymentResponse paymentRequest(PaymentRequest paymentRequest) {
        try {
        GenericPaymentRequest genericPaymentRequest = new GenericPaymentRequest(paymentRequest);

        WalletPaymentData walletPaymentData = new WalletPaymentData.WalletPaymentDataBuilder()
                .withBic(paymentRequest.getPaymentFormContext().getPaymentFormParameter().get(BankTransferForm.BANK_KEY))
                .withIban(paymentRequest.getPaymentFormContext().getPaymentFormParameter().get(BankTransferForm.IBAN_KEY))
                .build();

        // execute the payment Request
        return payment.paymentRequest(genericPaymentRequest, walletPaymentData);
        } catch (RuntimeException e) {
            LOGGER.error("Unexpected plugin error", e);
            return PaymentResponseFailure.PaymentResponseFailureBuilder
                    .aPaymentResponseFailure()
                    .withErrorCode(PluginException.runtimeErrorCode(e))
                    .withFailureCause(FailureCause.INTERNAL_ERROR)
                    .build();
        }
    }
}
