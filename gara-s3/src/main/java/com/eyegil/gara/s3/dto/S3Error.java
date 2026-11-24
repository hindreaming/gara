package com.eyegil.gara.s3.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JacksonXmlRootElement(localName = "Error")
public record S3Error(
        @JacksonXmlProperty(localName = "Code") String code,
        @JacksonXmlProperty(localName = "Message") String message,
        @JacksonXmlProperty(localName = "Resource") String resource
) {
}
