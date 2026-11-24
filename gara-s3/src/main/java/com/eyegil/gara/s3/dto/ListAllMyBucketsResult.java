package com.eyegil.gara.s3.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import java.util.List;

/**
 * Response of ListBuckets.
 */
@JacksonXmlRootElement(localName = "ListAllMyBucketsResult")
@JsonPropertyOrder({"Owner", "Buckets"})
public record ListAllMyBucketsResult(
        @JacksonXmlProperty(localName = "Owner") Owner owner,
        @JacksonXmlElementWrapper(localName = "Buckets")
        @JacksonXmlProperty(localName = "Bucket") List<BucketEntry> buckets
) {

    @JsonPropertyOrder({"ID", "DisplayName"})
    public record Owner(
            @JacksonXmlProperty(localName = "ID") String id,
            @JacksonXmlProperty(localName = "DisplayName") String displayName
    ) {
    }

    @JsonPropertyOrder({"Name", "CreationDate"})
    public record BucketEntry(
            @JacksonXmlProperty(localName = "Name") String name,
            @JacksonXmlProperty(localName = "CreationDate") String creationDate
    ) {
    }
}
