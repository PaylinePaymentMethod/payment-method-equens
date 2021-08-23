package com.payline.payment.equens.service;

import com.payline.payment.equens.MockUtils;
import com.payline.payment.equens.bean.GenericPaymentRequest;
import com.payline.payment.equens.bean.business.payment.Address;
import com.payline.payment.equens.bean.business.payment.PaymentData;
import com.payline.payment.equens.bean.business.payment.PaymentInitiationRequest;
import com.payline.payment.equens.bean.configuration.RequestConfiguration;
import com.payline.payment.equens.exception.InvalidDataException;
import com.payline.payment.equens.exception.PluginException;
import com.payline.payment.equens.service.impl.ConfigurationServiceImpl;
import com.payline.payment.equens.utils.Constants;
import com.payline.payment.equens.utils.TestUtils;
import com.payline.payment.equens.utils.http.PisHttpClient;
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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;


import java.math.BigInteger;
import java.util.Currency;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;


class GenericPaymentServiceTest {


    @Mock
    private PisHttpClient pisHttpClient;


    @InjectMocks
    private GenericPaymentService underTest;

    private JsonService jsonService = JsonService.getInstance();


    @BeforeEach
    void setUp() {
        underTest = GenericPaymentService.getInstance();
        MockitoAnnotations.initMocks(this);
    }

    @Nested
    public class testPaymentRequest {

        @Test
        void nominalCase() {
            doReturn(MockUtils.aPaymentInitiationResponse()).when(pisHttpClient).initPayment(any(), any(), any());

            final PaymentData paymentData = MockUtils.aPaymentdata();
            // when: calling paymentRequest() method
            final PaymentRequest paymentRequest = MockUtils.aPaylinePaymentRequest();
            final GenericPaymentRequest genericPaymentRequest = new GenericPaymentRequest(paymentRequest);
            final PaymentResponse paymentResponse = underTest.paymentRequest(genericPaymentRequest, paymentData);

            // then: the payment response is a success
            assertEquals(PaymentResponseRedirect.class, paymentResponse.getClass());
            TestUtils.checkPaymentResponse((PaymentResponseRedirect) paymentResponse);
        }

        @Test
        void paymentRequest_EmptyMerchantName() {
            final JsonService jsonService = JsonService.getInstance();

            final PaymentData paymentData = MockUtils.aPaymentdata();
            // when: calling paymentRequest() method
            final PaymentRequest paymentRequest = MockUtils.aPaylinePaymentRequest();

            paymentRequest.getContractConfiguration().getContractProperties().remove(Constants.ContractConfigurationKeys.MERCHANT_NAME);
            paymentRequest.getContractConfiguration().getContractProperties().put(Constants.ContractConfigurationKeys.MERCHANT_NAME, new ContractProperty(""));

            GenericPaymentRequest genericPaymentRequest = new GenericPaymentRequest(paymentRequest);

            // Build request configuration
            final RequestConfiguration requestConfiguration = new RequestConfiguration(
                    paymentRequest.getContractConfiguration()
                    , paymentRequest.getEnvironment()
                    , paymentRequest.getPartnerConfiguration()
            );


            // Build PaymentInitiationRequest (Equens) from PaymentRequest (Payline)
            final PaymentInitiationRequest request = underTest.buildPaymentInitiationRequest(genericPaymentRequest, paymentData);

            final String chaine = jsonService.toJson(request);

            assertFalse(chaine.contains("CreditorName"));

        }

        @Test
        void paymentRequest_EmptyMerchantIban() {
            final JsonService jsonService = JsonService.getInstance();

            final PaymentData paymentData = MockUtils.aPaymentdata();
            // when: calling paymentRequest() method
            final PaymentRequest paymentRequest = MockUtils.aPaylinePaymentRequest();

            paymentRequest.getContractConfiguration().getContractProperties().remove(Constants.ContractConfigurationKeys.MERCHANT_IBAN);
            paymentRequest.getContractConfiguration().getContractProperties().put(Constants.ContractConfigurationKeys.MERCHANT_IBAN, new ContractProperty(""));

            final GenericPaymentRequest genericPaymentRequest = new GenericPaymentRequest(paymentRequest);

            // Build request configuration
            final RequestConfiguration requestConfiguration = new RequestConfiguration(
                    paymentRequest.getContractConfiguration()
                    , paymentRequest.getEnvironment()
                    , paymentRequest.getPartnerConfiguration()
            );

            // Build PaymentInitiationRequest (Equens) from PaymentRequest (Payline)
            final PaymentInitiationRequest request = underTest.buildPaymentInitiationRequest(genericPaymentRequest, paymentData);

            final String chaine = jsonService.toJson(request);
            assertFalse(chaine.contains("CreditorAccount"));

        }

