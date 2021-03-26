package com.payline.payment.equens.service.impl;

import com.payline.payment.equens.bean.GenericPaymentRequest;
import com.payline.payment.equens.bean.business.payment.PaymentData;
import com.payline.payment.equens.exception.PluginException;
import com.payline.payment.equens.service.GenericPaymentService;
import com.payline.payment.equens.utils.Constants;
import com.payline.pmapi.bean.common.FailureCause;
import com.payline.pmapi.bean.payment.request.PaymentRequest;
import com.payline.pmapi.bean.payment.response.PaymentResponse;
import com.payline.pmapi.bean.payment.response.impl.PaymentResponseFailure;
import com.payline.pmapi.bean.paymentform.bean.form.BankTransferForm;
import com.payline.pmapi.logger.LogManager;
import com.payline.pmapi.service.PaymentService;
import org.apache.logging.log4j.Logger;

public class PaymentServiceImpl implements PaymentService {
    private GenericPaymentService genericPaymentService = GenericPaymentService.getInstance();
    private static final Logger LOGGER = LogManager.getLogger(PaymentServiceImpl.class);

    @Override
    public PaymentResponse paymentRequest(PaymentRequest paymentRequest) {
        try {
            final GenericPaymentRequest genericPaymentRequest = new GenericPaymentRequest(paymentRequest);
            final PaymentData paymentData = new PaymentData.PaymentDataBuilder()
                    .withBic(paymentRequest.getPaymentFormContext().getPaymentFormParameter().get(BankTransferForm.BANK_KEY))
                    .withIban(paymentRequest.getPaymentFormContext().getSensitivePaymentFormParameter().get(BankTransferForm.IBAN_KEY))
                    .withAspspId(paymentRequest.getPaymentFormContext().getPaymentFormParameter().get(Constants.FormKeys.ASPSP_ID))
                    .build();

            // execute the payment Request
            return genericPaymentService.paymentRequest(genericPaymentRequest, paymentData);
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
