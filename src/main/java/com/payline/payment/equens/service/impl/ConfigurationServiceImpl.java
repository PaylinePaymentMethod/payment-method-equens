package com.payline.payment.equens.service.impl;

import com.payline.payment.equens.bean.business.reachdirectory.GetAspspsResponse;
import com.payline.payment.equens.bean.configuration.RequestConfiguration;
import com.payline.payment.equens.exception.PluginException;
import com.payline.payment.equens.service.JsonService;
import com.payline.payment.equens.utils.Constants;
import com.payline.payment.equens.utils.PluginUtils;
import com.payline.payment.equens.utils.http.PisHttpClient;
import com.payline.payment.equens.utils.http.PsuHttpClient;
import com.payline.payment.equens.utils.i18n.I18nService;
import com.payline.payment.equens.utils.properties.ReleaseProperties;
import com.payline.pmapi.bean.configuration.PartnerConfiguration;
import com.payline.pmapi.bean.configuration.ReleaseInformation;
import com.payline.pmapi.bean.configuration.parameter.AbstractParameter;
import com.payline.pmapi.bean.configuration.parameter.impl.InputParameter;
import com.payline.pmapi.bean.configuration.parameter.impl.ListBoxParameter;
import com.payline.pmapi.bean.configuration.request.ContractParametersCheckRequest;
import com.payline.pmapi.bean.configuration.request.RetrievePluginConfigurationRequest;
import com.payline.pmapi.bean.payment.ContractConfiguration;
import com.payline.pmapi.bean.payment.ContractProperty;
import com.payline.pmapi.logger.LogManager;
import com.payline.pmapi.service.ConfigurationService;
import org.apache.logging.log4j.Logger;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ConfigurationServiceImpl implements ConfigurationService {

    private static final Logger LOGGER = LogManager.getLogger(ConfigurationServiceImpl.class);

    private static final String I18N_CONTRACT_PREFIX = "contract.";

    private final JsonService jsonService = JsonService.getInstance();

    public enum ChannelType {
        ECOMMERCE("Ecommerce");


        private final String type;

        ChannelType(final String type) {
            this.type = type;
        }

        public String getType() {
            return type;
        }
    }

    public enum ChargeBearer {
        CRED("CRED"), DEBT("DEBT"), SHAR("SHAR"), SLEV("SLEV");

        private final String bearer;

        ChargeBearer(final String bearer) {
            this.bearer = bearer;
        }

        public String getBearer() {
            return bearer;
        }
    }

    public enum PaymentProduct {
        NORMAL("Normal", true), INSTANT("Instant", false);

        private final String paymentProductCode;
        private final Boolean supportedByDefault;
        PaymentProduct(String paymentProductCode, Boolean supportedByDefault) {
            this.paymentProductCode = paymentProductCode;
            this.supportedByDefault = supportedByDefault;
        }

        public String getPaymentProductCode() {
            return paymentProductCode;
        }

        public Boolean getSupportedByDefault() {
            return supportedByDefault;
        }
    }

    public enum PurposeCode {
        CARPARK("Carpark"), COMMERCE("Commerce"), TRANSPORT("Transport");

        private final String code;

        PurposeCode(final String code) {
            this.code = code;
        }

        public String getCode() {
            return code;
        }
    }

    public enum CountryCode {
        FR, ES, ALL
    }

    public static final class ScaMethod {
        private ScaMethod() {
        } // private constructor to hide the implicit public one (Sonarqube issue)

        public static final String REDIRECT = "Redirect";
    }

    private I18nService i18n = I18nService.getInstance();
    private PisHttpClient pisHttpClient = PisHttpClient.getInstance();
    private PsuHttpClient psuHttpClient = PsuHttpClient.getInstance();
    private ReleaseProperties releaseProperties = ReleaseProperties.getInstance();


    @Override
    public List<AbstractParameter> getParameters(final Locale locale) {
        final List<AbstractParameter> parameters = new ArrayList<>();

        // Client name
        parameters.add(this.newInputParameter(Constants.ContractConfigurationKeys.CLIENT_NAME, true, locale));

        // Onboarding ID
        parameters.add(this.newInputParameter(Constants.ContractConfigurationKeys.ONBOARDING_ID, true, locale));

        // merchant iban
        parameters.add(this.newInputParameter(Constants.ContractConfigurationKeys.MERCHANT_IBAN, false, locale));

        // merchant name
        parameters.add(this.newInputParameter(Constants.ContractConfigurationKeys.MERCHANT_NAME, false, locale));

        //Choix du paiement mode (Normal/Instant).
        final Map<String, String> paymentProduct = new HashMap<>();
        paymentProduct.put(PaymentProduct.NORMAL.getPaymentProductCode(), PaymentProduct.NORMAL.getPaymentProductCode());
        paymentProduct.put(PaymentProduct.INSTANT.getPaymentProductCode(), PaymentProduct.INSTANT.getPaymentProductCode());
        parameters.add(this.newListBoxParameter(Constants.ContractConfigurationKeys.PAYMENT_PRODUCT, paymentProduct, PaymentProduct.NORMAL.getPaymentProductCode(), true, locale));

        // channel type
        final Map<String, String> channelTypes = new HashMap<>();
        channelTypes.put(ChannelType.ECOMMERCE.getType(), ChannelType.ECOMMERCE.getType());
        parameters.add(this.newListBoxParameter(Constants.ContractConfigurationKeys.CHANNEL_TYPE, channelTypes, ChannelType.ECOMMERCE.getType(), true, locale));

        // SCA method
        final Map<String, String> scaMethods = new HashMap<>();
        scaMethods.put(ScaMethod.REDIRECT, ScaMethod.REDIRECT);
        parameters.add(this.newListBoxParameter(Constants.ContractConfigurationKeys.SCA_METHOD, scaMethods, ScaMethod.REDIRECT, true, locale));

        // Charge bearer
        final Map<String, String> chargeBearers = new HashMap<>();
        chargeBearers.put(ChargeBearer.CRED.getBearer(), ChargeBearer.CRED.getBearer());
        chargeBearers.put(ChargeBearer.DEBT.getBearer(), ChargeBearer.DEBT.getBearer());
        chargeBearers.put(ChargeBearer.SHAR.getBearer(), ChargeBearer.SHAR.getBearer());
        chargeBearers.put(ChargeBearer.SLEV.getBearer(), ChargeBearer.SLEV.getBearer());
        parameters.add(this.newListBoxParameter(Constants.ContractConfigurationKeys.CHARGE_BEARER, chargeBearers, ChargeBearer.SLEV.getBearer(), true, locale));

        // purpose code
        final Map<String, String> purposeCodes = new HashMap<>();
        purposeCodes.put(PurposeCode.CARPARK.getCode(), PurposeCode.CARPARK.getCode());
        purposeCodes.put(PurposeCode.COMMERCE.getCode(), PurposeCode.COMMERCE.getCode());
        purposeCodes.put(PurposeCode.TRANSPORT.getCode(), PurposeCode.TRANSPORT.getCode());
        parameters.add(this.newListBoxParameter(Constants.ContractConfigurationKeys.PURPOSE_CODE, purposeCodes, PurposeCode.COMMERCE.getCode(), true, locale));

        // Create a listBox who display countries accepted by the API
        final Map<String, String> countryCode = new HashMap<>();
        countryCode.put(CountryCode.FR.name(), i18n.getMessage("countryCode.fr", locale));
        countryCode.put(CountryCode.ES.name(), i18n.getMessage("countryCode.es", locale));
        countryCode.put(CountryCode.ALL.name(), i18n.getMessage("countryCode.all", locale));
        parameters.add(this.newListBoxParameter(Constants.ContractConfigurationKeys.COUNTRIES, countryCode, CountryCode.ALL.name(), true, locale));

        // PISP contract
        parameters.add(this.newInputParameter(Constants.ContractConfigurationKeys.PISP_CONTRACT, true, locale));

        //InitiatingPartySubId
        parameters.add(this.newInputParameter(Constants.ContractConfigurationKeys.INITIATING_PARTY_SUBID, false, locale));

        return parameters;
    }

    @Override
    public Map<String, String> check(final ContractParametersCheckRequest contractParametersCheckRequest) {
        final Map<String, String> errors = new HashMap<>();

        final Map<String, String> accountInfo = contractParametersCheckRequest.getAccountInfo();
        final Locale locale = contractParametersCheckRequest.getLocale();

        // check required fields
        for (final AbstractParameter param : this.getParameters(locale)) {
            if (param.isRequired() && accountInfo.get(param.getKey()) == null) {
                final String message = i18n.getMessage(I18N_CONTRACT_PREFIX + param.getKey() + ".requiredError", locale);
                errors.put(param.getKey(), message);
            }
        }

        // check PISP format (N12)
        final String pispContract = accountInfo.get(Constants.ContractConfigurationKeys.PISP_CONTRACT);
        if (PluginUtils.isEmpty(pispContract) || pispContract.length() > 12 || !PluginUtils.isNumeric(pispContract)) {
            final String message = i18n.getMessage(I18N_CONTRACT_PREFIX + Constants.ContractConfigurationKeys.PISP_CONTRACT + ".badFormat", locale);
            errors.put(Constants.ContractConfigurationKeys.PISP_CONTRACT, message);
        }

        String initiatingPartySubId = accountInfo.get(Constants.ContractConfigurationKeys.INITIATING_PARTY_SUBID);
        //check InitiatingPartySubId length
        if (!PluginUtils.isEmpty(initiatingPartySubId) && initiatingPartySubId.length() > 50) {
            String message = i18n.getMessage(I18N_CONTRACT_PREFIX + Constants.ContractConfigurationKeys.INITIATING_PARTY_SUBID + ".badFormat", locale);
            errors.put(Constants.ContractConfigurationKeys.INITIATING_PARTY_SUBID, message);
        }

        // Check the clientName and onboarding ID
        final String clientNameKey = Constants.ContractConfigurationKeys.CLIENT_NAME;
        final String onboardingIdKey = Constants.ContractConfigurationKeys.ONBOARDING_ID;

        // If one of them is missing, no need to go further as they are both required to get an access token
        if (errors.containsKey(clientNameKey) || errors.containsKey(onboardingIdKey)) {
            return errors;
        }

        // We first need to replace these 2 values in the ContractConfiguration (to override the former validated values)
        final RequestConfiguration altRequestConfiguration = RequestConfiguration.build(contractParametersCheckRequest);
        final Map<String, ContractProperty> contractProperties = altRequestConfiguration.getContractConfiguration().getContractProperties();
        contractProperties.put(clientNameKey, new ContractProperty(accountInfo.get(clientNameKey)));
        contractProperties.put(onboardingIdKey, new ContractProperty(accountInfo.get(onboardingIdKey)));

        // Validate the merchant account on the 2 APIs (PIS and PSU) because they have separate subscriptions
        try {
            pisHttpClient.init(contractParametersCheckRequest.getPartnerConfiguration());
            pisHttpClient.authorize(altRequestConfiguration);

            psuHttpClient.init(contractParametersCheckRequest.getPartnerConfiguration());
            psuHttpClient.authorize(altRequestConfiguration);
        } catch (final PluginException e) {
            errors.put(clientNameKey, e.getMessage());
            errors.put(onboardingIdKey, "");
        }

        return errors;
    }

    @Override
    public String retrievePluginConfiguration(final RetrievePluginConfigurationRequest retrievePluginConfigurationRequest) {
        try {
            /*
            This method is frequently called by a batch.
            In the context of this call, there is no current transaction, so no merchant contract identified.
            So the ContractConfiguration contained in the request might be null. Or its content might be irrelevant.
            Still, the signature algorithm will need a clientName and an onboardingId (@see EquensHttpClient#authorizationHeaders).
            We create here a fake ContractConfiguration, containing specific onboardingId and a clientName from the
            PartnerConfiguration. This way, there is no need to duplicate ay of the code in the HTTP client.
             */
            final PartnerConfiguration partnerConfiguration = retrievePluginConfigurationRequest.getPartnerConfiguration();
            if (partnerConfiguration.getProperty(Constants.PartnerConfigurationKeys.PAYLINE_CLIENT_NAME) == null) {
                throw new PluginException("Missing Payline clientName from partner configuration");
            }
            if (partnerConfiguration.getProperty(Constants.PartnerConfigurationKeys.PAYLINE_ONBOARDING_ID) == null) {
                throw new PluginException("Missing Payline onboardingId from partner configuration");
            }
            final Map<String, ContractProperty> contractProperties = new HashMap<>();
            contractProperties.put(Constants.ContractConfigurationKeys.CLIENT_NAME,
                    new ContractProperty(partnerConfiguration.getProperty(Constants.PartnerConfigurationKeys.PAYLINE_CLIENT_NAME)));
            contractProperties.put(Constants.ContractConfigurationKeys.ONBOARDING_ID,
                    new ContractProperty(partnerConfiguration.getProperty(Constants.PartnerConfigurationKeys.PAYLINE_ONBOARDING_ID)));
            final ContractConfiguration contractConfiguration = new ContractConfiguration("fake contract", contractProperties);

            final RequestConfiguration requestConfiguration = new RequestConfiguration(
                    contractConfiguration,
                    retrievePluginConfigurationRequest.getEnvironment(),
                    retrievePluginConfigurationRequest.getPartnerConfiguration());

            // Init HTTP client
            pisHttpClient.init(requestConfiguration.getPartnerConfiguration());

            // Retrieve account service providers list
            final GetAspspsResponse apspsps = pisHttpClient.getAspsps(requestConfiguration);

            // Serialize the list (as JSON)
            return jsonService.toJson(apspsps);

        } catch (RuntimeException e) {
            LOGGER.error("Could not retrieve plugin configuration due to a plugin error", e);
            return retrievePluginConfigurationRequest.getPluginConfiguration();
        }
    }

    @Override
    public ReleaseInformation getReleaseInformation() {
        return ReleaseInformation.ReleaseBuilder.aRelease()
                                                .withDate(LocalDate.parse(releaseProperties.get("release.date"), DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                                                .withVersion(releaseProperties.get("release.version"))
                                                .build();
    }

    @Override
    public String getName(final Locale locale) {
        return i18n.getMessage("paymentMethod.name", locale);
    }

    /**
     * Build and return a new <code>InputParameter</code> for the contract configuration.
     *
     * @param key      The parameter key
     * @param required Is this parameter required ?
     * @param locale   The current locale
     * @return The new input parameter
     */
    private InputParameter newInputParameter(final String key, final boolean required, final Locale locale) {
        final InputParameter inputParameter = new InputParameter();
        inputParameter.setKey(key);
        inputParameter.setLabel(i18n.getMessage(I18N_CONTRACT_PREFIX + key + ".label", locale));
        inputParameter.setDescription(i18n.getMessage(I18N_CONTRACT_PREFIX + key + ".description", locale));
        inputParameter.setRequired(required);
        return inputParameter;
    }

    /**
     * Build and return a new <code>ListBoxParameter</code> for the contract configuration.
     *
     * @param key          The parameter key
     * @param values       All the possible values for the list box
     * @param defaultValue The key of the default value (which will be selected by default)
     * @param required     Is this parameter required ?
     * @param locale       The current locale
     * @return The new list box parameter
     */
    private ListBoxParameter newListBoxParameter(final String key, final Map<String, String> values, final String defaultValue, final boolean required, final Locale locale) {
        final ListBoxParameter listBoxParameter = new ListBoxParameter();
        listBoxParameter.setKey(key);
        listBoxParameter.setLabel(i18n.getMessage(I18N_CONTRACT_PREFIX + key + ".label", locale));
        listBoxParameter.setDescription(i18n.getMessage(I18N_CONTRACT_PREFIX + key + ".description", locale));
        listBoxParameter.setList(values);
        listBoxParameter.setRequired(required);
        listBoxParameter.setValue(defaultValue);
        return listBoxParameter;
    }

}


