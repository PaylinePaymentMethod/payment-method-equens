package com.payline.payment.equens.service.impl;

import com.payline.payment.equens.bean.business.reachdirectory.Aspsp;
import com.payline.payment.equens.exception.InvalidDataException;
import com.payline.payment.equens.exception.PluginException;
import com.payline.payment.equens.service.BankService;
import com.payline.payment.equens.service.LogoPaymentFormConfigurationService;
import com.payline.payment.equens.utils.Constants;
import com.payline.payment.equens.utils.PluginUtils;
import com.payline.pmapi.bean.common.FailureCause;
import com.payline.pmapi.bean.payment.ContractProperty;
import com.payline.pmapi.bean.paymentform.bean.field.PaymentFormDisplayFieldText;
import com.payline.pmapi.bean.paymentform.bean.field.PaymentFormField;
import com.payline.pmapi.bean.paymentform.bean.field.PaymentFormInputFieldSelect;
import com.payline.pmapi.bean.paymentform.bean.field.SelectOption;
import com.payline.pmapi.bean.paymentform.bean.field.specific.PaymentFormInputFieldIban;
import com.payline.pmapi.bean.paymentform.bean.form.BankTransferForm;
import com.payline.pmapi.bean.paymentform.bean.form.CustomForm;
import com.payline.pmapi.bean.paymentform.request.PaymentFormConfigurationRequest;
import com.payline.pmapi.bean.paymentform.response.configuration.PaymentFormConfigurationResponse;
import com.payline.pmapi.bean.paymentform.response.configuration.impl.PaymentFormConfigurationResponseFailure;
import com.payline.pmapi.bean.paymentform.response.configuration.impl.PaymentFormConfigurationResponseSpecific;
import com.payline.pmapi.logger.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static com.payline.payment.equens.utils.Constants.FormKeys.*;

public class PaymentFormConfigurationServiceImpl extends LogoPaymentFormConfigurationService {

    private static final Logger LOGGER = LogManager.getLogger(PaymentFormConfigurationServiceImpl.class);

    private static final String EQUENS_FILE_SCRIPT = "equensForm.js";

    private BankService bankService = BankService.getInstance();

    @Override
    public PaymentFormConfigurationResponse getPaymentFormConfiguration(PaymentFormConfigurationRequest paymentFormConfigurationRequest) {
        PaymentFormConfigurationResponse pfcResponse;
        final List<String> listCountryCode;
        final ContractProperty paymentModeProperty;
        try {
            final Locale locale = paymentFormConfigurationRequest.getLocale();

            // build the banks list from the plugin configuration
            if (paymentFormConfigurationRequest.getPluginConfiguration() == null) {
                throw new InvalidDataException("Plugin configuration must not be null");
            }
            // check if the string who contain the list of country is empty
            final String countries = paymentFormConfigurationRequest.getContractConfiguration().getProperty(Constants.ContractConfigurationKeys.COUNTRIES).getValue();
            if (PluginUtils.isEmpty(countries)) {
                throw new InvalidDataException("country must not be empty");
            }

            listCountryCode = PluginUtils.createListCountry(countries);

            paymentModeProperty = paymentFormConfigurationRequest
                    .getContractConfiguration().getProperty(Constants.ContractConfigurationKeys.PAYMENT_PRODUCT);

            if (paymentModeProperty == null || PluginUtils.isEmpty(paymentModeProperty.getValue())) {
                throw new InvalidDataException("Payment product must not be empty");
            }

            final List<Aspsp> banksList = bankService.fetchBanks(paymentFormConfigurationRequest.getPluginConfiguration(), listCountryCode, paymentModeProperty.getValue());

            final List<SelectOption> bankOptionsList = new ArrayList<>();
            final List<SelectOption> bankSubsidiairesList = new ArrayList<>();
            final String bankJSScript = buildBankScript(banksList, bankOptionsList, bankSubsidiairesList);
            final String errorSubidiariesMsg = i18n.getMessage(FIELD_SUBSIDIARY_REQUIRED_MSG, locale);

            final Map<String,String> arguments = new HashMap<>();
            arguments.put("$BANKS_TO_REPLACE$", bankJSScript);
            arguments.put("$ASPSP_MSG$", escapeJSVar(errorSubidiariesMsg));

            final List<PaymentFormField> customFields = new ArrayList<>();

            // Champ IBAN
            final PaymentFormInputFieldIban ibanField = PaymentFormInputFieldIban.IbanFieldBuilder.anIbanField()
                    .withKey(BankTransferForm.IBAN_KEY)
                    .withLabel(i18n.getMessage(FIELD_IBAN_LABEL, locale))
                    .withRequired(false)
                    .build();
            customFields.add(ibanField);

            // Champ OU
            final PaymentFormDisplayFieldText separatorField = PaymentFormDisplayFieldText.PaymentFormDisplayFieldTextBuilder
                    .aPaymentFormDisplayFieldText()
                    .withContent(i18n.getMessage(FIELD_OR_LABEL, locale))
                    .build();
            customFields.add(separatorField);

            // Champ de selection de banque
            final PaymentFormInputFieldSelect selectField = PaymentFormInputFieldSelect.PaymentFormFieldSelectBuilder.aPaymentFormInputFieldSelect()
                    .withSelectOptions(bankOptionsList)
                    .withIsFilterable(true)
                    .withKey(Constants.FormKeys.ASPSP_ID)
                    .withLabel(i18n.getMessage(FIELD_BANKS_LABEL, locale))
                    .withPlaceholder(i18n.getMessage(FIELD_PLACEHOLDER_LABEL, locale))
                    .withValidationErrorMessage(i18n.getMessage(FIELD_BANKS_ERROR_MSG, locale))
                    .withRequired(true)
                    .withRequiredErrorMessage(i18n.getMessage(FIELD_BANKS_REQUIRED_MSG, locale))
                    .build();
            customFields.add(selectField);

            final PaymentFormInputFieldSelect selectSubsidiaries = PaymentFormInputFieldSelect.PaymentFormFieldSelectBuilder.aPaymentFormInputFieldSelect()
                    .withSelectOptions(bankSubsidiairesList)
                    .withIsFilterable(true)
                    .withKey(Constants.FormKeys.SUB_ASPSP_ID)
                    .withLabel(i18n.getMessage(FIELD_SUBSIDIARY_LABEL, locale))
                    .withPlaceholder(i18n.getMessage(FIELD_SUBSIDIARY_PLACEHOLDER, locale))
                    .withValidationErrorMessage(i18n.getMessage(FIELD_SUBSIDIARY_ERROR_MSG, locale))
                    .withRequired(false).build();

            customFields.add(selectSubsidiaries);

            // Build the payment form
            final CustomForm form = CustomForm.builder()
                    .withDescription(i18n.getMessage("paymentForm.description", locale))
                    .withDisplayButton(true)
                    .withButtonText(i18n.getMessage("paymentForm.buttonText", locale))
                    .withCustomFields(customFields)
                    .withFormScript(loadScript(arguments))
                    .build();

            pfcResponse = PaymentFormConfigurationResponseSpecific.PaymentFormConfigurationResponseSpecificBuilder
                    .aPaymentFormConfigurationResponseSpecific()
                    .withPaymentForm(form)
                    .build();
        } catch (PluginException e) {
            pfcResponse = e.toPaymentFormConfigurationResponseFailureBuilder().build();
        } catch (RuntimeException e) {
            LOGGER.error("Unexpected plugin error", e);
            pfcResponse = PaymentFormConfigurationResponseFailure.PaymentFormConfigurationResponseFailureBuilder
                    .aPaymentFormConfigurationResponseFailure()
                    .withErrorCode(PluginException.runtimeErrorCode(e))
                    .withFailureCause(FailureCause.INTERNAL_ERROR)
                    .build();
        }

        return pfcResponse;
    }

