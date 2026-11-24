package com.eyegil.gara;

import com.eyegil.gara.catalog.entity.Volume;
import com.eyegil.gara.catalog.service.VolumeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@TestPropertySource(properties = {
        "gara.volume.max-size=100B",
        "gara.volume.fill-threshold=1.0"
})
class GaraApplicationTest extends GaraIntegrationTest {

    @Autowired
    private VolumeService volumeService;

    @Test
    void storesEachObjectAsOneFileAtItsKeyPath() {
        String etag = put("images/logo.png", "hello", "image/png");

        assertThat(etag).isEqualTo("\"5d41402abc4b2a76b9719d911017c592\"");
        assertThat(github.paths()).containsExactly("archive/assets/images/logo.png");

        HeadObjectResponse head = s3.headObject(b -> b.bucket(BUCKET).key("images/logo.png"));
        assertThat(head.contentLength()).isEqualTo(5);
        assertThat(head.contentType()).isEqualTo("image/png");
        assertThat(get("images/logo.png")).isEqualTo("hello");
    }

    @Test
    void listsWithDelimitersAndUrlEncodedKeys() {
        put("docs/a b+c.txt", "1", "text/plain");
        put("docs/nested/b.txt", "2", "text/plain");
        put("top.txt", "3", "text/plain");

        ListObjectsV2Response root = s3.listObjectsV2(b -> b.bucket(BUCKET).delimiter("/"));
        assertThat(root.contents()).extracting(S3Object::key).containsExactly("top.txt");
        assertThat(root.commonPrefixes()).extracting(p -> p.prefix()).containsExactly("docs/");

        ListObjectsV2Response docs = s3.listObjectsV2(b -> b.bucket(BUCKET).prefix("docs/").delimiter("/"));
        assertThat(docs.contents()).extracting(S3Object::key).containsExactly("docs/a b+c.txt");
        assertThat(docs.commonPrefixes()).extracting(p -> p.prefix()).containsExactly("docs/nested/");
    }

    @Test
    void replacingOverwritesTheFileAndKeepsCountingHistory() {
        put("note.txt", "first", "text/plain");
        put("note.txt", "second", "text/plain");

        assertThat(get("note.txt")).isEqualTo("second");
        assertThat(github.paths()).hasSize(1);
        Volume volume = volumeService.findActive(BUCKET).get(0);
        assertThat(volume.getWrittenBytes()).isEqualTo(11);
        assertThat(volume.getHeadFiles()).isEqualTo(1);
    }

    @Test
    void deletingRemovesTheFileFromTheHeadButNotFromTheWrittenSize() {
        put("gone.txt", "bytes", "text/plain");

        s3.deleteObject(b -> b.bucket(BUCKET).key("gone.txt"));

        assertThat(github.paths()).isEmpty();
        assertThatThrownBy(() -> get("gone.txt")).isInstanceOf(NoSuchKeyException.class);
        Volume volume = volumeService.findActive(BUCKET).get(0);
        assertThat(volume.getWrittenBytes()).isEqualTo(5);
        assertThat(volume.getHeadFiles()).isZero();
    }

    @Test
    void multipartUploadsAreJoinedIntoOneFile() {
        String uploadId = s3.createMultipartUpload(b -> b.bucket(BUCKET).key("big.txt").contentType("text/plain")).uploadId();
        List<CompletedPart> parts = List.of(
                uploadPart("big.txt", uploadId, 1, "aaa"),
                uploadPart("big.txt", uploadId, 2, "bbb"),
                uploadPart("big.txt", uploadId, 3, "c"));

        String etag = s3.completeMultipartUpload(b -> b.bucket(BUCKET).key("big.txt").uploadId(uploadId)
                .multipartUpload(m -> m.parts(parts))).eTag();

        assertThat(etag).matches("\"[0-9a-f]{32}-3\"");
        assertThat(github.paths()).containsExactly("archive/assets/big.txt");
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM gara_upload_part", Long.class)).isZero();
        assertThat(get("big.txt")).isEqualTo("aaabbbc");
        assertThat(s3.headObject(b -> b.bucket(BUCKET).key("big.txt")).contentLength()).isEqualTo(7);
    }

    @Test
    void abortingAMultipartUploadDropsItsParts() {
        String uploadId = s3.createMultipartUpload(b -> b.bucket(BUCKET).key("draft.bin")).uploadId();
        uploadPart("draft.bin", uploadId, 1, "part");

        s3.abortMultipartUpload(b -> b.bucket(BUCKET).key("draft.bin").uploadId(uploadId));

        assertThat(github.paths()).isEmpty();
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM gara_upload_part", Long.class)).isZero();
        assertThatThrownBy(() -> get("draft.bin")).isInstanceOf(NoSuchKeyException.class);
    }

