package com.payline.payment.equens.service;

import com.payline.payment.equens.MockUtils;
import com.payline.payment.equens.bean.GenericPaymentRequest;
import com.payline.payment.equens.bean.business.payment.Address;
import com.payline.payment.equens.bean.business.payment.PaymentData;
import com.payline.payment.equens.bean.business.payment.PaymentInitiationRequest;
import com.payline.payment.equens.bean.business.psu.Psu;
import com.payline.payment.equens.bean.business.psu.PsuCreateRequest;
import com.payline.payment.equens.bean.configuration.RequestConfiguration;
import com.payline.payment.equens.exception.InvalidDataException;
import com.payline.payment.equens.exception.PluginException;
import com.payline.payment.equens.utils.TestUtils;
import com.payline.payment.equens.utils.constant.ContractConfigurationKeys;
import com.payline.payment.equens.utils.http.PisHttpClient;
import com.payline.payment.equens.utils.http.PsuHttpClient;
import com.payline.pmapi.bean.common.Amount;
import com.payline.pmapi.bean.common.Buyer;
import com.payline.pmapi.bean.common.FailureCause;
import com.payline.pmapi.bean.configuration.PartnerConfiguration;
import com.payline.pmapi.bean.payment.ContractConfiguration;
import com.payline.pmapi.bean.payment.ContractProperty;
import com.payline.pmapi.bean.payment.request.PaymentRequest;
import com.payline.pmapi.bean.payment.response.PaymentResponse;
import com.payline.pmapi.bean.payment.response.impl.PaymentResponseFailure;
import com.payline.pmapi.bean.payment.response.impl.PaymentResponseRedirect;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigInteger;
import java.util.Currency;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

class GenericPaymentServiceTest {

    @InjectMocks
    private GenericPaymentService service;

    @Mock
    private PisHttpClient pisHttpClient;
    @Mock
    private PsuHttpClient psuHttpclient;

