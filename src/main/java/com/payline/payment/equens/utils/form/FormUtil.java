package com.payline.payment.equens.utils.form;

import com.payline.payment.equens.utils.constant.RequestContextKeys;
import com.payline.payment.equens.utils.i18n.I18nService;
import com.payline.pmapi.bean.payment.RequestContext;
import com.payline.pmapi.bean.payment.response.impl.PaymentResponseFormUpdated;
import com.payline.pmapi.bean.paymentform.bean.form.CustomForm;
import com.payline.pmapi.bean.paymentform.bean.form.IbanForm;
import com.payline.pmapi.bean.paymentform.response.configuration.PaymentFormConfigurationResponse;
import com.payline.pmapi.bean.paymentform.response.configuration.impl.PaymentFormConfigurationResponseSpecific;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class FormUtil {
    private final I18nService i18n = I18nService.getInstance();


    // --- Singleton Holder pattern + initialization BEGIN
    FormUtil() {
    }

    private static class Holder {
        private static final FormUtil instance = new FormUtil();
    }

    public static FormUtil getInstance() {
        return FormUtil.Holder.instance;
    }


    public PaymentResponseFormUpdated createIbanForm(String bank, Locale locale) {
        Map<String, String> requestData = new HashMap<>();
        requestData.put(RequestContextKeys.BANK, bank);

        RequestContext context = RequestContext.RequestContextBuilder
                .aRequestContext()
                .withRequestData(requestData)
                .build();

        CustomForm form = IbanForm.builder()
                .withDisplayButton(true)
                .withButtonText(i18n.getMessage("paymentForm.iban.buttonText", locale))
                .withDescription(i18n.getMessage("paymentForm.iban.description", locale))
                .withCustomFields(new ArrayList<>())
                .build();

        PaymentFormConfigurationResponse paymentFormConfigurationResponse = PaymentFormConfigurationResponseSpecific.PaymentFormConfigurationResponseSpecificBuilder
                .aPaymentFormConfigurationResponseSpecific()
                .withPaymentForm(form)
                .build();

        return PaymentResponseFormUpdated.PaymentResponseFormUpdatedBuilder
                .aPaymentResponseFormUpdated()
                .withRequestContext(context)
                .withPaymentFormConfigurationResponse(paymentFormConfigurationResponse)
                .build();
    }

}
