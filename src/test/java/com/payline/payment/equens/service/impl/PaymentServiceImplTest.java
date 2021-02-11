package com.payline.payment.equens.service.impl;

import com.payline.payment.equens.MockUtils;
import com.payline.payment.equens.bean.business.payment.PaymentData;
import com.payline.payment.equens.service.GenericPaymentService;
import com.payline.payment.equens.utils.constant.RequestContextKeys;
import com.payline.pmapi.bean.payment.PaymentFormContext;
import com.payline.pmapi.bean.payment.RequestContext;
import com.payline.pmapi.bean.payment.request.PaymentRequest;
import com.payline.pmapi.bean.payment.response.PaymentResponse;
import com.payline.pmapi.bean.payment.response.impl.PaymentResponseFormUpdated;
import com.payline.pmapi.bean.payment.response.impl.PaymentResponseRedirect;
import com.payline.pmapi.bean.paymentform.bean.form.BankTransferForm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PaymentServiceImplTest {

    @InjectMocks
    @Spy
    private PaymentServiceImpl service;

    @Mock
    private GenericPaymentService genericPaymentService;

    @Captor
    ArgumentCaptor<PaymentData> paymentDataCaptor;

    @BeforeEach
    void setup() {
        service = new PaymentServiceImpl();
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void paymentRequest_bankWithIBANNeeded() throws Exception {
        Mockito.doReturn(true).when(service).doesNeedIBAN(anyString(), anyString());

        // given: a valid payment request
        Map<String, String> parameters = new HashMap<>();
        parameters.put(BankTransferForm.BANK_KEY, "FOOO");
        PaymentFormContext context = PaymentFormContext.PaymentFormContextBuilder.aPaymentFormContext()
                .withPaymentFormParameter(parameters)
                .build();

        PaymentRequest paymentRequest = MockUtils.aPaylinePaymentRequestBuilder()
                .withPaymentFormContext(context)
                .build();

        // when: calling paymentRequest() method
        PaymentResponse paymentResponse = service.paymentRequest(paymentRequest);

        // then: the payment response is a FormUpdated
        assertEquals(PaymentResponseFormUpdated.class, paymentResponse.getClass());
        PaymentResponseFormUpdated responseFormUpdated = (PaymentResponseFormUpdated) paymentResponse;
        assertNotNull(responseFormUpdated.getRequestContext());
        assertTrue(responseFormUpdated.getRequestContext().getRequestData().containsKey(RequestContextKeys.BANK));

        verify(genericPaymentService, never()).paymentRequest(any(), paymentDataCaptor.capture());
    }

    @Test
    void paymentRequest_bankWithoutIBANNeeded() throws Exception {
        Mockito.doReturn(false).when(service).doesNeedIBAN(anyString(), anyString());

        // given: a valid payment request
        Map<String, String> parameters = new HashMap<>();
        parameters.put(BankTransferForm.BANK_KEY, "FOOO");
        PaymentFormContext context = PaymentFormContext.PaymentFormContextBuilder.aPaymentFormContext()
                .withPaymentFormParameter(parameters)
                .build();

        PaymentRequest paymentRequest = MockUtils.aPaylinePaymentRequestBuilder()
                .withPaymentFormContext(context)
                .build();

        PaymentResponseRedirect.RedirectionRequest redirectionRequest = PaymentResponseRedirect.RedirectionRequest.RedirectionRequestBuilder
                .aRedirectionRequest()
                .withRequestType(PaymentResponseRedirect.RedirectionRequest.RequestType.GET)
                .withUrl(new URL("http://www.foo.com"))
                .build();

        PaymentResponseRedirect responseRedirect = PaymentResponseRedirect.PaymentResponseRedirectBuilder
                .aPaymentResponseRedirect()
                .withPartnerTransactionId("123123")
                .withStatusCode("foo")
                .withRedirectionRequest(redirectionRequest)
                .build();

        doReturn(responseRedirect).when(genericPaymentService).paymentRequest(any(), paymentDataCaptor.capture());

        // when: calling paymentRequest() method
        PaymentResponse paymentResponse = service.paymentRequest(paymentRequest);

        // then: the payment response is a FormUpdated
        assertEquals(PaymentResponseRedirect.class, paymentResponse.getClass());
        assertEquals("FOOO", paymentDataCaptor.getValue().getBic());
        assertNull(paymentDataCaptor.getValue().getIban());

        verify(genericPaymentService, only()).paymentRequest(any(), paymentDataCaptor.capture());
    }

    @Test
    void paymentRequest_bankAndIBAN() throws Exception {
        // given: a valid payment request
        Map<String, String> parameters = new HashMap<>();
        parameters.put(BankTransferForm.IBAN_KEY, "thisIsAnIBAN");
        PaymentFormContext formContext = PaymentFormContext.PaymentFormContextBuilder.aPaymentFormContext()
                .withSensitivePaymentFormParameter(parameters)
                .build();

        Map<String, String> requestData = new HashMap<>();
        requestData.put(RequestContextKeys.BANK, "FOOO");
        RequestContext requestContext = RequestContext.RequestContextBuilder
                .aRequestContext()
                .withRequestData(requestData)
                .build();

        PaymentRequest paymentRequest = MockUtils.aPaylinePaymentRequestBuilder()
                .withPaymentFormContext(formContext)
                .withRequestContext(requestContext)
                .build();

        PaymentResponseRedirect.RedirectionRequest redirectionRequest = PaymentResponseRedirect.RedirectionRequest.RedirectionRequestBuilder
                .aRedirectionRequest()
                .withRequestType(PaymentResponseRedirect.RedirectionRequest.RequestType.GET)
                .withUrl(new URL("http://www.foo.com"))
                .build();

        PaymentResponseRedirect responseRedirect = PaymentResponseRedirect.PaymentResponseRedirectBuilder
                .aPaymentResponseRedirect()
                .withPartnerTransactionId("123123")
                .withStatusCode("foo")
                .withRedirectionRequest(redirectionRequest)
                .build();

        doReturn(responseRedirect).when(genericPaymentService).paymentRequest(any(), paymentDataCaptor.capture());

        // when: calling paymentRequest() method
        PaymentResponse paymentResponse = service.paymentRequest(paymentRequest);

        // then: the payment response is a FormUpdated
        assertEquals(PaymentResponseRedirect.class, paymentResponse.getClass());
        verify(genericPaymentService, only()).paymentRequest(any(), any());
        assertEquals("FOOO", paymentDataCaptor.getValue().getBic());
        assertEquals("thisIsAnIBAN", paymentDataCaptor.getValue().getIban());
    }

    @Test
    void doesNeedIBAN() {
        String aspspsJson = "{\"Application\":\"PIS\",\"ASPSP\":[" +
                "{\"AspspId\":\"1234\",\"Name\":[\"a Bank\"],\"CountryCode\":\"FR\",\"Details\":[{\"Api\":\"POST /payments\",\"Fieldname\":\"PaymentProduct\",\"Value\":\"Normal|Instant\",\"ProtocolVersion\":\"STET_V_1_4_0_47\"}],\"BIC\":\"MOOBARBAZXX\"}," +
                "{\"AspspId\":\"224\",\"CountryCode\":\"DE\",\"Name\":[\"another Bank\"],\"Details\":[{\"Api\":\"POST /payments\",\"Fieldname\":\"PaymentProduct\",\"Value\":\"Instant\",\"ProtocolVersion\":\"STET_V_1_4_0_47\"},{\"Api\": \"POST /payments\",\"Fieldname\": \"DebtorAccount\",\"Type\": \"MANDATORY\",\"ProtocolVersion\": \"BG_V_1_3_0\" }]}," +
                "{\"AspspId\":\"224\",\"CountryCode\":\"DE\",\"Name\":[\"a third Bank\"],\"Details\":[{\"Api\":\"POST /payments\",\"Fieldname\":\"PaymentProduct\",\"Value\":\"Instant\",\"ProtocolVersion\":\"STET_V_1_4_0_47\"},{\"Api\": \"POST /payments\",\"Fieldname\": \"DebtorAccount\",\"Type\": \"FOO\",\"ProtocolVersion\": \"BG_V_1_3_0\" }]}" +
                "],\"MessageCreateDateTime\":\"2019-11-15T16:52:37.092+0100\",\"MessageId\":\"6f31954f-7ad6-4a63-950c-a2a363488e\"}";

        assertFalse(service.doesNeedIBAN(null, null));
        assertFalse(service.doesNeedIBAN(null, aspspsJson));
        assertFalse(service.doesNeedIBAN("", null));
        assertFalse(service.doesNeedIBAN("", aspspsJson));
        assertFalse(service.doesNeedIBAN("FOOO", null));
        assertFalse(service.doesNeedIBAN("FOOO", ""));
        assertFalse(service.doesNeedIBAN("FOOO", aspspsJson));
        assertFalse(service.doesNeedIBAN("a Bank", aspspsJson));
        assertTrue(service.doesNeedIBAN("another Bank", aspspsJson));
        assertFalse(service.doesNeedIBAN("a third Bank", aspspsJson));
    }
}
