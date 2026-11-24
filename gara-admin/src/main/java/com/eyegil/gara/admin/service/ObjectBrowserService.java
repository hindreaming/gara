package com.eyegil.gara.admin.service;

import com.eyegil.gara.admin.dto.BrowseResponse;
import com.eyegil.gara.admin.dto.ObjectEntry;
import com.eyegil.gara.s3.dto.ListBucketResult;
import com.eyegil.gara.s3.dto.ListObjectsQuery;
import com.eyegil.gara.s3.dto.S3CommonPrefix;
import com.eyegil.gara.s3.service.S3ListingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Folder-style browsing of a bucket: one ListObjectsV2 page with "/" as delimiter, read from the catalog only.
 */
@Service
@RequiredArgsConstructor
public class ObjectBrowserService {

    private static final String DELIMITER = "/";
    private static final int MAX_ENTRIES = 1000;

    private final S3ListingService s3ListingService;

    public BrowseResponse browse(String bucketId, String prefix) {
        ListBucketResult listing = s3ListingService.list(bucketId,
                new ListObjectsQuery(true, prefix, DELIMITER, MAX_ENTRIES, null, null, null, null));
        return new BrowseResponse(
                bucketId,
                prefix,
                listing.getCommonPrefixes().stream().map(S3CommonPrefix::prefix).toList(),
                listing.getContents().stream()
                        .map(summary -> new ObjectEntry(summary.key(), summary.key().substring(prefix.length()),
                                summary.size(), summary.lastModified()))
                        .toList(),
                listing.isTruncated()
        );
    }
}
