package com.payline.payment.equens.service;

import com.payline.payment.equens.bean.GenericPaymentRequest;
import com.payline.payment.equens.bean.business.payment.*;
import com.payline.payment.equens.bean.business.reachdirectory.Aspsp;
import com.payline.payment.equens.bean.business.reachdirectory.GetAspspsResponse;
import com.payline.payment.equens.bean.configuration.RequestConfiguration;
import com.payline.payment.equens.exception.InvalidDataException;
import com.payline.payment.equens.exception.PluginException;
import com.payline.payment.equens.utils.Constants;
import com.payline.payment.equens.utils.PluginUtils;
import com.payline.payment.equens.utils.http.PisHttpClient;
import com.payline.pmapi.bean.common.Amount;
import com.payline.pmapi.bean.common.Buyer;
import com.payline.pmapi.bean.common.FailureCause;
import com.payline.pmapi.bean.payment.ContractProperty;
import com.payline.pmapi.bean.payment.RequestContext;
import com.payline.pmapi.bean.payment.response.PaymentResponse;
import com.payline.pmapi.bean.payment.response.impl.PaymentResponseFailure;
import com.payline.pmapi.bean.payment.response.impl.PaymentResponseRedirect;
import com.payline.pmapi.logger.LogManager;
import org.apache.http.Header;
import org.apache.http.message.BasicHeader;
import org.apache.logging.log4j.Logger;

import java.math.BigInteger;
import java.util.*;

public class GenericPaymentService {
    private static final Logger LOGGER = LogManager.getLogger(GenericPaymentService.class);

    public static final String HTTP_HEADER_USER_AGENT = "HttpHeaderUserAgent";
    public static final String PSU_IP_ADDRESS = "PsuIpAddress";

    private PisHttpClient pisHttpClient = PisHttpClient.getInstance();
    private static JsonService jsonService = JsonService.getInstance();
    private BankService bankService = BankService.getInstance();

    private GenericPaymentService() {
    }

    private static class Holder {
        private static final GenericPaymentService instance = new GenericPaymentService();
    }

    public static GenericPaymentService getInstance() {
        return Holder.instance;
    }