    @Test
    void presignedUrlsServeObjectsWithoutCredentials() throws Exception {
        put("private/report.txt", "for your eyes", "text/plain");
        URI presigned;
        try (S3Presigner presigner = S3Presigner.builder()
                .endpointOverride(URI.create(baseUrl()))
                .region(Region.EU_CENTRAL_1)
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(USERNAME, PASSWORD)))
                .build()) {
            presigned = presigner.presignGetObject(r -> r.signatureDuration(Duration.ofMinutes(5))
                    .getObjectRequest(b -> b.bucket(BUCKET).key("private/report.txt"))).url().toURI();
        }

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(HttpRequest.newBuilder(presigned).GET().build(), HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("for your eyes");
    }

    @Test
    void rejectsWrongCredentials() {
        try (S3Client intruder = s3Client(USERNAME, "wrong-secret")) {
            assertThatThrownBy(() -> intruder.listObjectsV2(b -> b.bucket(BUCKET)))
                    .isInstanceOfSatisfying(S3Exception.class,
                            e -> assertThat(e.awsErrorDetails().errorCode()).isEqualTo("SignatureDoesNotMatch"));
        }
        try (S3Client stranger = s3Client("someone", PASSWORD)) {
            assertThatThrownBy(() -> stranger.listObjectsV2(b -> b.bucket(BUCKET)))
                    .isInstanceOfSatisfying(S3Exception.class,
                            e -> assertThat(e.awsErrorDetails().errorCode()).isEqualTo("InvalidAccessKeyId"));
        }
    }

    @Test
    void retriesAFailedCommit() {
        github.failNextPuts(1);

        put("retry.txt", "hello", "text/plain");

        verify(githubContents, times(2)).createOrUpdateFile(anyString(), anyString(), anyString(), any());
        assertThat(get("retry.txt")).isEqualTo("hello");
    }

    @Test
    void retiresAFullVolumeAndCreatesTheNextRepositoryWhenNoneIsLeft() {
        s3.putObject(b -> b.bucket(BUCKET).key("big.bin"), RequestBody.fromBytes(new byte[100]));

        assertThat(volumeService.findActive(BUCKET)).isEmpty();

        put("small.txt", "hello", "text/plain");
        verify(githubRepoProvisioner).create(eq("archive-2"), anyString(), anyBoolean());
        assertThat(volumeService.findActive(BUCKET)).extracting(Volume::getRepoName).containsExactly("archive-2");
        assertThat(github.paths()).containsExactlyInAnyOrder("archive/assets/big.bin", "archive-2/assets/small.txt");
        assertThat(s3.listObjectsV2(b -> b.bucket(BUCKET)).contents()).hasSize(2);
    }

    @Test
    void replacingAKeyInAFullVolumeOverwritesItThere() {
        s3.putObject(b -> b.bucket(BUCKET).key("big.bin"), RequestBody.fromBytes(new byte[100]));

        put("big.bin", "smaller now", "text/plain");

        assertThat(github.paths()).containsExactly("archive/assets/big.bin");
        assertThat(get("big.bin")).isEqualTo("smaller now");
    }

    @Test
    void recoversWhenAFileWasChangedOutsideGara() {
        put("shared.txt", "from gara", "text/plain");
        github.editOutsideGara("archive/assets/shared.txt", "edited on github".getBytes(StandardCharsets.UTF_8));

        put("shared.txt", "from gara again", "text/plain");
        assertThat(get("shared.txt")).isEqualTo("from gara again");

        github.editOutsideGara("archive/assets/shared.txt", "edited again".getBytes(StandardCharsets.UTF_8));
        s3.deleteObject(b -> b.bucket(BUCKET).key("shared.txt"));
        assertThat(github.paths()).isEmpty();
    }


    private String put(String key, String content, String contentType) {
        return s3.putObject(b -> b.bucket(BUCKET).key(key).contentType(contentType),
                RequestBody.fromString(content, StandardCharsets.UTF_8)).eTag();
    }

    private String get(String key) {
        return s3.getObjectAsBytes(b -> b.bucket(BUCKET).key(key)).asString(StandardCharsets.UTF_8);
    }

    private CompletedPart uploadPart(String key, String uploadId, int partNumber, String content) {
        String etag = s3.uploadPart(b -> b.bucket(BUCKET).key(key).uploadId(uploadId).partNumber(partNumber),
                RequestBody.fromString(content, StandardCharsets.UTF_8)).eTag();
        return CompletedPart.builder().partNumber(partNumber).eTag(etag).build();
    }
}
