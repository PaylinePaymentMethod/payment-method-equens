package com.payline.payment.equens.service.impl;

import com.payline.payment.equens.bean.GenericPaymentRequest;
import com.payline.payment.equens.bean.business.payment.PaymentData;
import com.payline.payment.equens.exception.InvalidDataException;
import com.payline.payment.equens.exception.PluginException;
import com.payline.payment.equens.service.JsonService;
import com.payline.payment.equens.service.GenericPaymentService;
import com.payline.payment.equens.utils.PluginUtils;
import com.payline.payment.equens.utils.constant.PartnerConfigurationKeys;
import com.payline.payment.equens.utils.security.RSAUtils;
import com.payline.pmapi.bean.common.FailureCause;
import com.payline.pmapi.bean.payment.request.WalletPaymentRequest;
import com.payline.pmapi.bean.payment.response.PaymentResponse;
import com.payline.pmapi.bean.payment.response.impl.PaymentResponseFailure;
import com.payline.pmapi.logger.LogManager;
import com.payline.pmapi.service.PaymentWalletService;
import org.apache.logging.log4j.Logger;

public class PaymentWalletServiceImpl implements PaymentWalletService {
    private static final Logger LOGGER = LogManager.getLogger(PaymentWalletServiceImpl.class);

    private RSAUtils rsaUtils = RSAUtils.getInstance();
    private GenericPaymentService genericPaymentService = GenericPaymentService.getInstance();
    private JsonService jsonService = JsonService.getInstance();

    @Override
    public PaymentResponse walletPaymentRequest(WalletPaymentRequest walletPaymentRequest) {
        try {
            GenericPaymentRequest genericPaymentRequest = new GenericPaymentRequest(walletPaymentRequest);

            // get decrypted wallet data (BIC + IBAN)
            String encryptedData = walletPaymentRequest.getWallet().getPluginPaymentData();
            if (PluginUtils.isEmpty(encryptedData)){
                throw new InvalidDataException("WalletPaymentRequest shall have a pluginPaymentData");
            }

            String key = walletPaymentRequest.getPartnerConfiguration().getProperty(PartnerConfigurationKeys.ENCRYPTION_KEY);
            String data = rsaUtils.decrypt(encryptedData, key);

            // create the WalletPaymentData object to recover the BIC
            PaymentData paymentData = jsonService.fromJson(data, PaymentData.class);

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