    public PaymentResponse paymentRequest(GenericPaymentRequest paymentRequest, PaymentData paymentData) {
        PaymentResponse paymentResponse;

        try {
            // Control on the input data (to avoid NullPointerExceptions)
            if (paymentRequest.getAmount() == null
                    || paymentRequest.getAmount().getCurrency() == null
                    || paymentRequest.getAmount().getAmountInSmallestUnit() == null) {
                throw new InvalidDataException("Missing or invalid paymentRequest.amount");
            }
            if (paymentRequest.getOrder() == null) {
                throw new InvalidDataException("paymentRequest.order is required");
            }

            // Build request configuration
            RequestConfiguration requestConfiguration = new RequestConfiguration(
                    paymentRequest.getContractConfiguration()
                    , paymentRequest.getEnvironment()
                    , paymentRequest.getPartnerConfiguration()
            );

            // Init HTTP clients
            pisHttpClient.init(paymentRequest.getPartnerConfiguration());

            // Check required contract properties
            List<String> requiredContractProperties = Arrays.asList(
                    Constants.ContractConfigurationKeys.CHANNEL_TYPE,
                    Constants.ContractConfigurationKeys.CHARGE_BEARER,
                    Constants.ContractConfigurationKeys.MERCHANT_IBAN,
                    Constants.ContractConfigurationKeys.MERCHANT_NAME,
                    Constants.ContractConfigurationKeys.PURPOSE_CODE,
                    Constants.ContractConfigurationKeys.SCA_METHOD,
                    Constants.ContractConfigurationKeys.COUNTRIES
            );
            requiredContractProperties.forEach(key -> {
                if (paymentRequest.getContractConfiguration().getProperty(key) == null
                        || paymentRequest.getContractConfiguration().getProperty(key).getValue() == null) {
                    throw new InvalidDataException("\"" + key + "\" missing from contract properties");
                }
            });

            List<Header> headers = new ArrayList<>();

            final String userIpAddress = paymentRequest.getBrowser() != null ? paymentRequest.getBrowser().getIp() : null;
            final String headerUserAgent  = paymentRequest.getBrowser() != null ? paymentRequest.getBrowser().getUserAgent() : null;

            if (headerUserAgent != null) {
                headers.add(new BasicHeader(HTTP_HEADER_USER_AGENT, headerUserAgent));
            }
            if (userIpAddress != null) {
                headers.add(new BasicHeader(PSU_IP_ADDRESS, userIpAddress));
            }

            // Build PaymentInitiationRequest (Equens) from PaymentRequest (Payline)
            PaymentInitiationRequest request = buildPaymentInitiationRequest(paymentRequest, paymentData);

            // Send the payment initiation request
            PaymentInitiationResponse paymentInitResponse = pisHttpClient.initPayment(request, requestConfiguration, headers);

            // URL
            PaymentResponseRedirect.RedirectionRequest.RedirectionRequestBuilder redirectionRequestBuilder = PaymentResponseRedirect.RedirectionRequest.RedirectionRequestBuilder.aRedirectionRequest()
                    .withRequestType(PaymentResponseRedirect.RedirectionRequest.RequestType.GET)
                    .withUrl(paymentInitResponse.getLinks().getAspspRedirectUrl().getHref());

            // request context
            Map<String, String> requestData = new HashMap<>();
            requestData.put(Constants.RequestContextKeys.PAYMENT_ID, paymentInitResponse.getPaymentId());
            RequestContext requestContext = RequestContext.RequestContextBuilder.aRequestContext()
                    .withRequestData(requestData)
                    .withSensitiveRequestData(new HashMap<>())
                    .build();

            // Build PaymentResponse
            paymentResponse = PaymentResponseRedirect.PaymentResponseRedirectBuilder.aPaymentResponseRedirect()
                    .withPartnerTransactionId(paymentInitResponse.getPaymentId())
                    .withStatusCode(paymentInitResponse.getPaymentStatus().name())
                    .withRedirectionRequest(redirectionRequestBuilder.build())
                    .withRequestContext(requestContext)
                    .build();
        } catch (PluginException e) {
            paymentResponse = e.toPaymentResponseFailureBuilder().build();
        } catch (RuntimeException e) {
            LOGGER.error("Unexpected plugin error", e);
            paymentResponse = PaymentResponseFailure.PaymentResponseFailureBuilder
                    .aPaymentResponseFailure()
                    .withErrorCode(PluginException.runtimeErrorCode(e))
                    .withFailureCause(FailureCause.INTERNAL_ERROR)
                    .build();
        }

        return paymentResponse;
    }

    /**
     * Build a Equens <code>Address</code> from a Payline <code>Buyer.Address</code>.
     * In particular, manage the length of the address lines between Payline and Equens.
     *
     * @param paylineAddress the Payline address
     * @return The corresponding Equens address.
     */
    Address buildAddress(Buyer.Address paylineAddress) {
        if (paylineAddress == null) {
            return null;
        }

        List<String> lines = new ArrayList<>();

        // concat the full address line to split
        String toSplit = paylineAddress.getStreet1();
        if (paylineAddress.getStreet2() != null && !paylineAddress.getStreet2().isEmpty()) {
            toSplit += " " + paylineAddress.getStreet2();
        }

        // Split it in chunks of the max allowed length for an address line
        while (!toSplit.isEmpty()) {
            int chunkLength = Math.min(Address.ADDRESS_LINE_MAX_LENGTH, toSplit.length());

            // look for the first whitespace from the end of the chunk, to truncate properly
            int i = chunkLength;
            while (i > 0 && !Character.isWhitespace(toSplit.charAt(i - 1))) {
                i--;
            }
            // no whitespace: truncate brutally
            if (i == 0) {
                i = chunkLength;
            }
            // add the new line and remove the chunk from the initial string
            lines.add(toSplit.substring(0, i).trim());
            toSplit = i == toSplit.length() ? "" : toSplit.substring(i).trim();
        }

        return new Address.AddressBuilder()
                .withAddressLines(lines)
                .withPostCode(paylineAddress.getZipCode())
                .withTownName(paylineAddress.getCity())
                .withCountry(paylineAddress.getCountry())
                .build();
    }

    void validateRequest(GenericPaymentRequest paymentRequest) {
        // Control on the input data (to avoid NullPointerExceptions)
        if (paymentRequest.getAmount() == null
                || paymentRequest.getAmount().getCurrency() == null
                || paymentRequest.getAmount().getAmountInSmallestUnit() == null) {
            throw new InvalidDataException("Missing or invalid paymentRequest.amount");
        }
        if (paymentRequest.getOrder() == null) {
            throw new InvalidDataException("paymentRequest.order is required");
        }

        if (paymentRequest.getBuyer() == null || paymentRequest.getBuyer().getFullName() == null){
            throw new InvalidDataException("paymentRequest.buyer is required");
        }
    }

