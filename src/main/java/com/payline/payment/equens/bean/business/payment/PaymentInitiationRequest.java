package com.payline.payment.equens.bean.business.payment;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/**
 * Payment initiation request.
 */
public class PaymentInitiationRequest {

    /**
     * Identifies the debtor bank. This ID is taken from the reach directory.
     */
    @SerializedName("AspspId")
    private String aspspId;
    /**
     * Unique identification assigned by the Initiating Party to unumbiguously identify the transaction.
     * This identification is passed on, unchanged, throughout the entire end-to-end chain.
     * Can be used for reconciliation by the Initiating Party.
     */
    @SerializedName("EndToEndId")
    private String endToEndId;
    /**
     * Reference to the payment created by the Initiating Party.
     * This Id will not be visible to the Payment Service User.
     */
    @SerializedName("InitiatingPartyReferenceId")
    private String initiatingPartyReferenceId;

    /**
     * Information supplied to enable the matching of an entry with the items that the transfer is intended to settle.
     * This information will be visible to the Payment Service User.
     */
    @SerializedName("RemittanceInformation")
    private String remittanceInformation;
    /**
     * ?
     */
    @SerializedName("RemittanceInformationStructured")
    private RemittanceInformationStructured remittanceInformationStructured;

    /**
     * name of the debtor
     */
    @SerializedName("DebtorName")
    private String debtorName;

    /**
     * Identification of the debtor account.
     */
    @SerializedName("DebtorAccount")
    private Account debtorAccount;
    /**
     * Identification of the debtor postal address.
     */
    @SerializedName("DebtorPostalAddress")
    private Address debtorPostalAddress;
    /**
     * The name of the creditor.
     */
    @SerializedName("CreditorName")
    private String creditorName;
    /**
     * Identification of the creditor account.
     */
    @SerializedName("CreditorAccount")
    private Account creditorAccount;
    /**
     * Amount of the payment. The decimal separator is a dot.
     */
    @SerializedName("PaymentAmount")
    private String paymentAmount;
    /**
     * Currency of the payment. ISO 4217 currency codes should be used.
     */
    @SerializedName("PaymentCurrency")
    private String paymentCurrency;
    /**
     * Specifies the purpose code that resulted in a payment initiation.
     */
    @SerializedName("PurposeCode")
    private String purposeCode;
    /**
     * Information used for risk scoring by the ASPSP.
     */
    @SerializedName("PaymentContext")
    private PaymentContext paymentContext;
    /**
     * Payment preferred SCA.
     */
    @SerializedName("PreferredScaMethod")
    private List<String> preferredScaMethod;
    /**
     * Charge bearer
     */
    @SerializedName("ChargeBearer")
    private String chargeBearer;

    /**
     * Indicates the requested payment method.
     */
    @SerializedName("PaymentProduct")
    private String paymentProduct;

    @SerializedName("InitiatingPartySubId")
    private String initiatingPartySubId;


    PaymentInitiationRequest(PaymentInitiationRequestBuilder builder) {
        this.aspspId = builder.aspspId;
        this.endToEndId = builder.endToEndId;
        this.initiatingPartyReferenceId = builder.initiatingPartyReferenceId;
        this.remittanceInformation = builder.remittanceInformation;
        this.remittanceInformationStructured = builder.remittanceInformationStructured;
        this.debtorName = builder.debtorName;
        this.debtorAccount = builder.debtorAccount;
        this.debtorPostalAddress=builder.debtorPostalAddress;
        this.creditorName = builder.creditorName;
        this.creditorAccount = builder.creditorAccount;
        this.paymentAmount = builder.paymentAmount;
        this.paymentCurrency = builder.paymentCurrency;
        this.purposeCode = builder.purposeCode;
        this.paymentContext = builder.paymentContext;
        this.preferredScaMethod = builder.preferredScaMethod;
        this.chargeBearer = builder.chargeBearer;
        this.paymentProduct = builder.paymentProduct;
        this.initiatingPartySubId = builder.initiatingPartySubId;
    }

    public static class PaymentInitiationRequestBuilder  {

        private String aspspId;
        private String endToEndId;
        private String initiatingPartyReferenceId;
        private String remittanceInformation;
        private RemittanceInformationStructured remittanceInformationStructured;
        private String debtorName;
        private Account debtorAccount;
        private Address debtorPostalAddress;
        private String creditorName;
        private Account creditorAccount;
        private String paymentAmount;
        private String paymentCurrency;
        private String purposeCode;
        private PaymentContext paymentContext;
        private List<String> preferredScaMethod;
        private String chargeBearer;
        private String paymentProduct;
        private String initiatingPartySubId;