        @Test
        void paymentRequest_check_MerchantName_MerchantIban() {
            final JsonService jsonService = JsonService.getInstance();

            final PaymentData paymentData = MockUtils.aPaymentdata();
            // when: calling paymentRequest() method
            final PaymentRequest paymentRequest = MockUtils.aPaylinePaymentRequest();

            final GenericPaymentRequest genericPaymentRequest = new GenericPaymentRequest(paymentRequest);

            // Build request configuration
            final RequestConfiguration requestConfiguration = new RequestConfiguration(
                    paymentRequest.getContractConfiguration()
                    , paymentRequest.getEnvironment()
                    , paymentRequest.getPartnerConfiguration()
            );


            // Build PaymentInitiationRequest (Equens) from PaymentRequest (Payline)
            final PaymentInitiationRequest request = underTest.buildPaymentInitiationRequest(genericPaymentRequest, paymentData);

            final String chaine = jsonService.toJson(request);

            assertTrue(chaine.contains("CreditorAccount"));
            assertTrue(chaine.contains("CreditorName"));
        }

        @Test
        void paymentRequest_pisInitError() {
            // given: the pisHttpClient init throws an exception (invalid certificate data in PartnerConfiguration, for example)
            doThrow(new PluginException("A problem occurred initializing SSL context", FailureCause.INVALID_DATA))
                    .when(pisHttpClient)
                    .init(any(PartnerConfiguration.class));

            final PaymentData paymentData = MockUtils.aPaymentdata();
            // when: calling paymentRequest() method

            final PaymentRequest paymentRequest = MockUtils.aPaylinePaymentRequest();
            final GenericPaymentRequest genericPaymentRequest = new GenericPaymentRequest(paymentRequest);
            final PaymentResponse paymentResponse = underTest.paymentRequest(genericPaymentRequest, paymentData);

            // then: the exception is properly catch and the payment response is a failure
            assertEquals(PaymentResponseFailure.class, paymentResponse.getClass());
            TestUtils.checkPaymentResponse((PaymentResponseFailure) paymentResponse);
        }

        @Test
        void paymentRequest_paymentInitError() {
            final PaymentData paymentData = MockUtils.aPaymentdata();

            doThrow(new PluginException("partner error: 500 Internal Server Error"))
                    .when(pisHttpClient)
                    .initPayment(any(PaymentInitiationRequest.class), any(RequestConfiguration.class), any(List.class));

            // when: calling paymentRequest() method
            final PaymentRequest paymentRequest = MockUtils.aPaylinePaymentRequest();
            final GenericPaymentRequest genericPaymentRequest = new GenericPaymentRequest(paymentRequest);
            final PaymentResponse paymentResponse = underTest.paymentRequest(genericPaymentRequest, paymentData);

            // then: the exception is properly catch and the payment response is a failure
            assertEquals(PaymentResponseFailure.class, paymentResponse.getClass());
            TestUtils.checkPaymentResponse((PaymentResponseFailure) paymentResponse);
        }

        @Test
        void paymentRequest_missingContractProperty() {
            final PaymentData paymentData = MockUtils.aPaymentdata();
            // given: a property is missing from ContractConfiguration
            final ContractConfiguration contractConfiguration = MockUtils.aContractConfiguration(MockUtils.getExampleCountry());
            contractConfiguration.getContractProperties().remove(Constants.ContractConfigurationKeys.MERCHANT_IBAN);
            final PaymentRequest paymentRequest = MockUtils.aPaylinePaymentRequestBuilder()
                    .withContractConfiguration(contractConfiguration)
                    .build();

            // when: calling paymentRequest() method
            final GenericPaymentRequest genericPaymentRequest = new GenericPaymentRequest(paymentRequest);
            final PaymentResponse paymentResponse = underTest.paymentRequest(genericPaymentRequest, paymentData);

            // then: the exception is properly catch and the payment response is a failure
            assertEquals(PaymentResponseFailure.class, paymentResponse.getClass());
            TestUtils.checkPaymentResponse((PaymentResponseFailure) paymentResponse);
        }

    }