    void validPaymentData(final PaymentData paymentData, final String pluginConfiguration, final GenericPaymentRequest paymentRequest) {
        // get the list of countryCode available by the merchant
        final List<String> listCountryCode = PluginUtils.createListCountry(
                paymentRequest.getContractConfiguration().getProperty(Constants.ContractConfigurationKeys.COUNTRIES).getValue());
        final String paymentMode = paymentRequest.getContractConfiguration().getProperty(Constants.ContractConfigurationKeys.PAYMENT_PRODUCT).getValue();
        final String aspspId = paymentData.getAspspId();

        if (PluginUtils.isEmpty(aspspId)) {
            throw new InvalidDataException("AspspId is required");
        }

        final Aspsp aspsp = bankService.getAspsp(pluginConfiguration, aspspId);
        if (aspsp == null) {
            throw new InvalidDataException("Aspsp not found");
        }

        if (!listCountryCode.isEmpty() && !listCountryCode.contains(aspsp.getCountryCode())) {
            throw new InvalidDataException("Aspsp not available for this country");
        }

        if (!bankService.isCompatibleBank(aspsp, paymentMode)) {
            throw new InvalidDataException("Aspsp not compatible with this payment mode");
        }
        if (bankService.isIbanRequired(aspsp)) {
            validateIban(paymentRequest, paymentData, aspsp, listCountryCode);
        }

    }

    void validateIban(GenericPaymentRequest paymentRequest, PaymentData paymentData, Aspsp aspsp, final List<String> listCountryCode) {

        if (PluginUtils.isEmpty(paymentData.getIban())) {
            throw new InvalidDataException("Iban is required");
        }

        String countryCode = "";
        // get the countryCode from the BIC or ASPSP id
        if (paymentData.getBic() != null) {
            countryCode = PluginUtils.getCountryCodeFromBIC(
                    jsonService.fromJson(paymentRequest.getPluginConfiguration(), GetAspspsResponse.class).getAspsps()
                    , paymentData.getBic());
        }
        else {
            countryCode = aspsp.getCountryCode();
        }

        if (PluginUtils.isEmpty(countryCode)) {
            throw new InvalidDataException("Unable to determine country code for this transaction");
        }

        // if the buyer want to use his IBAN, it should be an IBAN from a country available by the merchant
        if (!PluginUtils.isEmpty(paymentData.getIban()) && !PluginUtils.correctIban(listCountryCode, paymentData.getIban())) {
            throw new InvalidDataException("IBAN should be from a country available by the merchant " + listCountryCode.toString());
        }
    }