    /**
     * Method used to load JQuery script for Equens form.
     * @param arguments list of arguments to inject to script.
     * @return script with arguments.
     */
    protected String loadScript(Map<String, String> arguments) {

        try (InputStream input = this.getClass().getClassLoader().getResourceAsStream(EQUENS_FILE_SCRIPT)) {
            if (input == null) {
                LOGGER.error("Unable to load file {}", EQUENS_FILE_SCRIPT);
                throw new PluginException("Plugin error: unable to load equens js file");
            }

             String script = new BufferedReader(
                    new InputStreamReader(input, StandardCharsets.UTF_8)).lines()
                    .collect(Collectors.joining("\n"));

            for (Map.Entry<String, String> entry : arguments.entrySet()) {
                script = script.replace(entry.getKey(), entry.getValue());
            }

            return script;
        } catch (final IOException e) {
            throw new PluginException("Plugin error: unable to read the logo", e);
        }
    }


    protected String buildBankScript(final List<Aspsp> banksList, final List<SelectOption> bankOptionsList, final List<SelectOption> bankSubsidiairesList) {
        List<String> bankJSList = new ArrayList<>();
        banksList.forEach(e -> {
                bankOptionsList.add(buildAspspOption(e));

                if (!PluginUtils.isEmptyList(e.getSubsidiariesList())) {
                    String temp = "{ id : '" + escapeJSVar(e.getName().get(0))+ "',\n" +
                            "  subList : [";
                    List<String> subsidiariesJSScript = new ArrayList<>();
                    e.getSubsidiariesList().forEach(sub -> {
                        bankSubsidiairesList.add(buildAspspOption(sub));
                        subsidiariesJSScript.add(buildSubsidiariesJS(sub));
                    });
                    temp += String.join(",", subsidiariesJSScript ) + "]}";
                    bankJSList.add(temp);
                }
            });
        return String.join(",", bankJSList);
    }

    /**
     * Method used to build subsidiaries information for JS file.
     * @param sub
     *          Aspsp subsidiary
     * @return
     *          Subsidiary info for JS Script.
     */
    protected String buildSubsidiariesJS(final Aspsp sub) {
        final String name = PluginUtils.isEmptyList(sub.getName()) ? "" : sub.getName().get(0);
        return "{aspspId : '" + sub.getAspspId() + "'" +
                ", label : '" + escapeJSVar(name) + "'}";
    }



    /**
     * Build a SelectOption object from aspsp information
     * @param aspsp
     *          Aspsp object which contains information.
     * @return
     *        selectOption build.
     */
    protected SelectOption buildAspspOption(Aspsp aspsp) {
        // add the aspsp name if exists
        final String name = PluginUtils.isEmptyList(aspsp.getName()) ? "" : aspsp.getName().get(0);
        return SelectOption.SelectOptionBuilder.aSelectOption()
                .withKey(aspsp.getAspspId())
                .withValue(name)
                .build();
    }

    protected String escapeJSVar(String varToEscape){
        return varToEscape.replace("'", "\\'");
    }
}
