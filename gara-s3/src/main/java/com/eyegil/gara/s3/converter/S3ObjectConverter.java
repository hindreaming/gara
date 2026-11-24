package com.eyegil.gara.s3.converter;

import com.eyegil.gara.catalog.entity.StoredObject;
import com.eyegil.gara.s3.dto.S3ObjectMetadata;
import com.eyegil.gara.s3.dto.S3ObjectSummary;
import org.springframework.stereotype.Component;

import java.time.temporal.ChronoUnit;

@Component
public class S3ObjectConverter {

    private static final String STORAGE_CLASS = "STANDARD";

    public S3ObjectMetadata toMetadata(StoredObject object) {
        return new S3ObjectMetadata(
                object.getObjectKey(),
                object.getSize(),
                object.getEtag(),
                object.getContentType(),
                object.getUpdatedAt()
        );
    }

    public S3ObjectSummary toSummary(StoredObject object) {
        return new S3ObjectSummary(
                object.getObjectKey(),
                object.getUpdatedAt().truncatedTo(ChronoUnit.MILLIS).toString(),
                "\"" + object.getEtag() + "\"",
                object.getSize(),
                STORAGE_CLASS
        );
    }
}
