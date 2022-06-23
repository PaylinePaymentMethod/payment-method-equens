package com.payline.payment.equens.service.impl;

import com.payline.payment.equens.bean.business.payment.PaymentStatus;
import com.payline.payment.equens.bean.business.payment.PaymentStatusResponse;
import com.payline.payment.equens.bean.configuration.RequestConfiguration;
import com.payline.payment.equens.bean.pmapi.TransactionAdditionalData;
import com.payline.payment.equens.business.PaymentBusiness;
import com.payline.payment.equens.business.impl.PaymentBusinessImpl;
import com.payline.payment.equens.exception.PluginException;
import com.payline.payment.equens.service.JsonService;
import com.payline.payment.equens.utils.Constants;
import com.payline.payment.equens.utils.http.PisHttpClient;
import com.payline.pmapi.bean.common.FailureCause;
import com.payline.pmapi.bean.payment.response.PaymentResponse;
import com.payline.pmapi.bean.payment.response.impl.PaymentResponseFailure;
import com.payline.pmapi.logger.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class AbstractRedirectionServiceImpl {

    private PisHttpClient pisHttpClient = PisHttpClient.getInstance();
    private JsonService jsonService = JsonService.getInstance();
    private PaymentBusiness paymentBusiness = new PaymentBusinessImpl();

    private static final Logger LOGGER = LogManager.getLogger(AbstractRedirectionServiceImpl.class);
    /**
     * Request the partner API to get the payment status and return the appropriate <code>PaymentResponse</code>.
     *
     * @param paymentId The payment ID (on the partner side)
     * @param requestConfiguration the request configuration
     * @return a PaymentResponse
     */
    PaymentResponse updatePaymentStatus(final String paymentId, final RequestConfiguration requestConfiguration, final boolean lastCall) {
        PaymentResponse paymentResponse;
        try {
            // Init HTTP client
            pisHttpClient.init(requestConfiguration.getPartnerConfiguration());

            // Retrieve the payment status
            final PaymentStatusResponse paymentStatusResponse = pisHttpClient.paymentStatus(paymentId, requestConfiguration, true);
            final PaymentStatus status = paymentStatusResponse.getPaymentStatus();

            if (status == null) {
                throw new PluginException("Missing payment status in the partner response", FailureCause.PARTNER_UNKNOWN_ERROR);
            }

            // Build transaction additional data
            final TransactionAdditionalData transactionAdditionalData = new TransactionAdditionalData(paymentStatusResponse.getAspspPaymentId());
            final String transactionDataJson = jsonService.toJson(transactionAdditionalData);

            // Retrieve merchant IBAN
            String merchantIban = null;
            if (requestConfiguration.getContractConfiguration()
                    .getProperty(Constants.ContractConfigurationKeys.MERCHANT_IBAN) != null) {
                merchantIban = requestConfiguration.getContractConfiguration().getProperty(Constants.ContractConfigurationKeys.MERCHANT_IBAN).getValue();
            }
            if (lastCall) {
                paymentResponse = paymentBusiness.fetchLastStatusPaymentResponse(paymentStatusResponse, paymentId, transactionDataJson, merchantIban);
            } else {
                paymentResponse = paymentBusiness.fetchPaymentResponse(paymentStatusResponse, paymentId, transactionDataJson, merchantIban);
            }
        } catch (final PluginException e) {
            paymentResponse = e.toPaymentResponseFailureBuilder()
                    .withPartnerTransactionId(paymentId)
                    .build();
        } catch (final RuntimeException e) {
            LOGGER.error("Unexpected plugin error", e);
            paymentResponse = PaymentResponseFailure.PaymentResponseFailureBuilder
                    .aPaymentResponseFailure()
                    .withErrorCode(PluginException.runtimeErrorCode(e))
                    .withFailureCause(FailureCause.INTERNAL_ERROR)
                    .build();
        }
        return paymentResponse;
    }
}
