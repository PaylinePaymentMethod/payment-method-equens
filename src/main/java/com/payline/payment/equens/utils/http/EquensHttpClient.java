package com.payline.payment.equens.utils.http;

import com.google.gson.JsonSyntaxException;
import com.payline.payment.equens.bean.business.EquensErrorResponse;
import com.payline.payment.equens.bean.configuration.RequestConfiguration;
import com.payline.payment.equens.exception.InvalidDataException;
import com.payline.payment.equens.exception.PluginException;
import com.payline.payment.equens.utils.constant.ContractConfigurationKeys;
import com.payline.payment.equens.utils.constant.PartnerConfigurationKeys;
import com.payline.payment.equens.utils.security.RSAHolder;
import com.payline.pmapi.bean.common.FailureCause;
import com.payline.pmapi.bean.configuration.PartnerConfiguration;
import com.payline.pmapi.bean.payment.ContractConfiguration;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.message.BasicHeader;
import org.tomitribe.auth.signatures.Signature;
import org.tomitribe.auth.signatures.Signer;

import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateEncodingException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Stream;

import static org.tomitribe.auth.signatures.Algorithm.RSA_SHA256;

/**
 * Generic HTTP client to contact Equens Worldline API.
 * It's based upon OAuthHttpClient as Equens API uses this authorization protocol.
 * This class must be extended because the construction of the headers for the authorization call needs an app name.
 * Each Equens API has its proper name and access tokens.
 */
abstract class EquensHttpClient extends OAuthHttpClient {

    public static final String HEADER_AUTH_APP = "App";
    public static final String HEADER_AUTH_CLIENT = "Client";
    public static final String HEADER_AUTH_DATE = "Date";
    public static final String HEADER_AUTH_ID = "Id";
    static final String HEADER_REQUEST_ID = "X-Request-ID";

    /**
     * Holder containing the keystore data (keys or certificates).
     */
    private RSAHolder rsaHolder;

    public void init(PartnerConfiguration partnerConfiguration) {
        try {
            // Build RSA holder from PartnerConfiguration
            if( partnerConfiguration.getProperty( PartnerConfigurationKeys.CLIENT_CERTIFICATE ) == null ){
                throw new InvalidDataException("Missing client certificate chain from partner configuration (sentitive properties)");
            }
            if( partnerConfiguration.getProperty( PartnerConfigurationKeys.CLIENT_PRIVATE_KEY ) == null ){
                throw new InvalidDataException("Missing client private key from partner configuration (sensitive properties)");
            }
            if( partnerConfiguration.getProperty( PartnerConfigurationKeys.API_URL_TOKEN ) == null ){
                throw new InvalidDataException("Missing API URL Token from partner configuration");
            }

            // Initialize RsaHolder instance
            this.rsaHolder = new RSAHolder.RSAHolderBuilder()
                    .parseChain( partnerConfiguration.getProperty(PartnerConfigurationKeys.CLIENT_CERTIFICATE) )
                    .parsePrivateKey( partnerConfiguration.getProperty(PartnerConfigurationKeys.CLIENT_PRIVATE_KEY) )
                    .build();

        } catch ( IOException | GeneralSecurityException e ){
            throw new PluginException( "A problem occurred initializing SSL context", FailureCause.INVALID_DATA, e );
        }

        // Build authorization endpoint
        String authorizationEndpoint = partnerConfiguration.getProperty(PartnerConfigurationKeys.API_URL_TOKEN);

        // Pass these elements to the parent method initializer
        super.init(authorizationEndpoint);
    }

    protected abstract String appName();