    @Nested
    public class buildPaymentInitiationRequest {
        @Test
        void buildPaymentInitiationRequest() {
            final PaymentData paymentData = MockUtils.aPaymentdata();
            final GenericPaymentRequest genericPaymentRequest = new GenericPaymentRequest(MockUtils.aPaylinePaymentRequest());
            final String ibanFR = MockUtils.getIbanFR();
            final PaymentInitiationRequest expected = MockUtils.aPaymentInitiationRequest(ibanFR);

            final PaymentInitiationRequest result = underTest.buildPaymentInitiationRequest(genericPaymentRequest, paymentData);
            assertEquals(result.getAspspId(), expected.getAspspId());
            assertEquals(result.getChargeBearer(), expected.getChargeBearer());
            assertEquals(result.getCreditorAccount().getIdentification(), expected.getCreditorAccount().getIdentification());
            assertEquals(result.getDebtorAccount().getIdentification(), expected.getDebtorAccount().getIdentification());
            assertEquals(result.getCreditorName(), expected.getCreditorName());
            assertEquals(result.getEndToEndId(), expected.getEndToEndId());
            assertEquals(result.getInitiatingPartyReferenceId(), expected.getInitiatingPartyReferenceId());
            assertEquals(result.getPaymentAmount(), expected.getPaymentAmount());
            assertEquals(result.getPaymentCurrency(), expected.getPaymentCurrency());
            assertEquals(result.getPaymentProduct(), expected.getPaymentProduct());
            assertEquals(result.getPreferredScaMethod(), expected.getPreferredScaMethod());
             assertEquals(result.getPurposeCode(), expected.getPurposeCode());
            assertEquals(result.getRemittanceInformation(), expected.getRemittanceInformation());
            assertEquals(result.getRemittanceInformationStructured().getReference(), expected.getRemittanceInformationStructured().getReference());
            assertEquals(result.getPaymentContext().getChannelType(), expected.getPaymentContext().getChannelType());
            assertEquals(result.getPaymentContext().getMerchantCategoryCode(), expected.getPaymentContext().getMerchantCategoryCode());
            assertEquals(result.getPaymentContext().getMerchantCustomerId(), expected.getPaymentContext().getMerchantCustomerId());
            assertEquals(result.getDebtorName(), expected.getDebtorName());
            assertEquals(result.getInitiatingPartySubId(), expected.getInitiatingPartySubId());

        }

        @Test
        void buildPaymentInitiationRequest_AspspNull() {
            final PaymentData paymentData = MockUtils.aPaymentDataBuilder().withAspspId(null).build();
            final ContractConfiguration contractConfiguration = MockUtils.aContractConfiguration(MockUtils.getExampleCountry(), ConfigurationServiceImpl.PaymentProduct.NORMAL.getPaymentProductCode());

            // create a genericPaymentRequest with an empty IBAN and a BIC from Spain
            final GenericPaymentRequest genericPaymentRequest = new GenericPaymentRequest(
                    MockUtils.aPaylinePaymentRequestBuilder()
                            .withPaymentFormContext(MockUtils.aPaymentFormContext(paymentData.getIban()))
                            .build());


            final InvalidDataException thrown = Assertions.assertThrows(InvalidDataException.class,
                    () -> underTest.buildPaymentInitiationRequest(genericPaymentRequest, paymentData));

            assertEquals("AspspId is required", thrown.getMessage());
        }

        @Test
        void buildPaymentInitiationRequest_AspspNotFound() {
            final PaymentData paymentData = MockUtils.aPaymentDataBuilder().withAspspId("9999").build();
            final ContractConfiguration contractConfiguration = MockUtils.aContractConfiguration(MockUtils.getExampleCountry(), ConfigurationServiceImpl.PaymentProduct.NORMAL.getPaymentProductCode());

            // create a genericPaymentRequest with an empty IBAN and a BIC from Spain
            final GenericPaymentRequest genericPaymentRequest = new GenericPaymentRequest(
                    MockUtils.aPaylinePaymentRequestBuilder()
                            .withPaymentFormContext(MockUtils.aPaymentFormContext(paymentData.getIban()))
                            .build());


            final InvalidDataException thrown = Assertions.assertThrows(InvalidDataException.class,
                    () -> underTest.buildPaymentInitiationRequest(genericPaymentRequest, paymentData));

            assertEquals("Aspsp not found", thrown.getMessage());
        }

