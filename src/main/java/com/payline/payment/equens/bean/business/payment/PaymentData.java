package com.payline.payment.equens.bean.business.payment;

public class PaymentData {

    /** Identifiant de l'Aspsp **/
    private String aspspId;
    /** the BIC for the creation of the wallet */
    private String bic;
    /** the IBAN for the creation of the wallet */
    private String iban;

    private PaymentData(PaymentDataBuilder builder) {
        bic = builder.bic;
        iban = builder.iban;
        aspspId = builder.aspspId;
    }

    public static class PaymentDataBuilder {
        private String bic;
        private String iban;
        private String aspspId;

        public PaymentDataBuilder withBic(String bic) {
            this.bic = bic;
            return this;
        }

        public PaymentDataBuilder withIban(String iban) {
            this.iban = iban;
            return this;
        }

        public PaymentDataBuilder withAspspId(String aspspId) {
            this.aspspId = aspspId;
            return this;
        }

        public PaymentData build() {
            return new PaymentData(this);
        }
    }

    public String getBic() {
        return bic;
    }

    public String getIban() {
        return iban;
    }

    public String getAspspId() {
        return aspspId;
    }
}
