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
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doAnswer;

/**
 * Concurrent uploads hold their volume for the whole commit. A new upload must get a fresh volume while the
 * pool is below max-active, and be told to slow down once every allowed volume is busy.
 */
@TestPropertySource(properties = {
        "gara.volume.max-active=2",
        "gara.volume.lease-timeout=300ms"
})
class VolumePoolConcurrencyTest extends GaraIntegrationTest {

    private final CountDownLatch releaseCommits = new CountDownLatch(1);

    @AfterEach
    void releaseBlockedCommits() {
        releaseCommits.countDown();
    }

    @Test
    void addsAVolumeWhenEveryActiveVolumeIsBusy() throws Exception {
        CountDownLatch commitsStarted = blockCommitsTo("archive");
        CompletableFuture<String> first = upload("first.txt");
        assertThat(commitsStarted.await(5, TimeUnit.SECONDS)).isTrue();

        upload("second.txt").get(5, TimeUnit.SECONDS);

        verify(githubRepoProvisioner).create(eq("archive-2"), anyString(), anyBoolean());
        verify(githubContents).createOrUpdateFile(anyString(), eq("archive-2"), anyString(), any());

        releaseCommits.countDown();
        assertThat(first.get(5, TimeUnit.SECONDS)).isNotBlank();
    }

    @Test
    void asksClientsToSlowDownOnceMaxActiveVolumesAreAllBusy() throws Exception {
        CountDownLatch commitsStarted = blockCommitsTo("archive", "archive-2");
        CompletableFuture<String> first = upload("first.txt");
        CompletableFuture<String> second = upload("second.txt");
        assertThat(commitsStarted.await(5, TimeUnit.SECONDS)).isTrue();

        assertThatThrownBy(() -> s3.putObject(b -> b.bucket(BUCKET).key("third.txt"), RequestBody.fromString("c")))
                .isInstanceOfSatisfying(S3Exception.class, e -> {
                    assertThat(e.statusCode()).isEqualTo(503);
                    assertThat(e.awsErrorDetails().errorCode()).isEqualTo("SlowDown");
                });

        releaseCommits.countDown();
        assertThat(first.get(5, TimeUnit.SECONDS)).isNotBlank();
        assertThat(second.get(5, TimeUnit.SECONDS)).isNotBlank();
        verify(githubRepoProvisioner, times(1)).create(anyString(), anyString(), anyBoolean());
    }

    /**
     * Makes commits to the given repositories block until the test ends; the returned latch opens once each of
     * them has a commit in progress.
     */
    private CountDownLatch blockCommitsTo(String... repoNames) {
        CountDownLatch started = new CountDownLatch(repoNames.length);
        doAnswer(invocation -> {
            String repo = invocation.getArgument(1);
            for (String blocked : repoNames) {
                if (blocked.equals(repo)) {
                    started.countDown();
                    releaseCommits.await(5, TimeUnit.SECONDS);
                }
            }
            return github.commit(repo, invocation.getArgument(2), invocation.getArgument(3));
        }).when(githubContents).createOrUpdateFile(anyString(), anyString(), anyString(), any());
        return started;
    }

    private CompletableFuture<String> upload(String key) {
        return CompletableFuture.supplyAsync(() ->
                s3.putObject(b -> b.bucket(BUCKET).key(key), RequestBody.fromString(key)).eTag());
    }
}
