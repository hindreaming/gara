package com.eyegil.gara.s3.dto;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public record S3CommonPrefix(
        @JacksonXmlProperty(localName = "Prefix") String prefix
) {
}
