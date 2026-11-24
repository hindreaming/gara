package com.eyegil.gara.sandbox;

import com.eyegil.gara.GaraIntegrationTest;
import com.eyegil.gara.catalog.entity.Bucket;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

/**
 * The public sandbox page on top of the full Gara application.
 */
@TestPropertySource(properties = {
        "gara.sandbox.max-uploads-per-day=2",
        "gara.sandbox.max-bytes-per-day=1KB"
})
class SandboxTest extends GaraIntegrationTest {

    private final HttpClient http = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();

    @Test
    void createsAPublicSandboxBucketAtStartup() {
        verify(githubRepoProvisioner).create(eq("gara-sandbox"), anyString(), eq(true));
        assertThat(bucketService.find("gara-sandbox")).get().extracting(Bucket::isPublicRepos).isEqualTo(true);
    }

    @Test
    void servesThePageWithoutLogin() throws Exception {
        HttpResponse<String> redirect = get("/sandbox");
        assertThat(redirect.statusCode()).isEqualTo(302);
        assertThat(redirect.headers().firstValue("Location")).hasValue("/sandbox/index.html");

        HttpResponse<String> page = get("/sandbox/index.html");
        assertThat(page.statusCode()).isEqualTo(200);
        assertThat(page.body()).contains("Drag and drop");
        assertThat(get("/sandbox/sandbox.js").statusCode()).isEqualTo(200);
        assertThat(get("/sandbox/brand/favicon.ico").statusCode()).isEqualTo(200);
    }

    @Test
    void uploadReturnsPublicLinksAndStopsAtTheDailyUploadLimit() throws Exception {
        HttpResponse<String> first = upload("my photo.png", "a".repeat(100));
        assertThat(first.statusCode()).isEqualTo(201);
        String day = LocalDate.now(ZoneOffset.UTC).toString();
        assertThat(first.body())
                .contains("\"name\":\"my-photo.png\"")
                .contains("\"repositoryUrl\":\"https://github.com/test-owner/gara-sandbox\"")
                .containsPattern("https://raw\\.githubusercontent\\.com/test-owner/gara-sandbox/main/" + day + "/[0-9a-f]{8}-my-photo\\.png")
                .contains("\"uploadsUsed\":1");
        assertThat(github.paths()).anyMatch(path -> path.startsWith("gara-sandbox/" + day + "/") && path.endsWith("-my-photo.png"));

        assertThat(upload("second.txt", "b").statusCode()).isEqualTo(201);

        HttpResponse<String> third = upload("third.txt", "c");
        assertThat(third.statusCode()).isEqualTo(429);
        assertThat(third.body()).contains("2 uploads for today");
        assertThat(get("/sandbox/api/quota").body()).contains("\"uploadsUsed\":2", "\"bytesUsed\":101");
    }

    @Test
    void refusesFilesOverTheDailySpaceOrWithBlockedExtensions() throws Exception {
        assertThat(upload("big.bin", "x".repeat(1025)).statusCode()).isEqualTo(413);
        assertThat(upload("tool.exe", "x").statusCode()).isEqualTo(415);

        assertThat(upload("most.bin", "x".repeat(1000)).statusCode()).isEqualTo(201);
        HttpResponse<String> overflow = upload("rest.bin", "x".repeat(100));
        assertThat(overflow.statusCode()).isEqualTo(429);
        assertThat(overflow.body()).contains("does not fit");
    }

    private HttpResponse<String> upload(String name, String content) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(uri("/sandbox/api/files?name=" + name.replace(" ", "%20") + "&type=text/plain"))
                .header("Content-Type", "application/octet-stream")
                .POST(HttpRequest.BodyPublishers.ofString(content))
                .build();
        return http.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> get(String path) throws IOException, InterruptedException {
        return http.send(HttpRequest.newBuilder(uri(path)).GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String path) {
        return URI.create(baseUrl() + path);
    }
}
