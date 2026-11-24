package com.eyegil.gara.s3.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import java.util.List;

/**
 * Response of DeleteObjects.
 */
@JacksonXmlRootElement(localName = "DeleteResult")
@JsonPropertyOrder({"Deleted", "Error"})
public record DeleteResult(
        @JacksonXmlProperty(localName = "Deleted")
        @JacksonXmlElementWrapper(useWrapping = false) List<Deleted> deleted,
        @JacksonXmlProperty(localName = "Error")
        @JacksonXmlElementWrapper(useWrapping = false) List<Failed> errors
) {

    public record Deleted(@JacksonXmlProperty(localName = "Key") String key) {
    }

    @JsonPropertyOrder({"Key", "Code", "Message"})
    public record Failed(
            @JacksonXmlProperty(localName = "Key") String key,
            @JacksonXmlProperty(localName = "Code") String code,
            @JacksonXmlProperty(localName = "Message") String message
    ) {
    }
}
