package com.eyegil.gara.s3.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Response of ListObjects (V1) and ListObjectsV2. Fields that only one version uses stay null in the other.
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JacksonXmlRootElement(localName = "ListBucketResult")
@JsonPropertyOrder({"Name", "Prefix", "Marker", "ContinuationToken", "StartAfter", "Delimiter", "EncodingType", "KeyCount",
        "MaxKeys", "IsTruncated", "NextMarker", "NextContinuationToken", "Contents", "CommonPrefixes"})
public class ListBucketResult {

    @JacksonXmlProperty(localName = "Name")
    private String name;

    @JacksonXmlProperty(localName = "Prefix")
    private String prefix;

    @JacksonXmlProperty(localName = "Marker")
    private String marker;

    @JacksonXmlProperty(localName = "ContinuationToken")
    private String continuationToken;

    @JacksonXmlProperty(localName = "StartAfter")
    private String startAfter;

    @JacksonXmlProperty(localName = "Delimiter")
    private String delimiter;

    @JacksonXmlProperty(localName = "EncodingType")
    private String encodingType;

    @JacksonXmlProperty(localName = "KeyCount")
    private Integer keyCount;

    @JacksonXmlProperty(localName = "MaxKeys")
    private int maxKeys;

    @JacksonXmlProperty(localName = "IsTruncated")
    private boolean truncated;

    @JacksonXmlProperty(localName = "NextMarker")
    private String nextMarker;

    @JacksonXmlProperty(localName = "NextContinuationToken")
    private String nextContinuationToken;

    @JacksonXmlProperty(localName = "Contents")
    @JacksonXmlElementWrapper(useWrapping = false)
    private List<S3ObjectSummary> contents = new ArrayList<>();

    @JacksonXmlProperty(localName = "CommonPrefixes")
    @JacksonXmlElementWrapper(useWrapping = false)
    private List<S3CommonPrefix> commonPrefixes = new ArrayList<>();
}
