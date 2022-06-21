package com.payline.payment.equens.utils.http;

import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.message.BasicHeader;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class StringResponseTest {

    @Test
    void fromHttpResponse_nominal(){
        // given: a complete HTTP response
        CloseableHttpResponse httpResponse = HttpTestUtils.mockHttpResponse( 200, "OK", "some content",
                new Header[]{ new BasicHeader("Name", "Value")} );

        // when: converting it to StringResponse
        final StringResponse stringResponse = StringResponse.fromHttpResponse( httpResponse );

        // then: the StringResponse attributes match the content of the HttpResponse
        assertNotNull( stringResponse );
        assertEquals( 200, stringResponse.getStatusCode() );
        assertEquals( "OK", stringResponse.getStatusMessage() );
        assertEquals( "some content", stringResponse.getContent() );
        assertEquals( 1, stringResponse.getHeaders().size() );
        assertEquals( "Value", stringResponse.getHeader("name") );
    }

    @Test
    void fromHttpResponse_null(){
        // when: converting null to StringResponse
        StringResponse stringResponse = StringResponse.fromHttpResponse( null );

        // then: the StringResponse is null
        assertNull( stringResponse );
    }

    @Test
    void toStringMethod(){
        // given: a StringResponse instance
        String jsonContent = "{\"message\":\"This is an error message\"}";
        Map<String, String> headers = new HashMap<>();
        headers.put("Test", "This is a test header");
        StringResponse stringResponse = HttpTestUtils.mockStringResponse( 400, "Bad Request", jsonContent, headers);

        // when: calling toString method, then: the result is as expected
        String ln = System.lineSeparator();
        assertEquals( "HTTP 400 Bad Request" + ln
                + "Test: This is a test header" + ln
                + jsonContent
                , stringResponse.toString() );
    }

}
