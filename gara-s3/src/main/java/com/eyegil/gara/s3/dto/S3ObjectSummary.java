package com.eyegil.gara.s3.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"Key", "LastModified", "ETag", "Size", "StorageClass"})
public record S3ObjectSummary(
        @JacksonXmlProperty(localName = "Key") String key,
        @JacksonXmlProperty(localName = "LastModified") String lastModified,
        @JacksonXmlProperty(localName = "ETag") String etag,
        @JacksonXmlProperty(localName = "Size") long size,
        @JacksonXmlProperty(localName = "StorageClass") String storageClass
) {
}
