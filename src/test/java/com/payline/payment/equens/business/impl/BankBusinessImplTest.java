package com.payline.payment.equens.business.impl;

import com.google.gson.Gson;
import com.payline.payment.equens.bean.business.reachdirectory.Detail;
import com.payline.payment.equens.business.BankBusiness;
import com.payline.payment.equens.service.impl.ConfigurationServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BankBusinessImplTest {

    private BankBusiness underTest = new BankBusinessImpl();

    @Test
    void isCompatibleNormalWithNullDetail() {
        Assertions.assertTrue(underTest.isCompatibleBank(null,
                ConfigurationServiceImpl.PaymentProduct.NORMAL.getPaymentProductCode()));
    }

    @Test
    void shouldBeCompatibleNormalWithOneDetailsWithValueNormal() {
        final List<Detail> details = new ArrayList<>();
        details.add(detailWithPostPaymentNormal());

        Assertions.assertTrue(underTest.isCompatibleBank(details,
                ConfigurationServiceImpl.PaymentProduct.NORMAL.getPaymentProductCode()));
    }

    @Test
    void shouldBeCompatibleNormalWithOneDetailsWithValueNormalAndInstant() {
        final List<Detail> details = new ArrayList<>();
        details.add(detailWithPostPaymentInstantAndNormal());

        Assertions.assertTrue(underTest.isCompatibleBank(details,
                ConfigurationServiceImpl.PaymentProduct.NORMAL.getPaymentProductCode()));
    }

    @Test
    void shouldNotBeCompatibleNormalWithOneDetailsWithValueInstant() {
        final List<Detail> details = new ArrayList<>();
        details.add(detailWithPostPaymentInstant());

        Assertions.assertFalse(underTest.isCompatibleBank(details,
                ConfigurationServiceImpl.PaymentProduct.NORMAL.getPaymentProductCode()));
    }

    @Test
    void shouldNotBeCompatibleInstantWithNullDetail() {
        Assertions.assertFalse(underTest.isCompatibleBank(null,
                ConfigurationServiceImpl.PaymentProduct.INSTANT.getPaymentProductCode()));
    }

    @Test
    void shouldBeCompatibleInstantWithOneDetailsWithValueNormalAndInstant() {
        final List<Detail> details = new ArrayList<>();
        details.add(detailWithPostPaymentInstantAndNormal());

        Assertions.assertTrue(underTest.isCompatibleBank(details,
                ConfigurationServiceImpl.PaymentProduct.INSTANT.getPaymentProductCode()));
    }

    @Test
    void shouldBeCompatibleInstantWithOneDetailsWithValueInstant() {
        final List<Detail> details = new ArrayList<>();
        details.add(detailWithPostPaymentInstant());

        Assertions.assertTrue(underTest.isCompatibleBank(details,
                ConfigurationServiceImpl.PaymentProduct.INSTANT.getPaymentProductCode()));
    }

    @Test
    void shouldNotBeCompatibleWithTwoDetailsWithValuesButNoInstant() {
        final List<Detail> details = new ArrayList<>();
        details.add(detailWithPostPaymentRandomValue());
        details.add(detailWithPostPaymentRandomValue());

        Assertions.assertFalse(underTest.isCompatibleBank(details,
                ConfigurationServiceImpl.PaymentProduct.INSTANT.getPaymentProductCode()));
    }

    private Detail detailWithNoPostPayment() {
        return detailFromJSON("{\n" +
                "          \"Api\": \"POST autreapi\",\n" +
                "          \"Fieldname\" : \"PaymentProduct\",\n" +
                "          \"Type\": \"SUPPORTED\",\n" +
                "          \"Value\": \"Normal|Instant\",\n" +
                "          \"ProtocolVersion\": \"STET_V_1_4_0_47\"\n" +
                "        }");
    }

    private Detail detailWithPostPaymentInstantAndNormal() {
        return detailFromJSON("{\n" +
                "          \"Api\": \"POST /payments\",\n" +
                "          \"Fieldname\" : \"PaymentProduct\",\n" +
                "          \"Type\": \"SUPPORTED\",\n" +
                "          \"Value\": \"Normal|Instant\",\n" +
                "          \"ProtocolVersion\": \"STET_V_1_4_0_47\"\n" +
                "        }");
    }

    private Detail detailWithPostPaymentInstant() {
        return detailFromJSON("{\n" +
                "          \"Api\": \"POST /payments\",\n" +
                "          \"Fieldname\" : \"PaymentProduct\",\n" +
                "          \"Type\": \"SUPPORTED\",\n" +
                "          \"Value\": \"Instant\",\n" +
                "          \"ProtocolVersion\": \"STET_V_1_4_0_47\"\n" +
                "        }");
    }

    private Detail detailWithPostPaymentNormal() {
        return detailFromJSON("{\n" +
                "          \"Api\": \"POST /payments\",\n" +
                "          \"Fieldname\" : \"PaymentProduct\",\n" +
                "          \"Type\": \"SUPPORTED\",\n" +
                "          \"Value\": \"Normal\",\n" +
                "          \"ProtocolVersion\": \"STET_V_1_4_0_47\"\n" +
                "        }");
    }

    private Detail detailWithPostPaymentRandomValue() {
        return detailFromJSON("{\n" +
                "          \"Api\": \"POST /payments\",\n" +
                "          \"Fieldname\" : \"PaymentProduct\",\n" +
                "          \"Type\": \"SUPPORTED\",\n" +
                "          \"Value\": \"Random\",\n" +
                "          \"ProtocolVersion\": \"STET_V_1_4_0_47\"\n" +
                "        }");
    }

    private Detail detailFromJSON(final String json) {
        final Gson gson = new Gson();
        return gson.fromJson(json, Detail.class);
    }
}