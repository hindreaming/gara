package com.eyegil.gara;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;

/**
 * Each instance holds at most max-concurrent-uploads request bodies in memory at once.
 */
@TestPropertySource(properties = {
        "gara.s3.max-concurrent-uploads=1",
        "gara.s3.upload-wait=200ms"
})
class UploadLimitTest extends GaraIntegrationTest {

    private final CountDownLatch release = new CountDownLatch(1);

    @AfterEach
    void releaseBlockedCommit() {
        release.countDown();
    }

    @Test
    void asksClientsToSlowDownWhileAllUploadSlotsAreBusy() throws Exception {
        CountDownLatch committing = new CountDownLatch(1);
        doAnswer(invocation -> {
            committing.countDown();
            release.await(5, TimeUnit.SECONDS);
            return github.commit(invocation.getArgument(1), invocation.getArgument(2), invocation.getArgument(3));
        }).when(githubContents).createOrUpdateFile(anyString(), anyString(), anyString(), any());

        CompletableFuture<String> first = CompletableFuture.supplyAsync(() ->
                s3.putObject(b -> b.bucket(BUCKET).key("first.txt"), RequestBody.fromString("1")).eTag());
        assertThat(committing.await(5, TimeUnit.SECONDS)).isTrue();

        assertThatThrownBy(() -> s3.putObject(b -> b.bucket(BUCKET).key("second.txt"), RequestBody.fromString("2")))
                .isInstanceOfSatisfying(S3Exception.class,
                        e -> assertThat(e.awsErrorDetails().errorCode()).isEqualTo("SlowDown"));

        release.countDown();
        assertThat(first.get(5, TimeUnit.SECONDS)).isNotBlank();
    }
}
