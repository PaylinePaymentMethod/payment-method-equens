package com.payline.payment.equens.utils.http;

import com.payline.payment.equens.MockUtils;
import com.payline.payment.equens.bean.configuration.RequestConfiguration;
import com.payline.payment.equens.exception.InvalidDataException;
import com.payline.payment.equens.exception.PluginException;
import com.payline.payment.equens.utils.TestUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HttpContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OAuthHttpClientTest {

    /**
     * Private class required to test the Abstract class {@link OAuthHttpClient}.
     */
    private static class TestableHttpClient extends OAuthHttpClient {
        @Override
        protected Map<String, String> authorizationHeaders(String uri, RequestConfiguration requestConfiguration) {
            return new HashMap<>();
        }
    }

    @Spy
    @InjectMocks
    private TestableHttpClient oAuthHttpClient;

    @Mock
    private CloseableHttpClient client;

    RequestConfiguration requestConfiguration = MockUtils.aRequestConfiguration();
    List<Header> headers = new ArrayList();

    @BeforeEach
    void setup() throws NoSuchFieldException {
        // Init tested instance and inject mocks
        oAuthHttpClient = new TestableHttpClient();
        MockitoAnnotations.initMocks(this);

        // Manual init of private attributes
          oAuthHttpClient.retries = 3;
          oAuthHttpClient.tokenEndpointUrl = "https://authorization.domain.org/token";
          oAuthHttpClient.initialized = new AtomicBoolean(true);
    }

    @AfterEach
    void verifyMocks() throws IOException {
        /* verify that no HTTP call is ever made ! it ensures the mocks are working properly and there is no
        false negative that could be related to a failed request to the partner API. */
        verify(client, never()).execute(any(HttpRequestBase.class), any(HttpContext.class));
    }

    // --- Test OAuthHttpClient#authorize ---

    @Test
    void authorize_notInitialized() throws NoSuchFieldException {
        // given: the HTTP client has not been properly initialized before use

        oAuthHttpClient.initialized = new AtomicBoolean(false);
        // when: calling the authorize method, then: an exception is thrown
        assertThrows(PluginException.class, () -> oAuthHttpClient.authorize(requestConfiguration));

        // verify isAuthorized method is never called
        verify(oAuthHttpClient, never()).isAuthorized();
    }

    @Test
    void authorize_alreadyAuthorized() {
        // given: a valid authorization is already stored in the client
        doReturn(true).when(oAuthHttpClient).isAuthorized();

        // when: calling the authorize method
        oAuthHttpClient.authorize(MockUtils.aRequestConfiguration());

        // then: no HTTP request is built
        verify(oAuthHttpClient, never()).post(anyString(), anyList(), any(HttpEntity.class));
    }

    /**
     * This is a test set of server response contents to an authorization request,
     * which SHOULD prevent the building of a valid Authorization.
     * In all these cases, the authorize() method is expected to throw an exception.
     */
    private static Stream<String> authorize_blockingResponseContent_set() {
        Stream.Builder<String> builder = Stream.builder();
        // non valid JSON content
        builder.accept("{/}");
        // valid JSON content, but not an object
        builder.accept("[]");
        // the response does not contain an access_token
        builder.accept("{\"token_type\":\"Bearer\",\"expires_in\":1800,\"scope\":\"scope_value\"}");

        return builder.build();
    }

    @ParameterizedTest
    @MethodSource("authorize_blockingResponseContent_set")
    void authorize_blockingResponseContent(String responseContent) throws NoSuchFieldException {
        // given: the server returns a response with a non-sufficient content
        final StringResponse response = HttpTestUtils.mockStringResponse(HttpStatus.SC_OK, "OK", responseContent);
        doReturn(response).when(oAuthHttpClient).post(anyString(), anyList(), any(HttpEntity.class));

        // when: calling the authorize method, an exception is thrown
        assertThrows(PluginException.class, () -> oAuthHttpClient.authorize(requestConfiguration));

        // assert the mock is working properly (to avoid false negative)
        verify(oAuthHttpClient, never()).execute(any(HttpRequestBase.class));
    }

    /**
     * This is a test set of server response contents to an authorization request,
     * which SHOULD NOT prevent the service from building a valid Authorization instance.
     * In all these cases, a valid authorization must be built in the end.
     */
    private static Stream<String> authorize_nonBlockingResponseContent_set() {
        Stream.Builder<String> builder = Stream.builder();

        // nominal case
        builder.accept("{\"access_token\":\"ABCD012345679\",\"token_type\":\"Bearer\",\"expires_in\":1800,\"scope\":\"scope_value\"}");
        // no token type
        builder.accept("{\"access_token\":\"ABCD012345679\",\"expires_in\":1800,\"scope\":\"scope_value\"}");
        // no expiration delay
        builder.accept("{\"access_token\":\"ABCD012345679\",\"token_type\":\"Bearer\",\"scope\":\"scope_value\"}");
        // no scope
        builder.accept("{\"access_token\":\"ABCD012345679\",\"token_type\":\"Bearer\",\"expires_in\":1800}");

        return builder.build();
    }

    @ParameterizedTest
    @MethodSource("authorize_nonBlockingResponseContent_set")
    void authorize_nonBlockingResponseContent(String responseContent) throws NoSuchFieldException {
        // given: the server returns a response with a sufficient content
        final StringResponse response = HttpTestUtils.mockStringResponse(HttpStatus.SC_OK, "OK", responseContent);
        doReturn(response).when(oAuthHttpClient).post(anyString(), anyList(), any(HttpEntity.class));

        // when: calling the authorize method
        oAuthHttpClient.authorize(requestConfiguration);

        // then: the client now contains a valid authorization
        assertTrue(oAuthHttpClient.isAuthorized());

        // assert the mock is working properly
        verify(oAuthHttpClient, never()).execute(any(HttpRequestBase.class));
    }

    /**
     * This is a test set of server response errors to an authorization request.
     * In all these cases, the authorize() method is expected to throw a {@link PluginException}.
     */
    private static Stream<Arguments> authorize_error_set() {
        Stream.Builder<Arguments> builder = Stream.builder();

        // nominal case
        builder.accept(Arguments.of("{\"error\":\"invalid_request\",\"error_description\":\"Some description of the error\",\"error_uri\":\"http://authorization.domain.org/token/errorInfo\"}", HttpStatus.SC_BAD_REQUEST, "Bad Request"));
        // only error attribute
        builder.accept(Arguments.of("{\"error\":\"unauthorized_client\"}", HttpStatus.SC_UNAUTHORIZED, "Unauthorized"));
        // invalid JSON
        builder.accept(Arguments.of("{/}", HttpStatus.SC_INTERNAL_SERVER_ERROR, "Internal Server Error"));
        // valid JSON, but does not match the spec
        builder.accept(Arguments.of("[]", HttpStatus.SC_BAD_REQUEST, "Bad Request"));
        builder.accept(Arguments.of("{\"error\":\"[invalid_request, invalid_client]}", HttpStatus.SC_BAD_REQUEST, "Bad Request"));

        return builder.build();
    }

    @ParameterizedTest
    @MethodSource("authorize_error_set")
    void authorize_error(String content, int statusCode, String statusMessage) throws NoSuchFieldException {
        // given: the server returns an error
        final StringResponse response = HttpTestUtils.mockStringResponse(statusCode, statusMessage, content);

        doReturn(response).when(oAuthHttpClient).post(anyString(), anyList(), any(HttpEntity.class));

        // when: calling the authorize method, an exception is thrown
        assertThrows(PluginException.class, () -> oAuthHttpClient.authorize(requestConfiguration));

        // assert the mock is working properly (to avoid false negative)
        verify(oAuthHttpClient, never()).execute(any(HttpRequestBase.class));
    }

    // --- Test OAuthHttpClient#execute ---

    @Test
    void execute_nominal() throws IOException {
        // given: a properly formatted request, which gets a proper response
        HttpGet request = new HttpGet("http://domain.test.fr/endpoint");
        int expectedStatusCode = 200;
        String expectedStatusMessage = "OK";
        String expectedContent = "{\"content\":\"fake\"}";
        Mockito.doReturn(HttpTestUtils.mockHttpResponse(expectedStatusCode, expectedStatusMessage, expectedContent, null))
                .when(client).execute(request);

        // when: sending the request
        StringResponse stringResponse = oAuthHttpClient.execute(request);

        // then: the content of the StringResponse reflects the content of the HTTP response
        assertNotNull(stringResponse);
        assertEquals(expectedStatusCode, stringResponse.getStatusCode());
        assertEquals(expectedStatusMessage, stringResponse.getStatusMessage());
        assertEquals(expectedContent, stringResponse.getContent());
    }

    @Test
    void execute_retry() throws IOException {
        // given: the first 2 requests end up in timeout, the third request gets a response
        HttpGet request = new HttpGet("http://domain.test.fr/endpoint");
        when(client.execute(request))
                .thenThrow(ConnectTimeoutException.class)
                .thenThrow(ConnectTimeoutException.class)
                .thenReturn(HttpTestUtils.mockHttpResponse(200, "OK", "content", null));

        // when: sending the request
        StringResponse stringResponse = oAuthHttpClient.execute(request);

        // then: the client finally gets the response
        assertNotNull(stringResponse);
    }

    @Test
    void execute_retryFail() throws IOException {
        // given: a request which always gets an exception
        HttpGet request = new HttpGet("http://domain.test.fr/endpoint");
        doThrow(IOException.class).when(client).execute(request);

        // when: sending the request, a PluginException is thrown
        assertThrows(PluginException.class, () -> oAuthHttpClient.execute(request));
    }

    @Test
    void execute_invalidResponse() throws IOException {
        // given: a request that gets an invalid response (null)
        HttpGet request = new HttpGet("http://domain.test.fr/malfunctioning-endpoint");
        doReturn(null).when(client).execute(request);

        // when: sending the request, a PluginException is thrown
        assertThrows(PluginException.class, () -> oAuthHttpClient.execute(request));
    }

    // --- Test OAuthHttpClient#get ---

    @Test
    void get_invalidUrl() {
        // given: the API URL is invalid
        String url = "https:||authorization.domain.org/token";

        // when calling the get() method, an exception is thrown
        assertThrows(InvalidDataException.class, () -> oAuthHttpClient.get(url, headers));

        // assert the mock is working properly (to avoid false negative)
        verify(oAuthHttpClient, never()).execute(any(HttpRequestBase.class));
    }

    @Test
    void get_nominal() throws IOException {
        // given: properly formatted request elements, that would return an OK response
        String url = "https://authorization.domain.org/token";
        List<Header> headers = new ArrayList<>();
        headers.add(new BasicHeader("SomeHeader", "header-value"));
        Mockito.doReturn(HttpTestUtils.mockHttpResponse(200, "OK", "{\"content\":\"success\"}", null))
                .when(client)
                .execute(any(HttpRequestBase.class));

        // when: building and executing the request
        StringResponse stringResponse = oAuthHttpClient.get(url, headers);

        // then: the content of the StringResponse reflects the content of the HTTP response
        assertNotNull(stringResponse);
        assertEquals(200, stringResponse.getStatusCode());
    }

    // --- Test OAuthHttpClient#isAuthorized ---

    @Test
    void isAuthorized_valid() throws NoSuchFieldException {
        // given: a valid authorization
        final Authorization validAuth = MockUtils.anAuthorizationBuilder().build();
        oAuthHttpClient.authorization = validAuth;

        // when called, isAuthorized method returns true
        assertTrue(oAuthHttpClient.isAuthorized());
    }

    @Test
    void isAuthorized_null() throws NoSuchFieldException {
        // given: the client does not contain a valid authorization
        oAuthHttpClient.authorization = null;

        // when called, isAuthorized method returns false
        assertFalse(oAuthHttpClient.isAuthorized());
    }

    @Test
    void isAuthorized_expired() throws NoSuchFieldException {
        // given: an expired authorization
        Authorization expiredAuth = MockUtils.anAuthorizationBuilder()
                .withExpiresAt(TestUtils.addTime(new Date(), Calendar.HOUR, -1))
                .build();
        oAuthHttpClient.authorization = expiredAuth;

        // when called, isAuthorized method returns false
        assertFalse(oAuthHttpClient.isAuthorized());
    }

    // --- Test OAuthHttpClient#post ---

    @Test
    void post_invalidUrl() {
        // given: the API URL is invalid
        String url = "https:||authorization.domain.org/token";

        // when calling the post() method, an exception is thrown
        assertThrows(InvalidDataException.class, () -> oAuthHttpClient.post(url, headers, null));

        // assert the mock is working properly (to avoid false negative)
        verify(oAuthHttpClient, never()).execute(any(HttpRequestBase.class));
    }

    @Test
    void post_nominal() throws IOException {
        // given: properly formatted request elements, that would return an OK response
        String url = "https://authorization.domain.org/token";
        List<Header> headers = new ArrayList<>();
        headers.add(new BasicHeader("SomeHeader", "header-value"));
        StringEntity body = new StringEntity("toto", StandardCharsets.UTF_8);
        Mockito.doReturn(HttpTestUtils.mockHttpResponse(200, "OK", "{\"content\":\"success\"}", null))
                .when(client)
                .execute(any(HttpRequestBase.class));

        // when: building and posting the request
        StringResponse stringResponse = oAuthHttpClient.post(url, headers, body);

        // then: the content of the StringResponse reflects the content of the HTTP response
        assertNotNull(stringResponse);
        assertEquals(200, stringResponse.getStatusCode());
    }

}