    // Build PaymentInitiationRequest (Equens) from PaymentRequest (Payline)
    protected PaymentInitiationRequest buildPaymentInitiationRequest(GenericPaymentRequest paymentRequest, PaymentData paymentData) {

        // Control on the input data (to avoid NullPointerExceptions)
        validateRequest(paymentRequest);
        validPaymentData(paymentData, paymentRequest.getPluginConfiguration(), paymentRequest);

        // Extract delivery address
        Address deliveryAddress = null;
        if (paymentRequest.getBuyer().getAddresses() != null) {
            deliveryAddress = buildAddress(paymentRequest.getBuyer().getAddressForType(Buyer.AddressType.DELIVERY));
        }

        final String iban = paymentData.getIban();
        final String aspspId = paymentData.getAspspId();
        final String merchantName = paymentRequest.getContractConfiguration().getProperty(Constants.ContractConfigurationKeys.MERCHANT_NAME).getValue();
        final String creditorName =  PluginUtils.isEmpty(merchantName) ? null : merchantName;
        final String creditorAccountIdentification = paymentRequest.getContractConfiguration().getProperty(Constants.ContractConfigurationKeys.MERCHANT_IBAN).getValue();
        final Account creditorAccount =  PluginUtils.isEmpty(creditorAccountIdentification) ?  null : new Account.AccountBuilder().withIdentification(creditorAccountIdentification).build();

        final String softDescriptor = paymentRequest.getSoftDescriptor();
        final String pispContract = paymentRequest.getContractConfiguration().getProperty(Constants.ContractConfigurationKeys.PISP_CONTRACT).getValue();

        final ContractProperty subIdContractProperty =  paymentRequest.getContractConfiguration().getProperty(Constants.ContractConfigurationKeys.INITIATING_PARTY_SUBID);
        final String initiatingPartySubId = subIdContractProperty != null ? subIdContractProperty.getValue() : null;

        final PaymentInitiationRequest.PaymentInitiationRequestBuilder paymentInitiationRequestBuilder = new PaymentInitiationRequest.PaymentInitiationRequestBuilder()
                .withAspspId(aspspId)
                .withEndToEndId(paymentRequest.getOrder().getReference())
                .withInitiatingPartyReferenceId(paymentRequest.getTransactionId())
                .withRemittanceInformation(softDescriptor + pispContract)
                .withRemittanceInformationStructured(
                        new RemittanceInformationStructured.RemittanceInformationStructuredBuilder()
                                .withReference(paymentRequest.getOrder().getReference())
                                .build()
                )
                .withCreditorAccount(creditorAccount)
                .withCreditorName(creditorName)
                .withPaymentAmount(convertAmount(paymentRequest.getAmount()))
                .withPaymentCurrency(paymentRequest.getAmount().getCurrency().getCurrencyCode())
                .withPurposeCode(
                        paymentRequest.getContractConfiguration().getProperty(Constants.ContractConfigurationKeys.PURPOSE_CODE).getValue()
                )

                .withPaymentContext(
                        new PaymentContext.PaymentContextBuilder()
                                .withMerchantCategoryCode(paymentRequest.getSubMerchant() != null ? paymentRequest.getSubMerchant().getSubMerchantMCC() : null)
                                .withMerchantCustomerId(paymentRequest.getBuyer().getCustomerIdentifier())
                                .withDeliveryAddress(deliveryAddress)
                                .withChannelType(paymentRequest.getContractConfiguration().getProperty(Constants.ContractConfigurationKeys.CHANNEL_TYPE).getValue())
                                .build()
                )
                .addPreferredScaMethod(
                        paymentRequest.getContractConfiguration().getProperty(Constants.ContractConfigurationKeys.SCA_METHOD).getValue()
                )
                .withChargeBearer(
                        paymentRequest.getContractConfiguration().getProperty(Constants.ContractConfigurationKeys.CHARGE_BEARER).getValue()
                )
                .withPaymentProduct(
                        paymentRequest.getContractConfiguration()
                                .getProperty(Constants.ContractConfigurationKeys.PAYMENT_PRODUCT).getValue()
                ).withDebtorName(paymentRequest.getBuyer().getFullName().getLastName())
                .withDebtorPostalAddress(null);
        // add the debtor account only if he gives his IBAN, to avoid an empty object
        if (!PluginUtils.isEmpty(iban)) {
            paymentInitiationRequestBuilder.withDebtorAccount(
                    new Account.AccountBuilder()
                            .withIdentification(iban)
                            .build()
            );
        }
        if (!PluginUtils.isEmpty(initiatingPartySubId)) {
            paymentInitiationRequestBuilder.withInitiatingPartySubId(initiatingPartySubId);
        }
        return paymentInitiationRequestBuilder.build();
    }

    /**
     * Convert a Payline <code>Amount</code> into a string with a dot as decimals separator.
     *
     * @param amount the amount to convert
     * @return The amount formatted as a string
     */
    String convertAmount(Amount amount) {
        if (amount == null || amount.getAmountInSmallestUnit() == null || amount.getCurrency() == null) {
            return null;
        }
        Currency currency = amount.getCurrency();
        BigInteger amountInSmallestUnit = amount.getAmountInSmallestUnit();

        // number of digits of the currency
        int nbDigits = currency.getDefaultFractionDigits();

        // init string builder
        StringBuilder sb = new StringBuilder();
        sb.append(amountInSmallestUnit);

        // if necessary, add digits at the beginning (ex: convert "1" to "001" for EUR, for example)
        for (int i = sb.length(); i < nbDigits + 1; i++) {
            sb.insert(0, "0");
        }

        // add the dot separator (ex: convert "001" to "0.01" for EUR)
        if (nbDigits > 0) {
            sb.insert(sb.length() - nbDigits, ".");
        }

        return sb.toString();
    }
}