        public PaymentInitiationRequestBuilder withAspspId(String aspspId) {
            this.aspspId = aspspId;
            return this;
        }

        public PaymentInitiationRequestBuilder withEndToEndId(String endToEndId) {
            this.endToEndId = endToEndId;
            return this;
        }

        public PaymentInitiationRequestBuilder withInitiatingPartyReferenceId(String initiatingPartyReferenceId) {
            this.initiatingPartyReferenceId = initiatingPartyReferenceId;
            return this;
        }


        public PaymentInitiationRequestBuilder withRemittanceInformation(String remittanceInformation) {
            this.remittanceInformation = remittanceInformation;
            return this;
        }

        public PaymentInitiationRequestBuilder withRemittanceInformationStructured(RemittanceInformationStructured remittanceInformationStructured) {
            this.remittanceInformationStructured = remittanceInformationStructured;
            return this;
        }

        public PaymentInitiationRequestBuilder withDebtorName(String debtorName){
            this.debtorName = debtorName;
            return this;
        }

        public PaymentInitiationRequestBuilder withDebtorAccount(Account debtorAccount) {
            this.debtorAccount = debtorAccount;
            return this;
        }

        public PaymentInitiationRequestBuilder withDebtorPostalAddress(Address debtorPostalAddress) {
            this.debtorPostalAddress = debtorPostalAddress;
            return this;
        }

        public PaymentInitiationRequestBuilder withCreditorName(String creditorName) {
            this.creditorName = creditorName;
            return this;
        }

        public PaymentInitiationRequestBuilder withCreditorAccount(Account creditorAccount) {
            this.creditorAccount = creditorAccount;
            return this;
        }

        public PaymentInitiationRequestBuilder withPaymentAmount(String paymentAmount) {
            this.paymentAmount = paymentAmount;
            return this;
        }

        public PaymentInitiationRequestBuilder withPaymentCurrency(String paymentCurrency) {
            this.paymentCurrency = paymentCurrency;
            return this;
        }

        public PaymentInitiationRequestBuilder withPurposeCode(String purposeCode) {
            this.purposeCode = purposeCode;
            return this;
        }

        public PaymentInitiationRequestBuilder withPaymentContext(PaymentContext paymentContext) {
            this.paymentContext = paymentContext;
            return this;
        }

        public PaymentInitiationRequestBuilder addPreferredScaMethod(String preferredScaMethod) {
            if (this.preferredScaMethod == null) {
                this.preferredScaMethod = new ArrayList<>();
            }
            this.preferredScaMethod.add(preferredScaMethod);
            return this;
        }

        public PaymentInitiationRequestBuilder withChargeBearer(String chargeBearer) {
            this.chargeBearer = chargeBearer;
            return this;
        }

        public PaymentInitiationRequestBuilder withPaymentProduct(String paymentProduct) {
            this.paymentProduct = paymentProduct;
            return this;
        }

        public PaymentInitiationRequestBuilder withInitiatingPartySubId(String initiatingPartySubId) {
            this.initiatingPartySubId = initiatingPartySubId;
            return this;
        }

        public PaymentInitiationRequest build() {
            return new PaymentInitiationRequest(this);
        }

    }

    public String getAspspId() {
        return aspspId;
    }

    public String getEndToEndId() {
        return endToEndId;
    }

    public String getInitiatingPartyReferenceId() {
        return initiatingPartyReferenceId;
    }


    public String getRemittanceInformation() {
        return remittanceInformation;
    }

    public RemittanceInformationStructured getRemittanceInformationStructured() {
        return remittanceInformationStructured;
    }

    public String getDebtorName() {
        return this.debtorName;
    }

    public Account getDebtorAccount() {
        return debtorAccount;
    }

    public Address getDebtorPostalAddress() {
        return debtorPostalAddress;
    }

    public String getCreditorName() {
        return creditorName;
    }

    public Account getCreditorAccount() {
        return creditorAccount;
    }

    public String getPaymentAmount() {
        return paymentAmount;
    }

    public String getPaymentCurrency() {
        return paymentCurrency;
    }

    public String getPurposeCode() {
        return purposeCode;
    }

    public PaymentContext getPaymentContext() {
        return paymentContext;
    }

    public List<String> getPreferredScaMethod() {
        return preferredScaMethod;
    }

    public String getChargeBearer() {
        return chargeBearer;
    }


    public String getPaymentProduct() {
        return paymentProduct;
    }

    public String getInitiatingPartySubId() {
        return initiatingPartySubId;
    }

}
