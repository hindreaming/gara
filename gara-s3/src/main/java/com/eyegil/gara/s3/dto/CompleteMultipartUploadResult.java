package com.eyegil.gara.s3.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * Response of CompleteMultipartUpload. {@code etag} is quoted, as S3 returns it.
 */
@JacksonXmlRootElement(localName = "CompleteMultipartUploadResult")
@JsonPropertyOrder({"Location", "Bucket", "Key", "ETag"})
public record CompleteMultipartUploadResult(
        @JacksonXmlProperty(localName = "Location") String location,
        @JacksonXmlProperty(localName = "Bucket") String bucket,
        @JacksonXmlProperty(localName = "Key") String key,
        @JacksonXmlProperty(localName = "ETag") String etag
) {
}