    @BeforeEach
    void setUp() {
        service = GenericPaymentService.getInstance();
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void paymentRequest() {
        doReturn(MockUtils.aPsu()).when(psuHttpclient).createPsu(any(), any());
        doReturn(MockUtils.aPaymentInitiationResponse()).when(pisHttpClient).initPayment(any(), any());

        PaymentData paymentData = MockUtils.aPaymentdata();
        // when: calling paymentRequest() method
        PaymentRequest paymentRequest = MockUtils.aPaylinePaymentRequest();
        GenericPaymentRequest genericPaymentRequest = new GenericPaymentRequest(paymentRequest);
        PaymentResponse paymentResponse = service.paymentRequest(genericPaymentRequest, paymentData);

        // then: the payment response is a success
        assertEquals(PaymentResponseRedirect.class, paymentResponse.getClass());
        TestUtils.checkPaymentResponse((PaymentResponseRedirect) paymentResponse);
    }

    @Test
    void paymentRequest_EmptyMerchantName() {
        doReturn(MockUtils.aPsu()).when(psuHttpclient).createPsu(any(), any());
        JsonService jsonService = JsonService.getInstance();

        PaymentData paymentData = MockUtils.aPaymentdata();
        // when: calling paymentRequest() method
        PaymentRequest paymentRequest = MockUtils.aPaylinePaymentRequest();

        paymentRequest.getContractConfiguration().getContractProperties().remove(ContractConfigurationKeys.MERCHANT_NAME);
        paymentRequest.getContractConfiguration().getContractProperties().put(ContractConfigurationKeys.MERCHANT_NAME, new ContractProperty(""));

        GenericPaymentRequest genericPaymentRequest = new GenericPaymentRequest(paymentRequest);

        // Build request configuration
        RequestConfiguration requestConfiguration = new RequestConfiguration(
                paymentRequest.getContractConfiguration()
                , paymentRequest.getEnvironment()
                , paymentRequest.getPartnerConfiguration()
        );

        // Create a new PSU
        Psu newPsu = psuHttpclient.createPsu(new PsuCreateRequest.PsuCreateRequestBuilder().build(), requestConfiguration);

        // Build PaymentInitiationRequest (Equens) from PaymentRequest (Payline)
        PaymentInitiationRequest request = service.buildPaymentInitiationRequest(genericPaymentRequest, newPsu, paymentData);

        String chaine = jsonService.toJson(request);

        assertFalse(chaine.contains("CreditorName"));

    }

    @Test
    void paymentRequest_EmptyMerchantIban() {
        doReturn(MockUtils.aPsu()).when(psuHttpclient).createPsu(any(), any());
        JsonService jsonService = JsonService.getInstance();

        PaymentData paymentData = MockUtils.aPaymentdata();
        // when: calling paymentRequest() method
        PaymentRequest paymentRequest = MockUtils.aPaylinePaymentRequest();

        paymentRequest.getContractConfiguration().getContractProperties().remove(ContractConfigurationKeys.MERCHANT_IBAN);
        paymentRequest.getContractConfiguration().getContractProperties().put(ContractConfigurationKeys.MERCHANT_IBAN, new ContractProperty(""));

        GenericPaymentRequest genericPaymentRequest = new GenericPaymentRequest(paymentRequest);

        // Build request configuration
        RequestConfiguration requestConfiguration = new RequestConfiguration(
                paymentRequest.getContractConfiguration()
                , paymentRequest.getEnvironment()
                , paymentRequest.getPartnerConfiguration()
        );

        // Create a new PSU
        Psu newPsu = psuHttpclient.createPsu(new PsuCreateRequest.PsuCreateRequestBuilder().build(), requestConfiguration);

        // Build PaymentInitiationRequest (Equens) from PaymentRequest (Payline)
        PaymentInitiationRequest request = service.buildPaymentInitiationRequest(genericPaymentRequest, newPsu, paymentData);

        String chaine = jsonService.toJson(request);

        assertFalse(chaine.contains("CreditorAccount"));

    }

    @Test
    void paymentRequest_check_MerchantName_MerchantIban() {
        doReturn(MockUtils.aPsu()).when(psuHttpclient).createPsu(any(), any());
        JsonService jsonService = JsonService.getInstance();

        PaymentData paymentData = MockUtils.aPaymentdata();
        // when: calling paymentRequest() method
        PaymentRequest paymentRequest = MockUtils.aPaylinePaymentRequest();

        GenericPaymentRequest genericPaymentRequest = new GenericPaymentRequest(paymentRequest);

        // Build request configuration
        RequestConfiguration requestConfiguration = new RequestConfiguration(
                paymentRequest.getContractConfiguration()
                , paymentRequest.getEnvironment()
                , paymentRequest.getPartnerConfiguration()
        );

        // Create a new PSU
        Psu newPsu = psuHttpclient.createPsu(new PsuCreateRequest.PsuCreateRequestBuilder().build(), requestConfiguration);

        // Build PaymentInitiationRequest (Equens) from PaymentRequest (Payline)
        PaymentInitiationRequest request = service.buildPaymentInitiationRequest(genericPaymentRequest, newPsu, paymentData);

        String chaine = jsonService.toJson(request);

        assertTrue(chaine.contains("CreditorAccount"));
        assertTrue(chaine.contains("CreditorName"));
    }

    @Test
    void paymentRequest_pisInitError() {
        // given: the pisHttpClient init throws an exception (invalid certificate data in PartnerConfiguration, for example)
        doThrow(new PluginException("A problem occurred initializing SSL context", FailureCause.INVALID_DATA))
                .when(pisHttpClient)
                .init(any(PartnerConfiguration.class));

        PaymentData paymentData = MockUtils.aPaymentdata();
        // when: calling paymentRequest() method

        PaymentRequest paymentRequest = MockUtils.aPaylinePaymentRequest();
        GenericPaymentRequest genericPaymentRequest = new GenericPaymentRequest(paymentRequest);
        PaymentResponse paymentResponse = service.paymentRequest(genericPaymentRequest, paymentData);

        // then: the exception is properly catch and the payment response is a failure
        assertEquals(PaymentResponseFailure.class, paymentResponse.getClass());
        TestUtils.checkPaymentResponse((PaymentResponseFailure) paymentResponse);
    }

    @Test
    void paymentRequest_psuCreateError() {
        // given: the creation of the PSU fails
        doThrow(new PluginException("partner error: 500 Internal Server Error"))
                .when(psuHttpclient)
                .createPsu(any(PsuCreateRequest.class), any(RequestConfiguration.class));

        PaymentData paymentData = MockUtils.aPaymentdata();
        // when: calling paymentRequest() method
        PaymentRequest paymentRequest = MockUtils.aPaylinePaymentRequest();
        GenericPaymentRequest genericPaymentRequest = new GenericPaymentRequest(paymentRequest);
        PaymentResponse paymentResponse = service.paymentRequest(genericPaymentRequest, paymentData);

        // then: the exception is properly catch and the payment response is a failure
        assertEquals(PaymentResponseFailure.class, paymentResponse.getClass());
        TestUtils.checkPaymentResponse((PaymentResponseFailure) paymentResponse);
    }

    @Test
    void paymentRequest_paymentInitError() {
        PaymentData paymentData = MockUtils.aPaymentdata();
        // given: the payment initiation fails
        doReturn(MockUtils.aPsu())
                .when(psuHttpclient)
                .createPsu(any(PsuCreateRequest.class), any(RequestConfiguration.class));
        doThrow(new PluginException("partner error: 500 Internal Server Error"))
                .when(pisHttpClient)
                .initPayment(any(PaymentInitiationRequest.class), any(RequestConfiguration.class));

        // when: calling paymentRequest() method
        PaymentRequest paymentRequest = MockUtils.aPaylinePaymentRequest();
        GenericPaymentRequest genericPaymentRequest = new GenericPaymentRequest(paymentRequest);
        PaymentResponse paymentResponse = service.paymentRequest(genericPaymentRequest, paymentData);

        // then: the exception is properly catch and the payment response is a failure
        assertEquals(PaymentResponseFailure.class, paymentResponse.getClass());
        TestUtils.checkPaymentResponse((PaymentResponseFailure) paymentResponse);
    }

    @Test
    void paymentRequest_missingContractProperty() {
        PaymentData paymentData = MockUtils.aPaymentdata();
        // given: a property is missing from ContractConfiguration
        ContractConfiguration contractConfiguration = MockUtils.aContractConfiguration(MockUtils.getExampleCountry());
        contractConfiguration.getContractProperties().remove(ContractConfigurationKeys.MERCHANT_IBAN);
        PaymentRequest paymentRequest = MockUtils.aPaylinePaymentRequestBuilder()
                .withContractConfiguration(contractConfiguration)
                .build();
        doReturn(MockUtils.aPsu())
                .when(psuHttpclient)
                .createPsu(any(PsuCreateRequest.class), any(RequestConfiguration.class));

        // when: calling paymentRequest() method
        GenericPaymentRequest genericPaymentRequest = new GenericPaymentRequest(paymentRequest);
        PaymentResponse paymentResponse = service.paymentRequest(genericPaymentRequest, paymentData);

        // then: the exception is properly catch and the payment response is a failure
        assertEquals(PaymentResponseFailure.class, paymentResponse.getClass());
        TestUtils.checkPaymentResponse((PaymentResponseFailure) paymentResponse);
    }


    @Test
    void buildAddress_nominal() {
        // given: a Payline address
        Buyer.Address input = MockUtils.aPaylineAddress();

        // when: feeding it to the method buildAddress()
        Address output = service.buildAddress(input);

        // then: output attributes are not null and the address lines are less than 70 chars long
        assertNotNull(output);
        assertNotNull(output.getCountry());
        assertNotNull(output.getPostCode());
        assertFalse(output.getAddressLines().isEmpty());
        output.getAddressLines().forEach(line -> assertTrue(line.length() <= Address.ADDRESS_LINE_MAX_LENGTH));
    }

    @Test
    void buildAddress_spaceManagement() {
        // given: an address line in which the index 69 is in the middle of a word
        Buyer.Address input = Buyer.Address.AddressBuilder.anAddress()
                .withStreet1("This is an address, but we need to be careful where to split it : notInTheMiddleOfThis")
                .build();

        // when: feeding it to the method buildAddress()
        Address output = service.buildAddress(input);

        assertEquals("This is an address, but we need to be careful where to split it :", output.getAddressLines().get(0));
        assertEquals("notInTheMiddleOfThis", output.getAddressLines().get(1));
    }

    @Test
    void buildAddress_veryLongChunk() {
        // given: an address line with a very long part
        Buyer.Address input = Buyer.Address.AddressBuilder.anAddress()
                .withStreet1("ThisIsAnAddressWithoutAnySpaceAndWeNeedToSplitItSomewaySoWeTruncateBrutallyInTheMiddle")
                .build();

        // when: feeding it to the method buildAddress()
        Address output = service.buildAddress(input);

        assertEquals("ThisIsAnAddressWithoutAnySpaceAndWeNeedToSplitItSomewaySoWeTruncateBru", output.getAddressLines().get(0));
        assertEquals("tallyInTheMiddle", output.getAddressLines().get(1));
    }

    @Test
    void convertAmount() {
        assertNull(service.convertAmount(null));
        // Euro
        assertEquals("0.01", service.convertAmount(new Amount(BigInteger.ONE, Currency.getInstance("EUR"))));
        assertEquals("1.00", service.convertAmount(new Amount(BigInteger.valueOf(100), Currency.getInstance("EUR"))));
        // Yen: no decimal
        assertEquals("100", service.convertAmount(new Amount(BigInteger.valueOf(100), Currency.getInstance("JPY"))));
        // Bahrain Dinar: 3 decimals
        assertEquals("0.100", service.convertAmount(new Amount(BigInteger.valueOf(100), Currency.getInstance("BHD"))));
    }

    @Test
    void buildPaymentInitiationRequest() {
        Psu psu = MockUtils.aPsu();
        PaymentData paymentData = MockUtils.aPaymentdata();
        GenericPaymentRequest genericPaymentRequest = new GenericPaymentRequest(MockUtils.aPaylinePaymentRequest());

        PaymentInitiationRequest paymentInitiationRequest = service.buildPaymentInitiationRequest(genericPaymentRequest, psu, paymentData);
        String ibanFR = MockUtils.getIbanFR();
        PaymentInitiationRequest request = MockUtils.aPaymentInitiationRequest(ibanFR);
        Assertions.assertEquals(request.getAspspId(), paymentInitiationRequest.getAspspId());
        Assertions.assertEquals(request.getChargeBearer(), paymentInitiationRequest.getChargeBearer());
        Assertions.assertEquals(request.getCreditorAccount().getIdentification(), paymentInitiationRequest.getCreditorAccount().getIdentification());
        Assertions.assertEquals(request.getDebtorAccount().getIdentification(), paymentInitiationRequest.getDebtorAccount().getIdentification());
        Assertions.assertEquals(request.getCreditorName(), paymentInitiationRequest.getCreditorName());
        Assertions.assertEquals(request.getEndToEndId(), paymentInitiationRequest.getEndToEndId());
        Assertions.assertEquals(request.getInitiatingPartyReferenceId(), paymentInitiationRequest.getInitiatingPartyReferenceId());
        Assertions.assertEquals(request.getInitiatingPartyReturnUrl(), paymentInitiationRequest.getInitiatingPartyReturnUrl());
        Assertions.assertEquals(request.getPaymentAmount(), paymentInitiationRequest.getPaymentAmount());
        Assertions.assertEquals(request.getPaymentCurrency(), paymentInitiationRequest.getPaymentCurrency());
        Assertions.assertEquals(request.getPaymentProduct(), paymentInitiationRequest.getPaymentProduct());
        Assertions.assertEquals(request.getPreferredScaMethod(), paymentInitiationRequest.getPreferredScaMethod());
        Assertions.assertEquals(request.getPsuId(), paymentInitiationRequest.getPsuId());
        Assertions.assertEquals(request.getPsuSessionInformation().getHeaderUserAgent(), paymentInitiationRequest.getPsuSessionInformation().getHeaderUserAgent());
        Assertions.assertEquals(request.getPsuSessionInformation().getIpAddress(), paymentInitiationRequest.getPsuSessionInformation().getIpAddress());
        Assertions.assertEquals(request.getPurposeCode(), paymentInitiationRequest.getPurposeCode());
        Assertions.assertEquals(request.getRemittanceInformation(), paymentInitiationRequest.getRemittanceInformation());
        Assertions.assertEquals(request.getRemittanceInformationStructured().getReference(), paymentInitiationRequest.getRemittanceInformationStructured().getReference());
        Assertions.assertEquals(request.getRiskInformation().getChannelType(), paymentInitiationRequest.getRiskInformation().getChannelType());
        Assertions.assertEquals(request.getRiskInformation().getMerchantCategoryCode(), paymentInitiationRequest.getRiskInformation().getMerchantCategoryCode());
        Assertions.assertEquals(request.getRiskInformation().getMerchantCustomerId(), paymentInitiationRequest.getRiskInformation().getMerchantCustomerId());
        Assertions.assertEquals(request.getDebtorName(), paymentInitiationRequest.getDebtorName());
    }

    @Test
    void buildPaymentInitiationRequest_EmptyIBANForSpain() {
        Psu psu = MockUtils.aPsu();
        PaymentData paymentData = MockUtils.aPaymentDataBuilder()
                .withIban("")
                .build();
        // create a genericPaymentRequest with an empty IBAN and a BIC from Spain
        GenericPaymentRequest genericPaymentRequest = new GenericPaymentRequest(
                MockUtils.aPaylinePaymentRequestBuilder().withPaymentFormContext(MockUtils.aPaymentFormContext(""))
                        .build());

        Assertions.assertThrows(InvalidDataException.class,
                () -> service.buildPaymentInitiationRequest(genericPaymentRequest, psu, paymentData),
                "IBAN is required for Spain"
        );
    }

    @Test
    void buildPaymentInitiationRequest_WrongIBAN() {
        Psu psu = MockUtils.aPsu();
        PaymentData paymentData = MockUtils.aPaymentDataBuilder()
                .withIban("IT123456789")
                .build();
        // create a GenericPaymentRequest with an IBAN from a country not accepted, and the merchant lets all banks open
        GenericPaymentRequest genericPaymentRequest = new GenericPaymentRequest(
                MockUtils.aPaylinePaymentRequestBuilder()
                        .withPaymentFormContext(MockUtils.aPaymentFormContext("IT123456789"))
                        .withContractConfiguration(MockUtils.aContractConfiguration("TOUS"))
                        .build());

        Assertions.assertThrows(InvalidDataException.class,
                () -> service.buildPaymentInitiationRequest(genericPaymentRequest, psu, paymentData),
                "IBAN should be from a country available by the merchant "
        );
    }

    @Test
    void buildPaymentInitiationRequest_WrongCountryIBAN() {
        Psu psu = MockUtils.aPsu();
        PaymentData paymentData = MockUtils.aPaymentDataBuilder()
                .withIban(MockUtils.getIbanES())
                .build();
        // create a GenericPaymentRequest with and IBAn from Spain, and the merchant only want it from France
        GenericPaymentRequest genericPaymentRequest = new GenericPaymentRequest(
                MockUtils.aPaylinePaymentRequestBuilder().withPaymentFormContext(MockUtils.aPaymentFormContext(MockUtils.getIbanES()))
                        .build());

        Assertions.assertThrows(InvalidDataException.class,
                () -> service.buildPaymentInitiationRequest(genericPaymentRequest, psu, paymentData),
                "IBAN should be from a country available by the merchant "
        );
    }
}