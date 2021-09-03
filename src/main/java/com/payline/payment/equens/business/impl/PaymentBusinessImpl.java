package com.payline.payment.equens.business.impl;

import com.payline.payment.equens.bean.business.payment.PaymentStatus;
import com.payline.payment.equens.bean.business.payment.PaymentStatusResponse;
import com.payline.payment.equens.business.PaymentBusiness;
import com.payline.pmapi.bean.common.FailureCause;
import com.payline.pmapi.bean.common.OnHoldCause;
import com.payline.pmapi.bean.payment.response.PaymentResponse;
import com.payline.pmapi.bean.payment.response.buyerpaymentidentifier.BankAccount;
import com.payline.pmapi.bean.payment.response.buyerpaymentidentifier.impl.BankTransfer;
import com.payline.pmapi.bean.payment.response.impl.PaymentResponseFailure;
import com.payline.pmapi.bean.payment.response.impl.PaymentResponseOnHold;
import com.payline.pmapi.bean.payment.response.impl.PaymentResponseSuccess;

public class PaymentBusinessImpl implements PaymentBusiness {

    @Override
    public PaymentResponse fetchPaymentResponse(final PaymentStatusResponse paymentStatusResponse, final String paymentId,
                                                final String transactionAdditionalData, final String merchantIban) {
        final PaymentResponse paymentResponse;
        final PaymentStatus status = paymentStatusResponse.getPaymentStatus();
        // Build the appropriate response
        switch(status) {
            case OPEN:
            case AUTHORISED:
            case PENDING:
                paymentResponse = PaymentResponseOnHold.PaymentResponseOnHoldBuilder.aPaymentResponseOnHold()
                        .withPartnerTransactionId(paymentId)
                        .withOnHoldCause(OnHoldCause.INPROGRESS_PARTNER)
                        .withStatusCode(status.name())
                        .build();
                break;
            case SETTLEMENT_IN_PROCESS:
            case SETTLEMENT_COMPLETED:
                paymentResponse = PaymentResponseSuccess.PaymentResponseSuccessBuilder.aPaymentResponseSuccess()
                        .withPartnerTransactionId(paymentId)
                        .withStatusCode(status.name())
                        .withTransactionDetails(new BankTransfer(
                                this.getOwnerBankAccount(paymentStatusResponse),
                                this.getReceiverBankAccount(merchantIban)))
                        .withTransactionAdditionalData(transactionAdditionalData).build();
                break;

            case CANCELLED:
                paymentResponse = buildPaymentFailure(paymentId, transactionAdditionalData, "Payment not approved by PSU or insufficient funds", FailureCause.CANCEL);
                break;

            case EXPIRED:
                paymentResponse = buildPaymentFailure(paymentId, transactionAdditionalData, "Consent approval has expired", FailureCause.SESSION_EXPIRED);
                break;

            case ERROR:
            default:
                paymentResponse = buildPaymentFailure(paymentId, transactionAdditionalData, "Payment was rejected due to an error", FailureCause.REFUSED);
                break;
        }
        return paymentResponse;
    }

    @Override
    public PaymentResponse fetchLastStatusPaymentResponse(final PaymentStatusResponse paymentStatusResponse, final String paymentId,
                                                          final String transactionAdditionalData, final String merchantIban) {
        final PaymentResponse paymentResponse;
        final PaymentStatus status = paymentStatusResponse.getPaymentStatus();
        // Build the appropriate response
        switch(status) {
            case OPEN:
            case SETTLEMENT_IN_PROCESS:
            case SETTLEMENT_COMPLETED:
            case PENDING:
            case AUTHORISED:
                paymentResponse = PaymentResponseSuccess.PaymentResponseSuccessBuilder.aPaymentResponseSuccess()
                        .withPartnerTransactionId(paymentId)
                        .withStatusCode(status.name())
                        .withTransactionDetails(new BankTransfer(
                                this.getOwnerBankAccount(paymentStatusResponse),
                                this.getReceiverBankAccount(merchantIban)))
                        .withTransactionAdditionalData(transactionAdditionalData).build();
                break;

            case CANCELLED:
                paymentResponse = buildPaymentFailure(paymentId, transactionAdditionalData, "Payment not approved by PSU or insufficient funds", FailureCause.CANCEL);
                break;

            case EXPIRED:
                paymentResponse = buildPaymentFailure(paymentId, transactionAdditionalData, "Consent approval has expired", FailureCause.SESSION_EXPIRED);
                break;
            case ERROR:
            default:
                paymentResponse = buildPaymentFailure(paymentId, transactionAdditionalData, "Payment was rejected due to an error", FailureCause.REFUSED);
                break;
        }
        return paymentResponse;
    }

    /**
     * Extract the owner bank account data from the given PaymentStatusResponse.
     *
     * @param paymentStatusResponse the payment status response
     * @return the owner bank account
     */
    private BankAccount getOwnerBankAccount(final PaymentStatusResponse paymentStatusResponse) {
        // pre-fill a builder with empty strings (null values not authorized)
        final BankAccount.BankAccountBuilder ownerBuilder = BankAccount.BankAccountBuilder.aBankAccount()
                .withHolder("")
                .withAccountNumber("")
                .withIban("")
                .withBic("")
                .withCountryCode("")
                .withBankName("")
                .withBankCode("");

        // Fill available data
        if (paymentStatusResponse.getDebtorName() != null) {
            ownerBuilder.withHolder(paymentStatusResponse.getDebtorName());
        }
        if (paymentStatusResponse.getDebtorAccount() != null) {
            ownerBuilder.withIban(paymentStatusResponse.getDebtorAccount());
        }
        if (paymentStatusResponse.getDebtorAgent() != null) {
            ownerBuilder.withBic(paymentStatusResponse.getDebtorAgent());
        }

        return ownerBuilder.build();
    }

    /**
     * Build the receiver bank account with the given merchant IBAN.
     * Every other field is filled with an empty string.
     *
     * @param merchantIban the merchant IBAN
     * @return the receiver bank account
     */
    private BankAccount getReceiverBankAccount(final String merchantIban) {
        // pre-fill a builder fwith empty strings (null values not authorized)
        final BankAccount.BankAccountBuilder receiverBuilder = BankAccount.BankAccountBuilder.aBankAccount()
                .withHolder("")
                .withAccountNumber("")
                .withIban(merchantIban)
                .withBic("")
                .withCountryCode("")
                .withBankName("")
                .withBankCode("");
        return receiverBuilder.build();
    }

    /**
     * Méthode permettant de construire des PaymentResponseFailed.
     * @param paymentId
     *          Identifiant du paiement.
     * @param transactionAdditionalData
     *          Données additionnelles de la transaction.
     * @param message
     *          Message explicatif du refus de paiement.
     * @param failureCause
     *          Raison du refus de paiement.
     * @return
     *          La reponse du paiement correctement formattée
     */
    private PaymentResponseFailure buildPaymentFailure(final String paymentId, final String transactionAdditionalData, final String message, final FailureCause failureCause) {
        return PaymentResponseFailure.PaymentResponseFailureBuilder.aPaymentResponseFailure()
                .withErrorCode(message)
                .withFailureCause(failureCause)
                .withPartnerTransactionId(paymentId)
                .withTransactionAdditionalData(transactionAdditionalData).build();
    }
}
