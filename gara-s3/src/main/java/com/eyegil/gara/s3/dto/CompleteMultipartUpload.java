package com.eyegil.gara.s3.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Request body of CompleteMultipartUpload: the parts that make up the object, in order.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JacksonXmlRootElement(localName = "CompleteMultipartUpload")
public class CompleteMultipartUpload {

    @JacksonXmlProperty(localName = "Part")
    @JacksonXmlElementWrapper(useWrapping = false)
    private List<CompletedPart> parts = new ArrayList<>();

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CompletedPart {

        @JacksonXmlProperty(localName = "PartNumber")
        private int partNumber;

        @JacksonXmlProperty(localName = "ETag")
        private String etag;
    }
}
