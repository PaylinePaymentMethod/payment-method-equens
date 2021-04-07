package com.payline.payment.equens.service.impl;

import com.payline.payment.equens.MockUtils;
import com.payline.payment.equens.bean.business.payment.PaymentStatus;
import com.payline.payment.equens.bean.configuration.RequestConfiguration;
import com.payline.payment.equens.exception.PluginException;
import com.payline.payment.equens.utils.TestUtils;
import com.payline.payment.equens.utils.http.PisHttpClient;
import com.payline.pmapi.bean.common.FailureCause;
import com.payline.pmapi.bean.configuration.PartnerConfiguration;
import com.payline.pmapi.bean.payment.response.PaymentResponse;
import com.payline.pmapi.bean.payment.response.impl.PaymentResponseFailure;
import com.payline.pmapi.bean.payment.response.impl.PaymentResponseOnHold;
import com.payline.pmapi.bean.payment.response.impl.PaymentResponseSuccess;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class PaymentWithRedirectionServiceImplTest {
    private static String additionalData = "{\"aspspPaymentId\":\"im7QC5rZ-jyNr237sJb6VqEnBd8uNDnU6b9-rnAYVxTNub1NwmkrY3CBGDMRXsx5BeH6HIP2qhdTTZ1HINXSkg\\u003d\\u003d_\\u003d_psGLvQpt9Q\"}";

    @InjectMocks
    private PaymentWithRedirectionServiceImpl service;

    @Mock
    private PisHttpClient pisHttpClient;

    @BeforeEach
    void setup(){
        service = new PaymentWithRedirectionServiceImpl();
        MockitoAnnotations.initMocks( this );
    }

    @ParameterizedTest
    @MethodSource("statusMappingSet")
    void updatePaymentStatus_nominal( PaymentStatus paymentStatus, Class expectedReturnType ){
        // given: the paymentStatus method returns a payment with the given status and reason
        doReturn( MockUtils.aPaymentStatusResponse( paymentStatus ) )
                .when( pisHttpClient )
                .paymentStatus( anyString(), any(RequestConfiguration.class), anyBoolean() );

        // when: calling updateTransactionState method
        PaymentResponse response = service.updatePaymentStatus( MockUtils.aPaymentId(), MockUtils.aRequestConfiguration() );

        // then: the response is of the given type, and complete
        assertEquals( expectedReturnType, response.getClass() );
        if( response instanceof PaymentResponseSuccess ){
            TestUtils.checkPaymentResponse( (PaymentResponseSuccess) response );
            assertEquals("123456",((PaymentResponseSuccess) response).getPartnerTransactionId());
            assertEquals(additionalData,((PaymentResponseSuccess) response).getTransactionAdditionalData());
        } else if( response instanceof PaymentResponseOnHold ){
            TestUtils.checkPaymentResponse( (PaymentResponseOnHold) response );
        } else {
            TestUtils.checkPaymentResponse( (PaymentResponseFailure) response );
            assertEquals("123456",((PaymentResponseFailure) response).getPartnerTransactionId());
            assertEquals(additionalData,((PaymentResponseFailure) response).getTransactionAdditionalData());
        }

        // verify that mock has been called (to prevent false positive due to a RuntimeException)
        verify( pisHttpClient, times(1) )
                .paymentStatus( anyString(), any(RequestConfiguration.class), anyBoolean() );
    }
    static Stream<Arguments> statusMappingSet(){
        return Stream.of(
                Arguments.of( PaymentStatus.OPEN, PaymentResponseSuccess.class ),
                Arguments.of( PaymentStatus.AUTHORISED, PaymentResponseSuccess.class ),
                Arguments.of( PaymentStatus.SETTLEMENT_IN_PROCESS, PaymentResponseSuccess.class ),
                Arguments.of( PaymentStatus.PENDING, PaymentResponseSuccess.class ),
                Arguments.of( PaymentStatus.SETTLEMENT_COMPLETED, PaymentResponseSuccess.class ),
                Arguments.of( PaymentStatus.CANCELLED, PaymentResponseFailure.class ),
                Arguments.of( PaymentStatus.EXPIRED, PaymentResponseFailure.class ),
                Arguments.of( PaymentStatus.ERROR, PaymentResponseFailure.class )
        );
    }

    @Test
    void updatePaymentStatus_pisInitError(){
        // given: the pisHttpClient init throws an exception (invalid certificate data in PartnerConfiguration, for example)
        doThrow( new PluginException( "A problem occurred initializing SSL context", FailureCause.INVALID_DATA ) )
                .when( pisHttpClient )
                .init( any(PartnerConfiguration.class) );

        // when: calling updateTransactionState method
        PaymentResponse response = service.updatePaymentStatus( MockUtils.aPaymentId(), MockUtils.aRequestConfiguration() );

        // then: the exception is properly catch and the payment response is a failure
        assertEquals( PaymentResponseFailure.class, response.getClass() );
        TestUtils.checkPaymentResponse( (PaymentResponseFailure) response );
    }

    @Test
    void updatePaymentStatus_paymentStatusError(){
        // given: the paymentStatus request fails
        doThrow( new PluginException( "partner error: 500 Internal Server Error" ) )
                .when( pisHttpClient )
                .paymentStatus( anyString(), any(RequestConfiguration.class), anyBoolean() );

        // when: calling updateTransactionState method
        PaymentResponse response = service.updatePaymentStatus( MockUtils.aPaymentId(), MockUtils.aRequestConfiguration() );

        // then: the exception is properly catch and the payment response is a failure
        assertEquals( PaymentResponseFailure.class, response.getClass() );
        TestUtils.checkPaymentResponse( (PaymentResponseFailure) response );
    }


}
