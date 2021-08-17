package com.payline.payment.equens.bean.business.payment;

import com.google.gson.annotations.SerializedName;

/** Information used for risk scoring by the ASPSP. */
public class PaymentContext {

    /**
     * Category code conform to ISO 18245, related to the type of services or goods the merchant provides for the transaction.
     */
    @SerializedName("MerchantCategoryCode")
    private String merchantCategoryCode;
    /**
     * The unique customer identifier of the PSU with the merchant.
     */
    @SerializedName("MerchantCustomerId")
    private String merchantCustomerId;
    /** ? */
    @SerializedName("DeliveryAddress")
    private Address deliveryAddress;
    /** Payment channel type */
    @SerializedName("ChannelType")
    private String channelType;

    public PaymentContext(PaymentContextBuilder builder ){
        this.merchantCategoryCode = builder.merchantCategoryCode;
        this.merchantCustomerId = builder.merchantCustomerId;
        this.deliveryAddress = builder.deliveryAddress;
        this.channelType = builder.channelType;
    }

    public static class PaymentContextBuilder {

        private String merchantCategoryCode;
        private String merchantCustomerId;
        private Address deliveryAddress;
        private String channelType;

        public PaymentContextBuilder withMerchantCategoryCode(String merchantCategoryCode) {
            this.merchantCategoryCode = merchantCategoryCode;
            return this;
        }

        public PaymentContextBuilder withMerchantCustomerId(String merchantCustomerId) {
            this.merchantCustomerId = merchantCustomerId;
            return this;
        }

        public PaymentContextBuilder withDeliveryAddress(Address deliveryAddress) {
            this.deliveryAddress = deliveryAddress;
            return this;
        }

        public PaymentContextBuilder withChannelType(String channelType) {
            this.channelType = channelType;
            return this;
        }

        public PaymentContext build(){
            return new PaymentContext( this );
        }
    }

    public String getMerchantCategoryCode() {
        return merchantCategoryCode;
    }

    public String getMerchantCustomerId() {
        return merchantCustomerId;
    }

    public Address getDeliveryAddress() {
        return deliveryAddress;
    }

    public String getChannelType() {
        return channelType;
    }
}
