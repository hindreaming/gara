package com.eyegil.gara.s3.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Request body of DeleteObjects. In quiet mode only failures are reported back.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JacksonXmlRootElement(localName = "Delete")
public class DeleteObjectsRequest {

    @JacksonXmlProperty(localName = "Quiet")
    private boolean quiet;

    @JacksonXmlProperty(localName = "Object")
    @JacksonXmlElementWrapper(useWrapping = false)
    private List<ObjectIdentifier> objects = new ArrayList<>();

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ObjectIdentifier {

        @JacksonXmlProperty(localName = "Key")
        private String key;
    }
}