    @Override
    protected Map<String, String> authorizationHeaders(String uri, RequestConfiguration requestConfiguration ) {
        // Check required data
        ContractConfiguration contractConfiguration = requestConfiguration.getContractConfiguration();
        if( contractConfiguration.getProperty( ContractConfigurationKeys.CLIENT_NAME ) == null
                || contractConfiguration.getProperty( ContractConfigurationKeys.CLIENT_NAME ).getValue() == null ){
            throw new InvalidDataException("Missing client name in contract configuration");
        }
        if( contractConfiguration.getProperty( ContractConfigurationKeys.ONBOARDING_ID ) == null
                || contractConfiguration.getProperty( ContractConfigurationKeys.ONBOARDING_ID ).getValue() == null ){
            throw new InvalidDataException("Missing onboarding id in contract configuration");
        }

        // Build headers list
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put(HEADER_AUTH_APP, this.appName());
        headers.put(HEADER_AUTH_CLIENT, contractConfiguration.getProperty( ContractConfigurationKeys.CLIENT_NAME ).getValue());
        headers.put(HEADER_AUTH_DATE, new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.US).format(new Date()));
        headers.put(HEADER_AUTH_ID, contractConfiguration.getProperty( ContractConfigurationKeys.ONBOARDING_ID ).getValue());

        // Generate the request signature
        Signature signature = this.generateSignature( uri, headers );
        String finalSignature = signature.toString().replace(RSA_SHA256.getPortableName(), RSA_SHA256.getJmvName());

        // Insert the signature into the header Authorization
        headers.put(HttpHeaders.AUTHORIZATION, finalSignature);

