package com.eyegil.gara.s3.service;

import com.eyegil.gara.catalog.entity.StoredObject;
import com.eyegil.gara.catalog.service.ObjectCatalogService;
import com.eyegil.gara.s3.converter.S3ObjectConverter;
import com.eyegil.gara.s3.dto.ListBucketResult;
import com.eyegil.gara.s3.dto.ListObjectsQuery;
import com.eyegil.gara.s3.dto.S3CommonPrefix;
import com.eyegil.gara.s3.dto.S3ObjectSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * ListObjects / ListObjectsV2 over the catalog, so every volume of a bucket is listed without calling GitHub.
 * Keys are read in pages, and each folder found under the delimiter is skipped with one jump, so a listing reads
 * at most about max-keys rows however many objects the bucket holds.
 * Continuation tokens and markers are the last returned key or common prefix. With {@code encoding-type=url}
 * (the AWS SDK default) keys and prefixes in the response are percent-encoded.
 */
@Service
@RequiredArgsConstructor
public class S3ListingService {

    private static final int MAX_KEYS_LIMIT = 1000;
    private static final int BATCH_SIZE = 1000;
    private static final char HIGHEST_CHAR = '￿';
    private static final String URL_ENCODING = "url";

    private final S3BucketService s3BucketService;
    private final ObjectCatalogService objectCatalogService;
    private final S3ObjectConverter s3ObjectConverter;

    public ListBucketResult list(String bucketId, ListObjectsQuery query) {
        s3BucketService.requireExists(bucketId);

        String prefix = orEmpty(query.prefix());
        String delimiter = StringUtils.hasLength(query.delimiter()) ? query.delimiter() : null;
        int maxKeys = Math.min(Math.max(query.maxKeys(), 0), MAX_KEYS_LIMIT);
        String startAfter = query.v2()
                ? (StringUtils.hasLength(query.continuationToken()) ? query.continuationToken() : orEmpty(query.startAfter()))
                : orEmpty(query.marker());
        // A continuation token that ends with the delimiter is a common prefix: resume after everything under it.
        boolean resumesAfterPrefix = delimiter != null && startAfter.endsWith(delimiter);
        String cursor = resumesAfterPrefix ? pastPrefix(startAfter) : startAfter;

        ListBucketResult result = newResult(bucketId, query, prefix, delimiter, maxKeys);
        Set<String> seenPrefixes = new HashSet<>();
        if (resumesAfterPrefix) {
            seenPrefixes.add(startAfter);
        }
        String lastEntry = null;
        int entries = 0;
        boolean truncated = false;

        fetching:
        while (true) {
            List<StoredObject> batch = objectCatalogService.list(bucketId, prefix, cursor, BATCH_SIZE);
            for (StoredObject object : batch) {
                String key = object.getObjectKey();
                cursor = key;
                String commonPrefix = commonPrefix(key, prefix, delimiter);
                if (commonPrefix != null && seenPrefixes.contains(commonPrefix)) {
                    continue;
                }
                if (entries == maxKeys) {
                    truncated = true;
                    break fetching;
                }
                entries++;
                if (commonPrefix != null) {
                    seenPrefixes.add(commonPrefix);
                    result.getCommonPrefixes().add(new S3CommonPrefix(commonPrefix));
                    lastEntry = commonPrefix;
                    cursor = pastPrefix(commonPrefix);
                    continue fetching;
                }
                result.getContents().add(s3ObjectConverter.toSummary(object));
                lastEntry = key;
            }
            if (batch.size() < BATCH_SIZE) {
                break;
            }
        }

        result.setTruncated(truncated);
        if (query.v2()) {
            result.setKeyCount(entries);
            result.setNextContinuationToken(truncated ? lastEntry : null);
        } else {
            result.setNextMarker(truncated ? lastEntry : null);
        }
        if (URL_ENCODING.equals(query.encodingType())) {
            urlEncode(result);
        }
        return result;
    }

    private void urlEncode(ListBucketResult result) {
        result.setEncodingType(URL_ENCODING);
        result.setPrefix(encode(result.getPrefix()));
        result.setDelimiter(encode(result.getDelimiter()));
        result.setStartAfter(encode(result.getStartAfter()));
        result.setMarker(encode(result.getMarker()));
        result.setNextMarker(encode(result.getNextMarker()));
        result.setContents(result.getContents().stream()
                .map(summary -> new S3ObjectSummary(encode(summary.key()), summary.lastModified(), summary.etag(),
                        summary.size(), summary.storageClass()))
                .toList());
        result.setCommonPrefixes(result.getCommonPrefixes().stream()
                .map(prefix -> new S3CommonPrefix(encode(prefix.prefix())))
                .toList());
    }

    private String encode(String value) {
        return value == null ? null : URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private ListBucketResult newResult(String bucketId, ListObjectsQuery query, String prefix, String delimiter, int maxKeys) {
        ListBucketResult result = new ListBucketResult();
        result.setName(bucketId);
        result.setPrefix(prefix);
        result.setDelimiter(delimiter);
        result.setMaxKeys(maxKeys);
        if (query.v2()) {
            result.setContinuationToken(query.continuationToken());
            result.setStartAfter(query.startAfter());
        } else {
            result.setMarker(orEmpty(query.marker()));
        }
        return result;
    }

    /**
     * A cursor that sorts after every key under {@code commonPrefix}, so the next query skips the whole folder.
     */
    private static String pastPrefix(String commonPrefix) {
        return commonPrefix + HIGHEST_CHAR;
    }

    private String commonPrefix(String key, String prefix, String delimiter) {
        if (delimiter == null) {
            return null;
        }
        int index = key.indexOf(delimiter, prefix.length());
        return index < 0 ? null : key.substring(0, index + delimiter.length());
    }

    private String orEmpty(String value) {
        return value == null ? "" : value;
    }
}
