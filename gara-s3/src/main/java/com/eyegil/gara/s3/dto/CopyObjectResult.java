package com.eyegil.gara.s3.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * Response of CopyObject. {@code etag} is quoted, as S3 returns it.
 */
@JacksonXmlRootElement(localName = "CopyObjectResult")
@JsonPropertyOrder({"LastModified", "ETag"})
public record CopyObjectResult(
        @JacksonXmlProperty(localName = "LastModified") String lastModified,
        @JacksonXmlProperty(localName = "ETag") String etag
) {
}
