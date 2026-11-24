package com.eyegil.gara;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;
import software.amazon.awssdk.core.sync.RequestBody;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The admin side over plain HTTP: Basic auth, the overview API, the object browser, the UI files and health.
 */
@TestPropertySource(properties = "gara.auth.lockout.max-failures=3")
class AdminApiTest extends GaraIntegrationTest {

    private final HttpClient http = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();

    @Test
    void adminRequiresBasicAuthButHealthIsPublic() throws Exception {
        HttpResponse<String> anonymous = send(HttpRequest.newBuilder(uri("/admin/api/overview")).GET(), null);
        assertThat(anonymous.statusCode()).isEqualTo(401);
        assertThat(anonymous.headers().firstValue("WWW-Authenticate")).hasValueSatisfying(v -> assertThat(v).startsWith("Basic"));

        assertThat(send(HttpRequest.newBuilder(uri("/actuator/health")).GET(), null).statusCode()).isEqualTo(200);
        assertThat(send(HttpRequest.newBuilder(uri("/actuator/metrics")).GET(), null).statusCode()).isEqualTo(401);
    }

    @Test
    void locksOutAClientAfterRepeatedWrongPasswords() throws Exception {
        String wrong = "Basic " + Base64.getEncoder().encodeToString((USERNAME + ":guess").getBytes(StandardCharsets.UTF_8));
        for (int attempt = 0; attempt < 3; attempt++) {
            assertThat(send(HttpRequest.newBuilder(uri("/admin/api/overview")).GET(), wrong).statusCode()).isEqualTo(401);
        }

        assertThat(send(HttpRequest.newBuilder(uri("/admin/api/overview")).GET(), basicAuth()).statusCode()).isEqualTo(429);
    }

    @Test
    void createsAndDeletesBucketsFromTheAdminApi() throws Exception {
        when(githubRepoProvisioner.exists("media-archive")).thenReturn(false);
        String create = "{\"name\":\"media\",\"repository\":\"media-archive\",\"directory\":\"/files/\",\"createRepository\":true}";

        HttpResponse<String> created = send(HttpRequest.newBuilder(uri("/admin/api/buckets"))
                .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(create)), basicAuth());

        assertThat(created.statusCode()).isEqualTo(201);
        verify(githubRepoProvisioner).create(eq("media-archive"), anyString(), anyBoolean());
        assertThat(s3.listBuckets().buckets()).extracting(b -> b.name()).containsExactly("assets", "media");
        s3.putObject(b -> b.bucket("media").key("song.mp3"), RequestBody.fromString("la"));
        assertThat(github.paths()).contains("media-archive/files/song.mp3");

        HttpResponse<String> notEmpty = send(HttpRequest.newBuilder(uri("/admin/api/buckets/media")).DELETE(), basicAuth());
        assertThat(notEmpty.statusCode()).isEqualTo(409);

