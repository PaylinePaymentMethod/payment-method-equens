package com.payline.payment.equens.service.impl;

import com.payline.pmapi.bean.refund.request.RefundRequest;
import com.payline.pmapi.bean.refund.response.RefundResponse;
import com.payline.pmapi.bean.refund.response.impl.RefundResponseSuccess;
import com.payline.pmapi.service.RefundService;

import java.util.HashMap;

public class RefundServiceImpl implements RefundService {

    public static final String OK_STATUS_CODE = "OK";

    @Override
    public RefundResponse refundRequest(final RefundRequest refundRequest) {
        //Pour le remboursement EQUENS celui-ci se fait directement par MarketPay.
        //On note juste que le remboursement a été fait côté Payline mais aucune opération
        //de caisse n'est faite.
        return RefundResponseSuccess.RefundResponseSuccessBuilder.aRefundResponseSuccess()
                .withPartnerTransactionId(refundRequest.getPartnerTransactionId())
                .withStatusCode(OK_STATUS_CODE)
                .withMiscellaneous(new HashMap<>())
                .build();
    }

    @Override
    public boolean canMultiple() {
        return false;
    }

    @Override
    public boolean canPartial() {
        return false;
    }


}
