package com.payline.payment.equens.business;

import com.payline.payment.equens.bean.business.payment.PaymentStatusResponse;
import com.payline.pmapi.bean.payment.response.PaymentResponse;

public interface PaymentBusiness {

    /**
     * Methode permettant de récupérer une réponse de paiement en prenant en compte le fait
     * que la transaction puisse être en attente côté partenaire(OnHold).
     * @param paymentStatusResponse
     *          Reponse du service equens à analyser.
     * @param paymentId
     *          Numéro du paiement concerné.
     * @param transactionAdditionalData
     *          Données additionnelles liés à la transaction.
     * @param merchantIban
     *          Iban du marchand.
     * @return
     *          Réponse final de la transaction.
     */
    PaymentResponse fetchPaymentResponse(final PaymentStatusResponse paymentStatusResponse, final String paymentId,
                                                final String transactionAdditionalData, final String merchantIban);
    /**
     * Methode permettant de récupérer une réponse de paiement sans les statuts en attente (OnHold).
     * @param paymentStatusResponse
     *          Reponse du service equens à analyser.
     * @param paymentId
     *          Numéro du paiement concerné.
     * @param transactionAdditionalData
     *          Données additionnelles liés à la transaction.
     * @param merchantIban
     *          Iban du marchand.
     * @return
     *          Réponse final de la transaction.
     */
    PaymentResponse fetchLastStatusPaymentResponse(final PaymentStatusResponse paymentStatusResponse, final String paymentId,
                                                   final String transactionAdditionalData, final String merchantIban);

    }
