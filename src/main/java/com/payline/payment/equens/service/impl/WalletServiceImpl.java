package com.payline.payment.equens.service.impl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.payline.payment.equens.bean.business.payment.PaymentData;
import com.payline.payment.equens.exception.PluginException;
import com.payline.payment.equens.service.JsonService;
import com.payline.payment.equens.utils.Constants;
import com.payline.payment.equens.utils.PluginUtils;
import com.payline.payment.equens.utils.properties.ConfigProperties;
import com.payline.payment.equens.utils.security.RSAUtils;
import com.payline.pmapi.bean.common.FailureCause;
import com.payline.pmapi.bean.paymentform.bean.form.BankTransferForm;
import com.payline.pmapi.bean.wallet.bean.WalletDisplay;
import com.payline.pmapi.bean.wallet.bean.field.WalletDisplayFieldText;
import com.payline.pmapi.bean.wallet.bean.field.WalletField;
import com.payline.pmapi.bean.wallet.bean.field.logo.WalletLogoResponseFile;
import com.payline.pmapi.bean.wallet.request.*;
import com.payline.pmapi.bean.wallet.response.*;
import com.payline.pmapi.bean.wallet.response.impl.WalletCreateResponseFailure;
import com.payline.pmapi.bean.wallet.response.impl.WalletCreateResponseSuccess;
import com.payline.pmapi.bean.wallet.response.impl.WalletDeleteResponseSuccess;
import com.payline.pmapi.logger.LogManager;
import com.payline.pmapi.service.WalletService;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class WalletServiceImpl implements WalletService {
    private static final Logger LOGGER = LogManager.getLogger(WalletServiceImpl.class);
    private RSAUtils rsaUtils = RSAUtils.getInstance();
    protected ConfigProperties config = ConfigProperties.getInstance();
    private JsonService jsonService = JsonService.getInstance();

    @Override
    public WalletDeleteResponse deleteWallet(WalletDeleteRequest walletDeleteRequest) {
        return WalletDeleteResponseSuccess.builder().build();
    }

    @Override
    public WalletUpdateResponse updateWallet(WalletUpdateRequest walletUpdateRequest) {
        // this function is not used yet
        return null;
    }

    @Override
    public WalletCreateResponse createWallet(WalletCreateRequest walletCreateRequest) {
        try {
            // get wallet data
            String bic = walletCreateRequest.getPaymentFormContext().getPaymentFormParameter().get(BankTransferForm.BANK_KEY);
            String iban = walletCreateRequest.getPaymentFormContext().getSensitivePaymentFormParameter().get(BankTransferForm.IBAN_KEY);

            PaymentData walletPaymentData = new PaymentData.PaymentDataBuilder()
                    .withBic(bic)
                    .withIban(iban)
                    .build();

            // encrypt the Json that contains the BIC and the IBAN
            final String keyProp = walletCreateRequest.getPartnerConfiguration()
                                                       .getProperty(Constants.PartnerConfigurationKeys.ENCRYPTION_KEY);

            if (keyProp == null) {
                throw new PluginException("Missing Encryption Key", FailureCause.INVALID_DATA);
            }
            
            String key = keyProp.trim();
            String paymentData = rsaUtils.encrypt(jsonService.toJson( walletPaymentData), key);

            // create wallet
            return WalletCreateResponseSuccess.builder()
                    .pluginPaymentData(paymentData)
                    .build();
        } catch (PluginException e) {
            LOGGER.warn("Unable to create wallet ", e);
            return WalletCreateResponseFailure.builder()
                    .errorCode(e.getErrorCode())
                    .failureCause(e.getFailureCause())
                    .build();
        } catch (RuntimeException e) {
            LOGGER.error("Unexpected plugin error", e);
            return WalletCreateResponseFailure.builder()
                    .errorCode(PluginException.runtimeErrorCode(e))
                    .failureCause(FailureCause.INTERNAL_ERROR)
                    .build();
        }
    }

    @Override
    public WalletDisplayResponse displayWallet(WalletDisplayRequest walletDisplayRequest) {
        List<WalletField> walletFields = new ArrayList<>();
        try {
            // decrypt the encrypted data (BIC + IBAN)
            String encryptedData = walletDisplayRequest.getWallet().getPluginPaymentData();

            String key = walletDisplayRequest.getPartnerConfiguration()
                    .getProperty(Constants.PartnerConfigurationKeys.ENCRYPTION_KEY);
            String data = rsaUtils.decrypt(encryptedData, key);

            //Build wallet display fields (BIC and the masked IBAN)
            Gson gson = new GsonBuilder().create();
            PaymentData paymentData = gson.fromJson(data, PaymentData.class);
            String bic = paymentData.getBic();
            String iban = paymentData.getIban();
            if (bic != null) {
                walletFields.add(WalletDisplayFieldText.builder().content(paymentData.getBic()).build());
            }
            if (iban != null) {
                walletFields.add(WalletDisplayFieldText.builder().content(PluginUtils.hideIban(paymentData.getIban())).build());
            }
        } catch (PluginException e) {
            LOGGER.warn("Unable to display wallet ", e);
        }
        // create and return walletDisplayResponse
        return WalletDisplay.builder()
                .walletFields(walletFields)
                .build();
    }

    @Override
    public boolean hasWalletDisplay(final WalletDisplayRequest walletDisplayRequest) {
        return true;
    }

    @Override
    public boolean hasCustomLogo(final WalletLogoRequest walletLogoRequest) {
        return true;
    }

    @Override
    public WalletLogoResponse getWalletLogo(final WalletLogoRequest walletLogoRequest) {
        return WalletLogoResponseFile.builder().ratio(
                Integer.parseInt(config.get("logoWallet.ratio"))).build();
    }
}
