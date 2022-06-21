package com.payline.payment.equens.utils.http;

import com.google.gson.JsonSyntaxException;
import com.payline.payment.equens.bean.business.oauth.RFC6749AccessTokenErrorResponse;
import com.payline.payment.equens.bean.business.oauth.RFC6749AccessTokenSuccessResponse;
import com.payline.payment.equens.bean.configuration.RequestConfiguration;
import com.payline.payment.equens.exception.InvalidDataException;
import com.payline.payment.equens.exception.PluginException;
import com.payline.payment.equens.service.JsonService;
import com.payline.payment.equens.utils.PluginUtils;
import com.payline.payment.equens.utils.properties.ConfigProperties;
import com.payline.pmapi.bean.common.FailureCause;
import com.payline.pmapi.logger.LogManager;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.logging.log4j.Logger;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Client dedicated to OAuth 2.0 authorization process : the recovering of a valid access token.
 * The class handles the storage and renewal of this token, and must be extended to use it.
 *
 * It also provides a generic method to execute HTTP requests, with a retry system.
 */
abstract class OAuthHttpClient {

    private static final Logger LOGGER = LogManager.getLogger(OAuthHttpClient.class);

    protected ConfigProperties config = ConfigProperties.getInstance();
    protected JsonService jsonService = JsonService.getInstance();

    /**
     * Support for the current authorization information.
     */
    protected Authorization authorization;

    /**
     * Client used to contact APIs through HTTP.
     */
    protected CloseableHttpClient client;

    /**
     * Has this class been initialized with partner configuration ?
     */
    protected AtomicBoolean initialized = new AtomicBoolean();

    /**
     * The number of time the client must retry to send the request if it doesn't obtain a response.
     */
    protected int retries;

    /**
     * Full URL (API domain + path) of the endpoint that delivers OAuth access token.
     * Since the domain and path change from one implementation to the other,
     * its value must be passed through the init method.
     */
    protected String tokenEndpointUrl;

    /**
     * Initialize the instance.
     *
     * @param tokenEndpointUrl the full URL of the endpoint that delivers access tokens
     */
    protected void init( String tokenEndpointUrl){
        if( this.initialized.compareAndSet(false, true) ){
            // Set the token endpoint URL
            this.tokenEndpointUrl = tokenEndpointUrl;

            // Retrieve config properties
            int connectionRequestTimeout;
            int connectTimeout;
            int socketTimeout;
            try {
                // request config timeouts (in seconds)
                connectionRequestTimeout = Integer.parseInt(config.get("http.connectionRequestTimeout"));
                connectTimeout = Integer.parseInt(config.get("http.connectTimeout"));
                socketTimeout = Integer.parseInt(config.get("http.socketTimeout"));

                // number of retry attempts
                this.retries = Integer.parseInt(config.get("http.retries"));
            }
            catch( NumberFormatException e ){
                throw new PluginException("plugin error: http.* properties must be integers", e);
            }

            // Create RequestConfig
            RequestConfig requestConfig = RequestConfig.custom()
                    .setConnectionRequestTimeout(connectionRequestTimeout * 1000)
                    .setConnectTimeout(connectTimeout * 1000)
                    .setSocketTimeout(socketTimeout * 1000)
                    .build();

            // Instantiate Apache HTTP client
            this.client = HttpClientBuilder.create()
                    .useSystemProperties()
                    .setDefaultRequestConfig( requestConfig )
                    .setSSLSocketFactory(new SSLConnectionSocketFactory(HttpsURLConnection.getDefaultSSLSocketFactory(), SSLConnectionSocketFactory.getDefaultHostnameVerifier()))
                    .build();
        }
    }

