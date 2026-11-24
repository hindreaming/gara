package com.eyegil.gara;

import com.eyegil.gara.catalog.service.BucketService;
import com.eyegil.gara.github.service.GithubRepoProvisioner;
import com.eyegil.integration.github.client.service.GithubContentService;
import com.eyegil.integration.github.client.service.GithubGitDataService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.awscore.retry.AwsRetryStrategy;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;

import static org.mockito.Mockito.when;

/**
 * The real application on a random port with Postgres (Flyway-migrated) and GitHub replaced by {@link FakeGithub}.
 * Tests talk to it with the AWS SDK, so signing, aws-chunked uploads and XML parsing are those of a real client.
 * Each test starts with bucket "assets" on existing repository "archive", directory "assets". Every test gets a fresh context and
 * empty tables. Subclasses tune limits with {@code @TestPropertySource}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "gara.auth.username=" + GaraIntegrationTest.USERNAME,
        "gara.auth.password=" + GaraIntegrationTest.PASSWORD,
        "gara.github.owner=test-owner",
        "gara.github.verify-on-startup=false",
        "gara.volume.min-active=1",
        "gara.github.retry-backoff=10ms",
        "gara.lease.poll-interval=20ms"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public abstract class GaraIntegrationTest {

    protected static final String USERNAME = "test-user";
    protected static final String PASSWORD = "test-secret";
    protected static final String BUCKET = "assets";

    @LocalServerPort
    protected int port;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @MockitoBean
    protected GithubContentService githubContents;

    @MockitoBean
    protected GithubGitDataService githubGitData;

    @MockitoBean
    protected GithubRepoProvisioner githubRepoProvisioner;

    protected final FakeGithub github = new FakeGithub();

    protected S3Client s3;

    @DynamicPropertySource
    static void postgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", EmbeddedPostgresServer::jdbcUrl);
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "");
    }

    @Autowired
    protected BucketService bucketService;

    @BeforeEach
    void connect() {
        github.install(githubContents, githubGitData);
        when(githubRepoProvisioner.exists("archive")).thenReturn(true);
        bucketService.create(BUCKET, "archive", "assets", false, false);
        s3 = s3Client(USERNAME, PASSWORD);
    }

    @AfterEach
    void cleanUp() {
        s3.close();
        jdbcTemplate.execute("TRUNCATE gara_object, gara_multipart_upload, gara_upload_part, "
                + "gara_volume, gara_bucket, gara_lease, gara_rate_budget RESTART IDENTITY CASCADE");
    }

    /**
     * A path-style client without retries, so back-pressure answers reach the test unchanged.
     */
    protected S3Client s3Client(String accessKey, String secretKey) {
        return S3Client.builder()
                .endpointOverride(URI.create(baseUrl()))
                .region(Region.EU_CENTRAL_1)
                .forcePathStyle(true)
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
                .overrideConfiguration(config -> config.retryStrategy(AwsRetryStrategy.doNotRetry()))
                .build();
    }

    protected String baseUrl() {
        return "http://localhost:" + port;
    }
}
