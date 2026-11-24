package com.eyegil.gara.s3.util;

import com.eyegil.gara.s3.exception.S3ErrorCode;
import com.eyegil.gara.s3.exception.S3Exception;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Reads S3 XML request bodies regardless of the Content-Type the client sent (many send none), and writes XML
 * where Spring MVC's converters are not involved (servlet filters).
 * Kept private to this component: an {@code XmlMapper} bean would replace the application's JSON ObjectMapper.
 */
@Component
public class S3Xml {

    private final XmlMapper xmlMapper = XmlMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    public <T> T read(byte[] body, Class<T> type) {
        try {
            return xmlMapper.readValue(body, type);
        } catch (IOException e) {
            throw new S3Exception(S3ErrorCode.MALFORMED_XML);
        }
    }

    public byte[] write(Object value) {
        try {
            return xmlMapper.writeValueAsBytes(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize " + value.getClass().getSimpleName(), e);
        }
    }
}