    /**
     * Return an access token in an {@link Authorization} object (contains the token type too).
     *
     * @return A valid authorization
     */
    public Authorization authorize( RequestConfiguration requestConfiguration ){
        if( !this.initialized.get() ){
            throw new PluginException("Illegal state: client must be initialized");
        }
        if( this.isAuthorized() ){
            LOGGER.info("Client already contains a valid authorization");
            return this.authorization;
        }

        // Headers: convert the map of authorization headers to a list of Header
        List<Header> headers = new ArrayList<>();
        for (Map.Entry<String, String> h : this.authorizationHeaders( this.tokenEndpointUrl, requestConfiguration ).entrySet()) {
            headers.add( new BasicHeader(h.getKey(), h.getValue()) );
        }
        // Add Content-Type header
        headers.add( new BasicHeader(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded") );

        // www-form-urlencoded content
        StringEntity body = new StringEntity("grant_type=client_credentials", StandardCharsets.UTF_8);

        // Execute request
        StringResponse response = this.post( this.tokenEndpointUrl, headers, body );

        // Handle potential error
        if( !response.isSuccess() ){
            throw this.handleAuthorizationErrorResponse( response );
        }

        // Build authorization from response content
        Authorization.AuthorizationBuilder authBuilder = new Authorization.AuthorizationBuilder();
        try {
            // parse response content
            RFC6749AccessTokenSuccessResponse authResponse = jsonService.fromJson(response.getContent(), RFC6749AccessTokenSuccessResponse.class);

            // retrieve access token & token type
            if( authResponse.getAccessToken() == null ){
                throw new PluginException("No access_token in the authorization response", FailureCause.COMMUNICATION_ERROR);
            }
            authBuilder.withAccessToken( authResponse.getAccessToken() )
                    .withTokenType( authResponse.getTokenType() == null ? "Bearer" : authResponse.getTokenType() );

            // expiration date
            long expiresIn = 60L * 5 * 1000; // 5 minutes default expiration time
            if( authResponse.getExpiresIn() != null ){
                expiresIn = 1000L * authResponse.getExpiresIn();
            }
            Date expiresAt = new Date( System.currentTimeMillis() + expiresIn );
            authBuilder.withExpiresAt( expiresAt );

            this.authorization = authBuilder.build();
            return this.authorization;
        }
        catch(JsonSyntaxException | IllegalStateException e){
            throw new PluginException("Failed to parse authorization response", FailureCause.COMMUNICATION_ERROR, e);
        }
    }

    /**
     * Build the request headers required to obtain an access token, which can vary from one Instant Payment to another.
     * This method should be overridden by children classes, specific to each Instant Payment method.
     *
     * @param uri The request URI
     * @param requestConfiguration The request configuration
     * @return the authorization request headers
     */
    protected abstract Map<String, String> authorizationHeaders(String uri, RequestConfiguration requestConfiguration );

    /**
     * Check if there is a current valid authorization.
     * @return `true` if the current authorization is valid, `false` otherwise.
     */
    boolean isAuthorized(){
        return this.authorization != null
                && this.authorization.getExpiresAt().compareTo( new Date() ) > 0;
        /* Warning: the token can be valid at the time this method is called but not anymore 1 second later...
        Maybe add some time (5 minutes ?) to the current time, to be sure. */
    }

    /**
     * Handle error responses with RFC 6749 format.
     *
     * @param response The response received, converted as {@link StringResponse}.
     * @return The {@link PluginException} to throw
     */
    PluginException handleAuthorizationErrorResponse( StringResponse response ){
        RFC6749AccessTokenErrorResponse errorResponse;
        try {
            errorResponse = jsonService.fromJson(response.getContent(), RFC6749AccessTokenErrorResponse.class );
        }
        catch( JsonSyntaxException e ){
            errorResponse = null;
        }

        if( errorResponse != null && errorResponse.getError() != null ){
            if( errorResponse.getErrorDescription() != null ){
                LOGGER.error( "Authorization error: {}", errorResponse.getErrorDescription() );
            }
            return new PluginException("authorization error: " + errorResponse.getError(), FailureCause.INVALID_DATA);
        }
        else {
            return new PluginException("unknown authorization error", FailureCause.PARTNER_UNKNOWN_ERROR);
        }
    }

    /**
     * Send the request, with a retry system in case the client does not obtain a proper response from the server.
     *
     * @param httpRequest The request to send.
     * @return The response converted as a {@link StringResponse}.
     * @throws PluginException If an error repeatedly occurs and no proper response is obtained.
     */
    protected StringResponse execute( HttpRequestBase httpRequest ){
        StringResponse strResponse = null;
        int attempts = 1;

        while( strResponse == null && attempts <= this.retries ){
            if( LOGGER.isDebugEnabled() ){
                LOGGER.debug( "Start call to partner API (attempt {}) :{}", attempts, System.lineSeparator() + PluginUtils.requestToString( httpRequest ) );
            } else {
                LOGGER.info( "Start call to partner API [{} {}] (attempt {})", httpRequest.getMethod(), httpRequest.getURI(), attempts );
            }
            try( CloseableHttpResponse httpResponse = this.client.execute( httpRequest ) ){
                strResponse = StringResponse.fromHttpResponse( httpResponse );
            }
            catch (IOException e) {
                LOGGER.error("An error occurred during the HTTP call :", e);
                strResponse = null;
            }
            finally {
                attempts++;
            }
        }

        if( strResponse == null ){
            throw new PluginException( "Failed to contact the partner API", FailureCause.COMMUNICATION_ERROR );
        }

        if( LOGGER.isDebugEnabled() ){
            LOGGER.debug( "Response obtained from partner API :{}", System.lineSeparator() + strResponse.toString() );
        } else {
            LOGGER.info("Response obtained from partner API [{} {}]", strResponse.getStatusCode(), strResponse.getStatusMessage() );
        }

        return strResponse;
    }

    /**
     * Performs an HTTP request on the given url using GET method.
     *
     * @param url The target URL
     * @param headers The request headers
     * @return The response from the call
     */
    protected StringResponse get( String url, List<Header> headers ){
        // Instantiate URI from given url
        URI uri;
        try {
            uri = new URI( url );
        }
        catch (URISyntaxException e) {
            throw new InvalidDataException("Target URL is invalid : " + url, e);
        }

        // Create request
        HttpGet request = new HttpGet( uri );

        // Add headers
        Header[] headersArray = new Header[headers.size()];
        headers.toArray( headersArray );
        request.setHeaders( headersArray );

        // Execute request
        return this.execute( request );
    }

    /**
     * Performs an HTTP request on the given url using POST method.
     *
     * @param url The target url
     * @param headers The request headers
     * @param body The request body
     * @return The response from the call
     */
    protected StringResponse post(String url, List<Header> headers, HttpEntity body ){
        // Instantiate URI from given url
        URI uri;
        try {
            uri = new URI( url );
        }
        catch (URISyntaxException e) {
            throw new InvalidDataException("Target URL is invalid : " + url, e);
        }

        // Create request
        HttpPost request = new HttpPost( uri );

        // Add headers
        Header[] headersArray = new Header[headers.size()];
        headers.toArray( headersArray );
        request.setHeaders( headersArray );

        // Add body
        request.setEntity( body );

        // Execute request
        return this.execute( request );
    }

}
