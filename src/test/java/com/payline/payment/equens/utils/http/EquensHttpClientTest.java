package com.payline.payment.equens.utils.http;

import com.payline.payment.equens.MockUtils;
import com.payline.payment.equens.bean.configuration.RequestConfiguration;
import com.payline.payment.equens.exception.InvalidDataException;
import com.payline.payment.equens.exception.PluginException;
import com.payline.payment.equens.utils.constant.ContractConfigurationKeys;
import com.payline.payment.equens.utils.security.RSAHolder;
import com.payline.pmapi.bean.configuration.PartnerConfiguration;
import com.payline.pmapi.bean.payment.ContractConfiguration;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.HttpRequestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.tomitribe.auth.signatures.Signature;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EquensHttpClientTest {

    /**
     * Private class required to test the sbstract class {@link EquensHttpClient}.
     */
    private static class TestableHttpClient extends EquensHttpClient {
        @Override
        protected String appName() {
            return "TEST";
        }
    }

    private static final String HTTP_SIGNATURE_PATTERN = "Signature\\s+((keyId|signature|algorithm|created|expires|headers)=\".*\"[\\s,]*){2,6}";

    @Spy
    @InjectMocks
    private TestableHttpClient equensHttpClient;

    @Mock
    private RSAHolder rsaHolder;

    @BeforeEach
    void setup() throws NoSuchFieldException {
        // Init tested instance and inject mocks
        equensHttpClient = new TestableHttpClient();
        MockitoAnnotations.initMocks(this);
    }

    @AfterEach
    void verifyMocks() throws IOException {
        /* verify that execute() method is never called ! it ensures the mocks are working properly and there is no
        false negative that could be related to a failed HTTP request sent to the partner API. */
        verify( equensHttpClient, never() ).execute( any( HttpRequestBase.class ) );
    }

    // --- Test EquensHttpClient#authorizationHeaders ---

    @Test
    void authorizationHeaders_missingclientName(){
        // given: client name is missing from ContractConfiguration
        String uri = "http://test.domain.fr/path";
        ContractConfiguration contractConfiguration = MockUtils.aContractConfiguration(MockUtils.getExampleCountry());
        contractConfiguration.getContractProperties().remove(ContractConfigurationKeys.CLIENT_NAME);
        RequestConfiguration requestConfiguration = new RequestConfiguration(contractConfiguration, MockUtils.anEnvironment(), MockUtils.aPartnerConfiguration());

        // when: calling authorizationHeaders(), then: an exception is thrown
        assertThrows(InvalidDataException.class, () -> equensHttpClient.authorizationHeaders(uri, requestConfiguration));
    }

    @Test
    void authorizationHeaders_missingOnboardingId(){
        // given: onboarding id is missing from ContractConfiguration
        String uri = "http://test.domain.fr/path";
        ContractConfiguration contractConfiguration = MockUtils.aContractConfiguration(MockUtils.getExampleCountry());
        contractConfiguration.getContractProperties().remove(ContractConfigurationKeys.ONBOARDING_ID);
        RequestConfiguration requestConfiguration = new RequestConfiguration(contractConfiguration, MockUtils.anEnvironment(), MockUtils.aPartnerConfiguration());

        // when: calling authorizationHeaders(), then: an exception is thrown
        assertThrows(InvalidDataException.class, () -> equensHttpClient.authorizationHeaders(uri, requestConfiguration));
    }

    @Test
    void authorizationHeaders_nominal(){
        // given: the method generateSignature returns a valid signature
        String uri = "http://test.domain.fr/path";
        RequestConfiguration requestConfiguration = MockUtils.aRequestConfiguration();
        doReturn( MockUtils.aSignature() ).when( equensHttpClient ).generateSignature( anyString(), anyMap() );

        // when: calling authorizationHeaders() method
        Map<String, String> headers = equensHttpClient.authorizationHeaders( uri, requestConfiguration );

        // then: every authorization header required by Equens is present AND the header Authorization contains a valid signature
        assertNotNull( headers );
        assertFalse( headers.isEmpty() );
        assertEquals( equensHttpClient.appName(), headers.get( EquensHttpClient.HEADER_AUTH_APP) );
        assertNotNull( headers.get( EquensHttpClient.HEADER_AUTH_CLIENT) );
        assertNotNull( headers.get( EquensHttpClient.HEADER_AUTH_DATE) );
        assertNotNull( headers.get( EquensHttpClient.HEADER_AUTH_ID) );
        assertNotNull( headers.get( HttpHeaders.AUTHORIZATION ) );
        assertTrue( headers.get( HttpHeaders.AUTHORIZATION ).matches( HTTP_SIGNATURE_PATTERN ) );
    }

    // --- Test EquensHttpClient#generateSignature ---

    @Test
    void generateSignature_nominal() throws UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException {
        // given: valid input & RsaHolder returns valid pk and certificate
        String uri = "http://test.domain.fr/path";
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put( EquensHttpClient.HEADER_AUTH_APP, "TEST" );
        headers.put( EquensHttpClient.HEADER_AUTH_CLIENT, "Thales" );
        headers.put( EquensHttpClient.HEADER_AUTH_DATE, "01/01/2021" );
        headers.put( EquensHttpClient.HEADER_AUTH_ID, "0" );
        doReturn( MockUtils.aPrivateKey() ).when( rsaHolder ).getPrivateKey();
        doReturn( MockUtils.aClientCertificate() ).when( rsaHolder ).getClientCertificate();

        // when: generating the signature
        Signature signature = equensHttpClient.generateSignature( uri, headers );

        // then: the signature is valid
        assertNotNull( signature );
        assertTrue( signature.toString().matches( HTTP_SIGNATURE_PATTERN ) );
    }

    // --- Test EquensHttpClient#handleError ---

    @ParameterizedTest
    @MethodSource("handleError_set")
    void handleError( StringResponse apiResponse ){
        // when: handling the error
        PluginException exception = equensHttpClient.handleError( apiResponse );

        // then: no matter the error response provided by the API, the resulting PluginException must have an error code and a failure cause.
        assertNotNull( exception );
        assertNotNull( exception.getErrorCode() );
        assertNotNull( exception.getFailureCause() );
    }
    static Stream<StringResponse> handleError_set(){
        Stream.Builder<StringResponse> builder = Stream.builder();

        // The API returns a response which content matches the specification
        String contentMatchingSpec = "{" +
                "  \"MessageCreateDateTime\":\"2019-11-19T08:56:24.181+0000\"," +
                "  \"MessageId\":\"8793c366c134477fb6499b061c7b638a\"," +
                "  \"code\":\"002\"," +
                "  \"message\":\"The message does not comply the schema definition\"," +
                "  \"details\":\"Unrecognized field \\\"toto\\\" (class com.equensworldline.psu.v1.model.PsuCreateRequest), not marked as ignorable\"" +
                "}";
        builder.accept( HttpTestUtils.mockStringResponse( 400, "Bad Request", contentMatchingSpec ) );

        // The API returns a response which content does not match the specification
        String contentNotMatchingSpec = "[{" +
                "  \"errorCode\":\"002\"," +
                "  \"errorMessage\":\"This message does not comply the schema definition\"" +
                "}]";
        builder.accept( HttpTestUtils.mockStringResponse( 400, "Bad Request", contentNotMatchingSpec ) );

        // The API returns a response with an invalid content
        builder.accept( HttpTestUtils.mockStringResponse( 500, "Internal Server Error", "{/}" ) );

        // The API returns a response with an empty content
        builder.accept( HttpTestUtils.mockStringResponse( 401, "Unauthorized", "" ) );

        // The API returns a response without content
        builder.accept( HttpTestUtils.mockStringResponse( 404, "Not Found", null ) );

        return builder.build();
    }

    // --- Test EquensHttpClient#initHeaders ---

    @Test
    void initHeaders(){
        // given: authorize() method returns a valid authorization
        doReturn( MockUtils.anAuthorization() )
                .when( equensHttpClient )
                .authorize( any(RequestConfiguration.class) );

        // when: initializing the headers
        List<Header> headers = equensHttpClient.initHeaders( MockUtils.aRequestConfiguration() );

        // then: headers list contains the header Authorization
        assertFalse( headers.isEmpty() );
        assertEquals( HttpHeaders.AUTHORIZATION, headers.get(0).getName() );
    }

}
