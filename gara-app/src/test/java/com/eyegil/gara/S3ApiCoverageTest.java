package com.eyegil.gara;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.DeleteObjectsResponse;
import software.amazon.awssdk.services.s3.model.DeletedObject;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * S3 calls common clients rely on beyond put/get/list/delete: Range GET, ListBuckets, DeleteObjects, CopyObject.
 */
class S3ApiCoverageTest extends GaraIntegrationTest {

    @Test
    void servesByteRanges() {
        put("digits.txt", "0123456789", "text/plain");

        String range = s3.getObjectAsBytes(b -> b.bucket(BUCKET).key("digits.txt").range("bytes=2-5"))
                .asString(StandardCharsets.UTF_8);

        assertThat(range).isEqualTo("2345");
    }

    @Test
    void listsConfiguredBuckets() {
        assertThat(s3.listBuckets().buckets()).extracting(Bucket::name).containsExactly(BUCKET);
    }

    @Test
    void deletesManyKeysInOneRequest() {
        put("batch/a.txt", "a", "text/plain");
        put("batch/b.txt", "b", "text/plain");
        put("keep.txt", "k", "text/plain");

        DeleteObjectsResponse response = s3.deleteObjects(b -> b.bucket(BUCKET).delete(d -> d.objects(
                ObjectIdentifier.builder().key("batch/a.txt").build(),
                ObjectIdentifier.builder().key("batch/b.txt").build(),
                ObjectIdentifier.builder().key("never-existed.txt").build())));

        assertThat(response.deleted()).extracting(DeletedObject::key)
                .containsExactly("batch/a.txt", "batch/b.txt", "never-existed.txt");
        assertThat(response.errors()).isEmpty();
        assertThat(s3.listObjectsV2(b -> b.bucket(BUCKET)).contents()).extracting(S3Object::key).containsExactly("keep.txt");
        assertThat(github.paths()).containsExactly("archive/assets/keep.txt");
    }

    @Test
    void copiesObjectsKeepingTheirContentType() {
        put("original/report.csv", "a,b\n1,2", "text/csv");

        String etag = s3.copyObject(b -> b.sourceBucket(BUCKET).sourceKey("original/report.csv")
                .destinationBucket(BUCKET).destinationKey("copies/report copy.csv")).copyObjectResult().eTag();

        HeadObjectResponse copy = s3.headObject(b -> b.bucket(BUCKET).key("copies/report copy.csv"));
        assertThat(copy.contentType()).isEqualTo("text/csv");
        assertThat(copy.eTag()).isEqualTo(etag);
        assertThat(s3.getObjectAsBytes(b -> b.bucket(BUCKET).key("copies/report copy.csv")).asString(StandardCharsets.UTF_8))
                .isEqualTo("a,b\n1,2");
    }

    private void put(String key, String content, String contentType) {
        s3.putObject(b -> b.bucket(BUCKET).key(key).contentType(contentType), RequestBody.fromString(content, StandardCharsets.UTF_8));
    }
}