        @Test
        void buildPaymentInitiationRequest_WrongPaymentMode() {
            final PaymentData paymentData = MockUtils.aPaymentDataBuilder().build();
            final ContractConfiguration contractConfiguration = MockUtils.aContractConfiguration(MockUtils.getExampleCountry(), ConfigurationServiceImpl.PaymentProduct.NORMAL.getPaymentProductCode());

            // create a genericPaymentRequest with an empty IBAN and a BIC from Spain
            final GenericPaymentRequest genericPaymentRequest = new GenericPaymentRequest(
                    MockUtils.aPaylinePaymentRequestBuilder().withContractConfiguration(contractConfiguration)
                            .withPaymentFormContext(MockUtils.aPaymentFormContext(paymentData.getIban()))
                            .build());


            final InvalidDataException thrown = Assertions.assertThrows(InvalidDataException.class,
                    () -> underTest.buildPaymentInitiationRequest(genericPaymentRequest, paymentData));

            assertEquals("Aspsp not compatible with this payment mode", thrown.getMessage());
        }

        @Test
        void buildPaymentInitiationRequest_WrongIBAN() {
            final PaymentData paymentData = MockUtils.aPaymentDataBuilder()
                    .withIban("IT123456789")
                    .build();
            // create a GenericPaymentRequest with an IBAN from a country not accepted, and the merchant lets all banks open
            final GenericPaymentRequest genericPaymentRequest = new GenericPaymentRequest(
                    MockUtils.aPaylinePaymentRequestBuilder()
                            .withPaymentFormContext(MockUtils.aPaymentFormContext("IT123456789"))
                            .withContractConfiguration(MockUtils.aContractConfiguration("TOUS"))
                            .build());

            Assertions.assertThrows(InvalidDataException.class,
                    () -> underTest.buildPaymentInitiationRequest(genericPaymentRequest, paymentData),
                    "IBAN should be from a country available by the merchant "
            );
        }

        @Test
        void buildPaymentInitiationRequest_WrongCountryIBAN() {
            final PaymentData paymentData = MockUtils.aPaymentDataBuilder()
                    .withIban(MockUtils.getIbanES())
                    .build();
            // create a GenericPaymentRequest with and IBAn from Spain, and the merchant only want it from France
            final GenericPaymentRequest genericPaymentRequest = new GenericPaymentRequest(
                    MockUtils.aPaylinePaymentRequestBuilder().withPaymentFormContext(MockUtils.aPaymentFormContext(MockUtils.getIbanES()))
                            .build());

            Assertions.assertThrows(InvalidDataException.class,
                    () -> underTest.buildPaymentInitiationRequest(genericPaymentRequest, paymentData),
                    "IBAN should be from a country available by the merchant "
            );
        }
    }

    @Nested
    public class testBuildAddress {
        @Test
        void buildAddress_nominal() {
            // given: a Payline address
            final Buyer.Address input = MockUtils.aPaylineAddress();

            // when: feeding it to the method buildAddress()
            final Address output = underTest.buildAddress(input);

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
            final Buyer.Address input = Buyer.Address.AddressBuilder.anAddress()
                    .withStreet1("This is an address, but we need to be careful where to split it : notInTheMiddleOfThis")
                    .build();

            // when: feeding it to the method buildAddress()
            final Address output = underTest.buildAddress(input);

            assertEquals("This is an address, but we need to be careful where to split it :", output.getAddressLines().get(0));
            assertEquals("notInTheMiddleOfThis", output.getAddressLines().get(1));
        }

        @Test
        void buildAddress_veryLongChunk() {
            // given: an address line with a very long part
            final Buyer.Address input = Buyer.Address.AddressBuilder.anAddress()
                    .withStreet1("ThisIsAnAddressWithoutAnySpaceAndWeNeedToSplitItSomewaySoWeTruncateBrutallyInTheMiddle")
                    .build();

            // when: feeding it to the method buildAddress()
            final Address output = underTest.buildAddress(input);

            assertEquals("ThisIsAnAddressWithoutAnySpaceAndWeNeedToSplitItSomewaySoWeTruncateBru", output.getAddressLines().get(0));
            assertEquals("tallyInTheMiddle", output.getAddressLines().get(1));
        }

    }



    @Test
    void convertAmount() {
        assertNull(underTest.convertAmount(null));
        // Euro
        assertEquals("0.01", underTest.convertAmount(new Amount(BigInteger.ONE, Currency.getInstance("EUR"))));
        assertEquals("1.00", underTest.convertAmount(new Amount(BigInteger.valueOf(100), Currency.getInstance("EUR"))));
        // Yen: no decimal
        assertEquals("100", underTest.convertAmount(new Amount(BigInteger.valueOf(100), Currency.getInstance("JPY"))));
        // Bahrain Dinar: 3 decimals
        assertEquals("0.100", underTest.convertAmount(new Amount(BigInteger.valueOf(100), Currency.getInstance("BHD"))));
    }
}