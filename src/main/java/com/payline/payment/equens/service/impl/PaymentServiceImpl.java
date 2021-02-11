package com.payline.payment.equens.service.impl;

import com.payline.payment.equens.bean.GenericPaymentRequest;
import com.payline.payment.equens.bean.business.payment.PaymentData;
import com.payline.payment.equens.bean.business.reachdirectory.Aspsp;
import com.payline.payment.equens.bean.business.reachdirectory.GetAspspsResponse;
import com.payline.payment.equens.exception.PluginException;
import com.payline.payment.equens.service.GenericPaymentService;
import com.payline.payment.equens.service.JsonService;
import com.payline.payment.equens.utils.PluginUtils;
import com.payline.payment.equens.utils.constant.RequestContextKeys;
import com.payline.payment.equens.utils.form.FormUtil;
import com.payline.pmapi.bean.common.FailureCause;
import com.payline.pmapi.bean.payment.request.PaymentRequest;
import com.payline.pmapi.bean.payment.response.PaymentResponse;
import com.payline.pmapi.bean.payment.response.impl.PaymentResponseFailure;
import com.payline.pmapi.bean.paymentform.bean.form.BankTransferForm;
import com.payline.pmapi.logger.LogManager;
import com.payline.pmapi.service.PaymentService;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.stream.Collectors;

public class PaymentServiceImpl implements PaymentService {
    private GenericPaymentService genericPaymentService = GenericPaymentService.getInstance();
    private static final Logger LOGGER = LogManager.getLogger(PaymentServiceImpl.class);
    JsonService jsonService = JsonService.getInstance();

    @Override
    public PaymentResponse paymentRequest(PaymentRequest paymentRequest) {
        PaymentResponse response;
        try {

            // check if the request already contains bank in requestContext
            String bank = paymentRequest.getRequestContext().getRequestData().get(RequestContextKeys.BANK);
            if (PluginUtils.isEmpty(bank)) {
                // No bank in requestContext, get it in the paymentFormContext
                bank = paymentRequest.getPaymentFormContext().getPaymentFormParameter().get(BankTransferForm.BANK_KEY);

                // check if this bank need an IBAN
                boolean isIBANNeeded = doesNeedIBAN(bank, paymentRequest.getPluginConfiguration());

                if (isIBANNeeded) {
                    // if the bank need an IBAN, return an IBAN form
                    response = FormUtil.getInstance().createIbanForm(bank, paymentRequest.getLocale());

                } else {
                    // the bank does not need the IBAN, continue payment process
                    PaymentData paymentData = new PaymentData.PaymentDataBuilder()
                            .withBic(bank)
                            .build();

                    GenericPaymentRequest genericPaymentRequest = new GenericPaymentRequest(paymentRequest);
                    response = genericPaymentService.paymentRequest(genericPaymentRequest, paymentData);
                }

            } else {
                // bank is in requestContext and needed an IBAN.
                // the IBAN is in paymentFormContext
                String iban = paymentRequest.getPaymentFormContext().getSensitivePaymentFormParameter().get(BankTransferForm.IBAN_KEY);

                PaymentData paymentData = new PaymentData.PaymentDataBuilder()
                        .withBic(bank)
                        .withIban(iban)
                        .build();
                GenericPaymentRequest genericPaymentRequest = new GenericPaymentRequest(paymentRequest);
                response = genericPaymentService.paymentRequest(genericPaymentRequest, paymentData);
            }

        } catch (RuntimeException e) {
            LOGGER.error("Unexpected plugin error", e);
            response = PaymentResponseFailure.PaymentResponseFailureBuilder
                    .aPaymentResponseFailure()
                    .withErrorCode(PluginException.runtimeErrorCode(e))
                    .withFailureCause(FailureCause.INTERNAL_ERROR)
                    .build();
        }

        return response;
    }


    /**
     * Check in pluginConfiguration if the chosen bank need an IBAN by checking if it contain a Detail with Fieldname=DebtorAccount and Type=Mandatory
     *
     * @param chosenBank the ban chosen by the buyer
     * @param pluginConfiguration configuration containing all available bank with details
     * @return
     */
    public boolean doesNeedIBAN(String chosenBank, String pluginConfiguration) {
        boolean ibanNeeded;

        if (PluginUtils.isEmpty(chosenBank) || PluginUtils.isEmpty(pluginConfiguration)) {
            ibanNeeded = false;
        } else {
            List<Aspsp> aspsps = jsonService.fromJson(PluginUtils.extractBanks(pluginConfiguration), GetAspspsResponse.class).getAspsps();

            // get the aspsp corresponding to the chosenBank name
            List<Aspsp> correspondingAspsps = aspsps.stream()
                    .filter(aspsp -> aspsp.getName().get(0).equals(chosenBank))
                    .collect(Collectors.toList());

            if (correspondingAspsps.isEmpty()) {
                // should not append as the bank is chosen from pluginConfiguration
                ibanNeeded = false;
            } else {
                // check if a details says that the debtorAccount is MANDATORY
                ibanNeeded = correspondingAspsps.get(0).getDetails().stream()
                        .anyMatch(detail -> detail.getFieldName().equalsIgnoreCase("DebtorAccount")
                                && detail.getType().equalsIgnoreCase("MANDATORY"));
            }
        }
        return ibanNeeded;
    }


}
