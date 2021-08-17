package com.payline.payment.equens.utils.http;

import com.payline.payment.equens.MockUtils;
import com.payline.payment.equens.bean.business.payment.PaymentInitiationRequest;
import com.payline.payment.equens.bean.business.payment.PaymentInitiationResponse;
import com.payline.payment.equens.bean.business.payment.PaymentStatusResponse;
import com.payline.payment.equens.bean.business.reachdirectory.GetAspspsResponse;
import com.payline.payment.equens.bean.configuration.RequestConfiguration;
import com.payline.payment.equens.exception.InvalidDataException;
import com.payline.payment.equens.exception.PluginException;
import com.payline.pmapi.bean.common.FailureCause;
import com.payline.pmapi.bean.configuration.PartnerConfiguration;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.HttpRequestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PisHttpClientTest {

    @Spy
    @InjectMocks
    private PisHttpClient pisHttpClient = new PisHttpClient();

    private String paymentId = MockUtils.aPaymentId();

    private RequestConfiguration goodRequestConfiguration = MockUtils.aRequestConfiguration();

    @BeforeEach
    void setup() {
        MockitoAnnotations.initMocks(this);
        // Mock a valid authorization
        doReturn(MockUtils.anAuthorization()).when(pisHttpClient).authorize(any(RequestConfiguration.class));
    }

    @AfterEach
    void verifyMocks() {
        /* verify that execute() method is never called ! it ensures the mocks are working properly and there is no
        false negative that could be related to a failed HTTP request sent to the partner API. */
        verify(pisHttpClient, never()).execute(any(HttpRequestBase.class));
    }

    // --- Test PisHttpClient#getAspsps ---

    @Test
    void getAspsps_nominal() {
        // given: the partner API returns a valid success response
        String responseBody = "{" +
                "  \"MessageCreateDateTime\":\"2019-11-15T15:52:37.092+0000\"," +
                "  \"MessageId\":\"6f31954f-7ad6-4a63-950c-a2a363488e\"," +
                "  \"Application\":\"PIS\"," +
                "  \"ASPSP\":[" +
                "    {" +
                "      \"AspspId\":\"7005\"," +
                "      \"Name\":[\"Ing Bank\"]," +
                "      \"CountryCode\":\"NL\"," +
                "      \"Details\":[{\"ProtocolVersion\":\"ING_V_0_9_3\"}]," +
                "      \"BIC\":\"TODO\"}" +
                "  ]" +
                "}";

        String goodUrl = "https://xs2a.awltest.de/xs2a/routingservice/services/directory/v1/aspsps?allDetails=true";
        doReturn(HttpTestUtils.mockStringResponse(200, "OK", responseBody))
                .when(pisHttpClient)
                .get(anyString(), anyList());

        // when: calling the method
        GetAspspsResponse response = pisHttpClient.getAspsps(goodRequestConfiguration);
        Mockito.verify(pisHttpClient, atLeastOnce()).get(eq(goodUrl), any());

        // then: the list contains 1 Aspsp
        assertNotNull(response);
        assertEquals(1, response.getAspsps().size());
    }

    @Test
    void getAspsps_invalidConfig() {
        // given: the config property containing the path is missing
        PartnerConfiguration partnerConfiguration = new PartnerConfiguration(new HashMap<>(), new HashMap<>());
        RequestConfiguration requestConfiguration = new RequestConfiguration(MockUtils.aContractConfiguration("FR"), MockUtils.anEnvironment(), partnerConfiguration);

        // when: calling the method, then: an exception is thrown
        assertThrows(InvalidDataException.class, () -> pisHttpClient.getAspsps(requestConfiguration));
    }

    @Test
    void getAspsps_noListInResponse() {
        // given: the partner API returns a invalid success response, without any Aspsp list
        String responseBody = "{" +
                "  \"MessageCreateDateTime\":\"2019-11-15T15:52:37.092+0000\"," +
                "  \"MessageId\":\"6f31954f-7ad6-4a63-950c-a2a363488e\"," +
                "  \"Application\":\"PIS\"" +
                "}";
        doReturn(HttpTestUtils.mockStringResponse(200, "OK", responseBody))
                .when(pisHttpClient)
                .get(anyString(), anyList());

        // when: calling the method
        GetAspspsResponse response = pisHttpClient.getAspsps(goodRequestConfiguration);

        // then: resulting list is null
        assertNull(response.getAspsps());
    }

    @Test
    void getAspsps_error() {
        // given: the partner API returns a valid error response
        doReturn(HttpTestUtils.mockStringResponse(401, "Unauthorized", ""))
                .when(pisHttpClient)
                .get(anyString(), anyList());

        // when: calling the method, then: an exception is thrown
        assertThrows(PluginException.class, () -> pisHttpClient.getAspsps(goodRequestConfiguration));
    }

    @Test
    void getAspsps_notJson() {
        // given: the partner API returns a valid error response
        // @see PAYLAPMEXT-265
        String error = "<html><head><title>HTML Error 502</title></head><body><p><h2>equensWorldline - HTML Error 502</h2></p></body></html>";
        doReturn(HttpTestUtils.mockStringResponse(200, "", error))
                .when(pisHttpClient)
                .get(anyString(), anyList());

        // when: calling the method, then: an exception is thrown
        PluginException e = assertThrows(PluginException.class, () -> pisHttpClient.getAspsps(goodRequestConfiguration));
        Assertions.assertEquals(error.substring(0,50), e.getErrorCode());
    }

    // --- Test PisHttpClient#initPayment ---

    @Test
    void initPayment_nominal() {
        // given: the partner API returns a valid success response
        String redirectionUrl = "https://xs2a.banking.co.at/xs2a-sandbox/m044/v1/pis/confirmation/btWMz6mTz7I3SOe4lMqXiwciqe6igXBCeebfVWlmZ8N8zVw_qRKMMuhlLLXtPrVcBeH6HIP2qhdTTZ1HINXSkg==_=_psGLvQpt9Q/authorisations/fa8e44a7-3bf7-4543-82d1-5a1163aaaaad";
        String responseContent = "{\n" +
                "    \"MessageCreateDateTime\": \"2019-11-19T16:35:52.244+0000\",\n" +
                "    \"MessageId\": \"e8683740-38be-4026-b48e-72089b023e\",\n" +
                "    \"PaymentId\": \"130436\",\n" +
                "    \"InitiatingPartyReferenceId\": \"REF1574181352\",\n" +
                "    \"PaymentStatus\": \"Open\",\n" +
                "    \"Links\":{\"AspspRedirectUrl\":{\"Href\":\"" + redirectionUrl + "\"}}}";
        doReturn(HttpTestUtils.mockStringResponse(201, "Created", responseContent))
                .when(pisHttpClient)
                .post(anyString(), anyList(), any(HttpEntity.class));

        // when: initializing a payment
        PaymentInitiationResponse response = pisHttpClient.initPayment(MockUtils.aPaymentInitiationRequest(MockUtils.getIbanFR()), goodRequestConfiguration, MockUtils.aPISHttpClientHeader());

        // then: the response contains the redirection URL
        assertNotNull(response);
        assertNotNull(response.getLinks());
        assertNotNull(response.getLinks().getAspspRedirectUrl());
        assertNotNull(response.getLinks().getAspspRedirectUrl().getHref());

        assertEquals(redirectionUrl, response.getLinks().getAspspRedirectUrl().getHref().toString());

        // verify the post() method has been called and the content of the arguments passed
        ArgumentCaptor<List<Header>> headersCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<HttpEntity> bodyCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(pisHttpClient, times(1)).post(anyString(), headersCaptor.capture(), bodyCaptor.capture());
        this.verifyAuthorizationHeader(headersCaptor.getValue());
        assertNotNull(bodyCaptor.getValue());
    }

    @Test
    void initPayment_invalidConfig() {
        // given: the config property containing the path is missing
        PartnerConfiguration partnerConfiguration = new PartnerConfiguration(new HashMap<>(), new HashMap<>());
        RequestConfiguration requestConfiguration = new RequestConfiguration(MockUtils.aContractConfiguration("FR"), MockUtils.anEnvironment(), partnerConfiguration);

        // when: calling the method, then: an exception is thrown
        PaymentInitiationRequest request = MockUtils.aPaymentInitiationRequest(MockUtils.getIbanFR());
        assertThrows(InvalidDataException.class, () -> pisHttpClient.initPayment( request, requestConfiguration, MockUtils.aPISHttpClientHeader()));
    }

    @Test
    void initPayment_badRequest() {
        // given: the partner API returns a 400 Bad Request
        String responseContent = "{" +
                "    \"code\":\"002\"," +
                "    \"message\":\"The message does not comply the schema definition\"," +
                "    \"details\":\"Property paymentAmount : must not be null\"," +
                "    \"MessageCreateDateTime\":\"2019-11-25T15:02:36.555+0100\"," +
                "    \"MessageId\":\"c2a4ce10086547019b1d50411ea6a99e\"" +
                "}";
        doReturn(HttpTestUtils.mockStringResponse(400, "Bad Request", responseContent))
                .when(pisHttpClient)
                .post(anyString(), anyList(), any(HttpEntity.class));

        // when: initializing a payment, then: an exception is thrown
        PaymentInitiationRequest request = MockUtils.aPaymentInitiationRequest(MockUtils.getIbanFR());
        PluginException thrown = assertThrows(PluginException.class, () -> pisHttpClient.initPayment(request, goodRequestConfiguration, MockUtils.aPISHttpClientHeader()));
        Assertions.assertNotNull(thrown.getErrorCode());
        Assertions.assertEquals("Property paymentAmount : must not be null", thrown.getErrorCode());
        Assertions.assertNotNull(thrown.getFailureCause());
        Assertions.assertEquals(FailureCause.INVALID_DATA, thrown.getFailureCause());
    }

    // --- Test PisHttpClient#paymentStatus ---

    @Test
    void paymentStatus_nominal() {
        // given: the partner API returns a valid success response
        String paymentId = "130676";
        String responseContent = "{\n" +
                "    \"MessageCreateDateTime\": \"2019-11-20T13:44:35.115+0000\",\n" +
                "    \"MessageId\": \"ca58925c-57cc-44b0-a827-cd439fb87f\",\n" +
                "    \"PaymentId\": \"" + paymentId + "\",\n" +
                "    \"PaymentStatus\": \"AUTHORISED\",\n" +
                "    \"AspspPaymentId\": \"im7QC5rZ-jyNr237sJb6VqEnBd8uNDnU6b9-rnAYVxTNub1NwmkrY3CBGDMRXsx5BeH6HIP2qhdTTZ1HINXSkg==_=_psGLvQpt9Q\",\n" +
                "    \"InitiatingPartyReferenceId\": \"REF1574257016\",\n" +
                "    \"DebtorAgent\": \"BNPADEFF\",\n" +
                "    \"DebtorAccount\": \"AT880000000000000001\"\n" +
                "}";
        doReturn(HttpTestUtils.mockStringResponse(200, "OK", responseContent))
                .when(pisHttpClient)
                .get(anyString(), anyList());

        // when: retrieving the payment status
        PaymentStatusResponse response = pisHttpClient.paymentStatus(paymentId, goodRequestConfiguration, false);

        // then: the response contains the status
        assertNotNull(response);
        assertNotNull(response.getPaymentStatus());

        // verify the get() method has been called and the content of the arguments passed
        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<List<Header>> headersCaptor = ArgumentCaptor.forClass(List.class);
        verify(pisHttpClient, times(1)).get(urlCaptor.capture(), headersCaptor.capture());
        assertTrue(urlCaptor.getValue().contains(paymentId));
        this.verifyAuthorizationHeader(headersCaptor.getValue());
    }

    @Test
    void paymentStatus_invalidConfig() {
        // given: the config property containing the path is missing
        PartnerConfiguration partnerConfiguration = new PartnerConfiguration(new HashMap<>(), new HashMap<>());
        RequestConfiguration requestConfiguration = new RequestConfiguration(MockUtils.aContractConfiguration("FR"), MockUtils.anEnvironment(), partnerConfiguration);

        // when: calling the method, then: an exception is thrown
        assertThrows(InvalidDataException.class, () -> pisHttpClient.paymentStatus(paymentId, requestConfiguration, false));
    }

    @Test
    void paymentStatus_notFound() {
        // given: the partner API returns a 400 Bad Request
        String responseContent = "{" +
                "    \"code\":\"110\"," +
                "    \"message\":\"Transaction could not be found\"," +
                "    \"details\":\"Load operation error: transactionId = 666, reason:[nothing found]\"," +
                "    \"MessageCreateDateTime\":\"22019-12-03T15:27:32.629+0000\"," +
                "    \"MessageId\":\"3274abb431c8410f886a903b88285ebd\"" +
                "}";
        doReturn(HttpTestUtils.mockStringResponse(404, "Not Found", responseContent))
                .when(pisHttpClient)
                .get(anyString(), anyList());

        // when: retrieving the payment status, then: an exception is thrown
        PluginException thrown = assertThrows(PluginException.class,
                () -> pisHttpClient.paymentStatus("666", goodRequestConfiguration, false));
        Assertions.assertNotNull(thrown.getErrorCode());
        Assertions.assertEquals("Transaction could not be found", thrown.getErrorCode());
        Assertions.assertNotNull(thrown.getFailureCause());
        Assertions.assertEquals(FailureCause.INVALID_DATA, thrown.getFailureCause());
    }

    private void verifyAuthorizationHeader(List<Header> headers) {
        boolean headerPresent = false;
        for (Header h : headers) {
            if (HttpHeaders.AUTHORIZATION.equals(h.getName())) {
                headerPresent = true;
            }
        }
        assertTrue(headerPresent);
    }

    @Test
    void check_addStringUrlParameter(){

        String url = "https://xs2a.awltest.de/xs2a/routingservice/services/pis/v1/payments/{paymentId}/status";

        url = url.replace("{paymentId}", paymentId);
        url = pisHttpClient.addStringUrlParameter(url, "Param=value");

        assertTrue(url.contains("?") && !url.contains("&"));

        url = pisHttpClient.addStringUrlParameter(url, "confirm=true");

        assertTrue(url.contains("?") && url.contains("&"));
    }

}