        s3.deleteObject(b -> b.bucket("media").key("song.mp3"));
        assertThat(send(HttpRequest.newBuilder(uri("/admin/api/buckets/media")).DELETE(), basicAuth()).statusCode()).isEqualTo(204);
        assertThat(s3.listBuckets().buckets()).extracting(b -> b.name()).containsExactly("assets");
    }

    @Test
    void rejectsInvalidReservedAndDuplicateBucketNames() throws Exception {
        assertThat(createBucket("Bad_Name", "repo")).isEqualTo(400);
        assertThat(createBucket("admin", "repo")).isEqualTo(400);
        assertThat(createBucket("assets", "archive")).isEqualTo(409);
        assertThat(createBucket("photos", "missing-repo")).isEqualTo(404);
    }

    @Test
    void s3RequestsWithoutASignatureAreDenied() throws Exception {
        HttpResponse<String> response = send(HttpRequest.newBuilder(uri("/assets")).GET(), null);

        assertThat(response.statusCode()).isEqualTo(403);
        assertThat(response.body()).contains("<Code>AccessDenied</Code>");
    }

    @Test
    void overviewDescribesBucketsVolumesAndHealth() throws Exception {
        HttpResponse<String> response = send(HttpRequest.newBuilder(uri("/admin/api/overview")).GET(), basicAuth());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body())
                .contains("\"health\":\"UP\"")
                .contains("\"repoName\":\"archive\"")
                .contains("\"id\":\"assets\"")
                .contains("\"volumePool\":\"UP\"")
                .contains("PostgreSQL");
    }

    @Test
    void servesTheUi() throws Exception {
        HttpResponse<String> redirect = send(HttpRequest.newBuilder(uri("/admin")).GET(), basicAuth());
        assertThat(redirect.statusCode()).isEqualTo(302);

        HttpResponse<String> page = send(HttpRequest.newBuilder(uri("/admin/ui/index.html")).GET(), basicAuth());
        assertThat(page.statusCode()).isEqualTo(200);
        assertThat(page.body()).contains("Objects");
        assertThat(send(HttpRequest.newBuilder(uri("/admin/ui/js/views/browser.js")).GET(), basicAuth()).statusCode()).isEqualTo(200);
        assertThat(send(HttpRequest.newBuilder(uri("/admin/ui/brand/favicon.ico")).GET(), basicAuth()).statusCode()).isEqualTo(200);
    }

    @Test
    void objectBrowserUploadsBrowsesDownloadsAndDeletes() throws Exception {
        String boundary = UUID.randomUUID().toString();
        String body = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"prefix\"\r\n\r\nreports/2026/\r\n"
                + "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"summary.txt\"\r\n"
                + "Content-Type: text/plain\r\n\r\nquarterly numbers\r\n"
                + "--" + boundary + "--\r\n";
        HttpResponse<String> upload = send(HttpRequest.newBuilder(uri("/admin/api/buckets/assets/objects"))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofString(body)), basicAuth());
        assertThat(upload.statusCode()).isEqualTo(200);

        assertThat(send(HttpRequest.newBuilder(uri("/admin/api/buckets/assets/objects")).GET(), basicAuth()).body())
                .contains("\"folders\":[\"reports/\"]").contains("\"objects\":[]");
        assertThat(send(HttpRequest.newBuilder(uri("/admin/api/buckets/assets/objects?prefix=reports/2026/")).GET(), basicAuth()).body())
                .contains("\"name\":\"summary.txt\"").contains("\"size\":17");

        HttpResponse<String> download = send(HttpRequest.newBuilder(
                uri("/admin/api/buckets/assets/objects/content?key=reports/2026/summary.txt")).GET(), basicAuth());
        assertThat(download.body()).isEqualTo("quarterly numbers");
        assertThat(download.headers().firstValue("Content-Disposition")).hasValueSatisfying(v -> assertThat(v).contains("summary.txt"));

        HttpResponse<String> delete = send(HttpRequest.newBuilder(
                uri("/admin/api/buckets/assets/objects?key=reports/2026/summary.txt")).DELETE(), basicAuth());
        assertThat(delete.statusCode()).isEqualTo(204);
        assertThat(send(HttpRequest.newBuilder(uri("/admin/api/buckets/assets/objects/content?key=reports/2026/summary.txt")).GET(),
                basicAuth()).statusCode()).isEqualTo(404);
    }


    @Test
    void sandboxIsNotPartOfGara() throws Exception {
        // Only gara-sandbox adds the sandbox, so here /sandbox is an ordinary (unsigned, refused) S3 path
        assertThat(send(HttpRequest.newBuilder(uri("/sandbox/api/quota")).GET(), null).statusCode()).isEqualTo(403);
        assertThat(bucketService.exists("gara-sandbox")).isFalse();
    }

    private int createBucket(String name, String repository) throws Exception {
        String body = "{\"name\":\"" + name + "\",\"repository\":\"" + repository + "\",\"directory\":\"\",\"createRepository\":false}";
        return send(HttpRequest.newBuilder(uri("/admin/api/buckets"))
                .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body)), basicAuth()).statusCode();
    }

    private HttpResponse<String> send(HttpRequest.Builder request, String authorization) throws IOException, InterruptedException {
        if (authorization != null) {
            request.header("Authorization", authorization);
        }
        return http.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static String basicAuth() {
        return "Basic " + Base64.getEncoder().encodeToString((USERNAME + ":" + PASSWORD).getBytes(StandardCharsets.UTF_8));
    }

    private URI uri(String path) {
        return URI.create(baseUrl() + path);
    }
}