        return headers;
    }

    /**
     * Generate a signature, using the private key and client certificate returned by the {@link RSAHolder} instance.
     *
     * @param uri the request URI
     * @param headers the request headers
     * @return The signature
     */
    Signature generateSignature( String uri, Map<String, String> headers ){
        // Retrieve private key
        Key pk;
        try {
            pk = this.rsaHolder.getPrivateKey();
        } catch (GeneralSecurityException e) {
            throw new PluginException("plugin error: unable to get client private key", e);
        }

        // Generate keyId for the signature, which is the SHA1 hash of the client certificate without colons
        String sha1;
        try {
            sha1 = sha1( this.rsaHolder.getClientCertificate().getEncoded() );
        }
        catch (KeyStoreException e) {
            throw new PluginException("plugin error: unable to get client certificate", e);
        }
        catch (CertificateEncodingException e) {
            throw new PluginException("plugin error: unable to encode client certificate", e);
        }
        if( sha1 == null ){
            throw new PluginException("plugin error: unable to apply sha1 on client certificate");
        }
        String keyId = sha1.replace(":", "");

        // @see https://github.com/tomitribe/http-signatures-java
        // Create a signer
        Signature signature = new Signature(keyId, RSA_SHA256, null, "app", "client", "id", "date");
        Signer signer = new Signer(pk, signature);

        // Sign the HTTP message
        try {
            signature = signer.sign("POST", uri, headers);
        }
        catch (IOException e) {
            throw new PluginException("unexpected plugin error: while signing the request");
        }

        return signature;
    }

    /**
     * Get the Payline <code>errorCode</code> from the HTTP status code and the API error response.
     *
     * @param httpStatusCode The HTTP status code
     * @param apiError The API error response
     * @return The corresponding <code>errorCode</code>.
     */
    String getErrorCode( final int httpStatusCode, final EquensErrorResponse apiError ){
        if( apiError == null ){
            return null;
        }
        // default value
        String errorCode = apiError.getMessage();

        // particular cases
        if( httpStatusCode == 400
                && apiError.getCode() != null
                && Stream.of("001", "002", "133").anyMatch( apiError.getCode()::contains )
                && apiError.getDetails() != null ){
            errorCode = apiError.getDetails();
        }

        return errorCode;
    }

    /**
     * Get the Payline <code>FailureCause</code> from the HTTP status code and the API error code.
     *
     * @param httpStatusCode The HTTP status code (ex: 400)
     * @param apiErrorCode The API error code (ex: "007")
     * @return The corresponding <code>FailureCause</code>
     */
    FailureCause getFailureCause( final int httpStatusCode, final String apiErrorCode ){
        FailureCause failureCause = FailureCause.PARTNER_UNKNOWN_ERROR;
        switch( httpStatusCode ){
            case 400:
                failureCause = FailureCause.INVALID_DATA;
                if( Stream.of("108", "109").anyMatch( apiErrorCode::contains ) ){
                    failureCause = FailureCause.COMMUNICATION_ERROR;
                }
                break;

            case 403:
                if( Stream.of("007", "017").anyMatch( apiErrorCode::contains ) ){
                    failureCause = FailureCause.COMMUNICATION_ERROR;
                }
                else if( "107".equals( apiErrorCode ) ){
                    failureCause = FailureCause.INVALID_DATA;
                }
                else if( "120".equals( apiErrorCode ) ){
                    failureCause = FailureCause.REFUSED;
                }
                break;

            case 404:
                failureCause = FailureCause.INVALID_DATA;
                break;

            case 405:
                if( "135".equals( apiErrorCode ) ) {
                    failureCause = FailureCause.REFUSED;
                }
                break;

            case 401:
            case 502:
            case 503:
            case 511:
                failureCause = FailureCause.COMMUNICATION_ERROR;
                break;

            default:
                failureCause = FailureCause.PARTNER_UNKNOWN_ERROR;
        }
        return failureCause;
    }

    /**
     * Retrieve the requested API path in the plugin configuration.
     * Throws an exception if it's not found.
     *
     * @param configProperty The configuration property in which the path should be stored
     * @return The path
     */
    protected String getPath( String configProperty ){
        if( this.config.get(configProperty) == null ){
            throw new InvalidDataException("Illegal state: missing config " + configProperty);
        }
        return this.config.get(configProperty);
    }

    /**
     * Handle error responses with the specified format (see swagger description files of the API)
     *
     * @param apiResponse The raw response received from the API, as a <code>StringResponse</code>.
     * @return The <code>PluginException</code> to throw
     */
    protected PluginException handleError( StringResponse apiResponse ){
        // Try to parse the error response with the specified format
        EquensErrorResponse errorResponse;
        try {
            errorResponse = jsonService.fromJson( apiResponse.getContent(), EquensErrorResponse.class);
        }
        catch( JsonSyntaxException e ){
            errorResponse = null;
        }

        // Default message built from the HTTP code and status
        String message = "partner error: " + apiResponse.getStatusCode() + " " + apiResponse.getStatusMessage();
        // Default FailureCause : partner unknown error
        FailureCause failureCause = FailureCause.PARTNER_UNKNOWN_ERROR;

        // If errorResponse or errorResponse.code is null, no need to go further
        if( errorResponse == null || errorResponse.getCode() == null ){
            return new PluginException(message, failureCause);
        }

        // Map partner error codes to Payline FailureCause and errorCode
        return new PluginException(
                this.getErrorCode( apiResponse.getStatusCode(), errorResponse ),
                this.getFailureCause( apiResponse.getStatusCode(), errorResponse.getCode() )
        );
    }

    /**
     * Initialize a list of headers with :
     * - an Authorization header containing the access token
     * - a X-Request-ID header containing a unique request identifier
     *
     * @param requestConfiguration The request configuration
     * @return A list of headers
     */
    protected List<Header> initHeaders(RequestConfiguration requestConfiguration ){
        List<Header> headers = new ArrayList<>();
        Authorization auth = this.authorize( requestConfiguration );
        headers.add( new BasicHeader( HttpHeaders.AUTHORIZATION, auth.getHeaderValue() ) );
        headers.add( new BasicHeader( HEADER_REQUEST_ID, UUID.randomUUID().toString() ) );
        return headers;
    }

    private static String sha1( byte[] block ){
        try {
            return toHexString(MessageDigest.getInstance("SHA1").digest( block ));
        }
        catch (NoSuchAlgorithmException e) {
            // Should not happen as "SHA1" is a verified valid algorithm
            return null;
        }
    }

    private static String toHexString(byte[] block) {
        StringBuilder sb = new StringBuilder();
        int len = block.length;
        for (int i = 0; i < len; i++) {
            byte2hex(block[i], sb);
            if (i < len - 1) {
                sb.append(":");
            }
        }
        return sb.toString();
    }

    private static void byte2hex(byte b, StringBuilder sb) {
        char[] hexChars = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
        int high = ((b & 0xf0) >> 4);
        int low = (b & 0x0f);
        sb.append(hexChars[high]);
        sb.append(hexChars[low]);
    }

}
