package com.eyegil.gara.s3.service;

import com.eyegil.gara.catalog.entity.StoredObject;
import com.eyegil.gara.catalog.service.ObjectCatalogService;
import com.eyegil.gara.s3.converter.S3ObjectConverter;
import com.eyegil.gara.s3.dto.ListBucketResult;
import com.eyegil.gara.s3.dto.ListObjectsQuery;
import com.eyegil.gara.s3.dto.S3CommonPrefix;
import com.eyegil.gara.s3.dto.S3ObjectSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class S3ListingServiceTest {

    private static final String BUCKET = "assets";
    private static final List<String> KEYS = List.of(
            "a.txt", "docs/one.md", "docs/two.md", "img/x/1.png", "img/y.png", "z.txt");

    private final ObjectCatalogService objectCatalogService = mock(ObjectCatalogService.class);
    private final S3ListingService service = new S3ListingService(
            mock(S3BucketService.class), objectCatalogService, new S3ObjectConverter());

    @BeforeEach
    void catalogue() {
        when(objectCatalogService.list(eq(BUCKET), anyString(), anyString(), anyInt())).thenAnswer(invocation -> {
            String prefix = invocation.getArgument(1);
            String startAfter = invocation.getArgument(2);
            int limit = invocation.getArgument(3);
            return KEYS.stream()
                    .filter(key -> key.startsWith(prefix) && key.compareTo(startAfter) > 0)
                    .limit(limit)
                    .map(S3ListingServiceTest::location)
                    .toList();
        });
    }

    @Test
    void groupsKeysUnderTheDelimiterIntoCommonPrefixes() {
        ListBucketResult result = service.list(BUCKET, v2(null, "/", 1000, null));

        assertThat(keys(result)).containsExactly("a.txt", "z.txt");
        assertThat(prefixes(result)).containsExactly("docs/", "img/");
        assertThat(result.isTruncated()).isFalse();
        assertThat(result.getKeyCount()).isEqualTo(4);
    }

    @Test
    void listsOneLevelBelowAPrefix() {
        ListBucketResult result = service.list(BUCKET, v2("img/", "/", 1000, null));

        assertThat(keys(result)).containsExactly("img/y.png");
        assertThat(prefixes(result)).containsExactly("img/x/");
    }

    @Test
    void paginatesWithContinuationTokensWithoutRepeatingCommonPrefixes() {
        ListBucketResult first = service.list(BUCKET, v2(null, "/", 2, null));
        assertThat(first.isTruncated()).isTrue();
        assertThat(first.getNextContinuationToken()).isEqualTo("docs/");

        ListBucketResult second = service.list(BUCKET, v2(null, "/", 2, first.getNextContinuationToken()));
        assertThat(prefixes(second)).containsExactly("img/");
        assertThat(keys(second)).containsExactly("z.txt");
        assertThat(second.isTruncated()).isFalse();
        assertThat(second.getNextContinuationToken()).isNull();
    }

    @Test
    void v1ListingUsesMarkers() {
        ListBucketResult result = service.list(BUCKET,
                new ListObjectsQuery(false, null, null, 3, "a.txt", null, null, null));

        assertThat(keys(result)).containsExactly("docs/one.md", "docs/two.md", "img/x/1.png");
        assertThat(result.getMarker()).isEqualTo("a.txt");
        assertThat(result.getNextMarker()).isEqualTo("img/x/1.png");
        assertThat(result.getKeyCount()).isNull();
    }

    @Test
    void percentEncodesKeysWhenAskedForUrlEncoding() {
        ListBucketResult result = service.list(BUCKET,
                new ListObjectsQuery(true, "img/", "/", 1000, null, null, null, "url"));

        assertThat(result.getEncodingType()).isEqualTo("url");
        assertThat(result.getPrefix()).isEqualTo("img%2F");
        assertThat(keys(result)).containsExactly("img%2Fy.png");
        assertThat(prefixes(result)).containsExactly("img%2Fx%2F");
    }

    private static ListObjectsQuery v2(String prefix, String delimiter, int maxKeys, String token) {
        return new ListObjectsQuery(true, prefix, delimiter, maxKeys, null, token, null, null);
    }

    private static StoredObject location(String key) {
        StoredObject object = new StoredObject(BUCKET, key);
        object.setSize(1);
        object.setEtag("d41d8cd98f00b204e9800998ecf8427e");
        object.setUpdatedAt(Instant.EPOCH);
        return object;
    }

    private static List<String> keys(ListBucketResult result) {
        return result.getContents().stream().map(S3ObjectSummary::key).toList();
    }

    private static List<String> prefixes(ListBucketResult result) {
        return result.getCommonPrefixes().stream().map(S3CommonPrefix::prefix).toList();
    }
}
