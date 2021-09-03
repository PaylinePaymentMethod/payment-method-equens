package com.payline.payment.equens.business;

import com.payline.payment.equens.bean.business.payment.PaymentStatusResponse;
import com.payline.pmapi.bean.payment.response.PaymentResponse;

public interface PaymentBusiness {

    /**
     * Methode permettant de r�cup�rer une r�ponse de paiement en prenant en compte le fait
     * que la transaction puisse �tre en attente c�t� partenaire(OnHold).
     * @param paymentStatusResponse
     *          Reponse du service equens � analyser.
     * @param paymentId
     *          Num�ro du paiement concern�.
     * @param transactionAdditionalData
     *          Donn�es additionnelles li�s � la transaction.
     * @param merchantIban
     *          Iban du marchand.
     * @return
     *          R�ponse final de la transaction.
     */
    PaymentResponse fetchPaymentResponse(final PaymentStatusResponse paymentStatusResponse, final String paymentId,
                                                final String transactionAdditionalData, final String merchantIban);
    /**
     * Methode permettant de r�cup�rer une r�ponse de paiement sans les statuts en attente (OnHold).
     * @param paymentStatusResponse
     *          Reponse du service equens � analyser.
     * @param paymentId
     *          Num�ro du paiement concern�.
     * @param transactionAdditionalData
     *          Donn�es additionnelles li�s � la transaction.
     * @param merchantIban
     *          Iban du marchand.
     * @return
     *          R�ponse final de la transaction.
     */
    PaymentResponse fetchLastStatusPaymentResponse(final PaymentStatusResponse paymentStatusResponse, final String paymentId,
                                                   final String transactionAdditionalData, final String merchantIban);

    }
