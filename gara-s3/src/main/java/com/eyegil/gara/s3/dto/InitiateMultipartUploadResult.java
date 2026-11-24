package com.eyegil.gara.s3.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@JacksonXmlRootElement(localName = "InitiateMultipartUploadResult")
@JsonPropertyOrder({"Bucket", "Key", "UploadId"})
public record InitiateMultipartUploadResult(
        @JacksonXmlProperty(localName = "Bucket") String bucket,
        @JacksonXmlProperty(localName = "Key") String key,
        @JacksonXmlProperty(localName = "UploadId") String uploadId
) {
}
