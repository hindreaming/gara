package com.eyegil.gara.github.service;

import com.eyegil.gara.github.config.GithubProperty;
import com.eyegil.integration.github.client.config.error.GithubApiException;
import com.eyegil.integration.github.client.dto.CommitFileRequest;
import com.eyegil.integration.github.client.service.GithubContentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;
import java.util.function.Function;

/**
 * Commits single files through the contents API. Each commit is paced by {@link GithubWriteThrottle} and retried
 * when GitHub reports a conflict, a rate limit or an outage.
 * <p>
 * Overwriting or deleting a file needs its current blob sha. Callers pass the sha they stored at the last write;
 * if GitHub rejects it as stale (the file was changed outside Gara), the current sha is fetched and the commit retried.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GithubFileWriter {

    private static final int NOT_FOUND = 404;
    private static final Set<Integer> STALE_SHA = Set.of(409, 422);
    private static final Set<Integer> RETRYABLE = Set.of(409, 422, 429, 500, 502, 503, 504);

    private final GithubContentService githubContentService;
    private final GithubProperty githubProperty;
    private final GithubWriteThrottle githubWriteThrottle;

    /**
     * @param knownSha blob sha of the file now at {@code path}, or null for a new file
     * @return blob sha of the committed file
     */
    public String put(String repoName, String path, byte[] content, String knownSha) {
        return commit(repoName, path, knownSha, sha -> {
            CommitFileRequest request = CommitFileRequest.of("Put " + path, content).onBranch(githubProperty.branch());
            return githubContentService.createOrUpdateFile(githubProperty.owner(), repoName, path,
                    sha == null ? request : request.withSha(sha)).content().sha();
        });
    }

    /**
     * Removes the file from the branch head; a file that is already gone counts as deleted.
     *
     * @param knownSha blob sha of the file now at {@code path}
     */
    public void delete(String repoName, String path, String knownSha) {
        try {
            commit(repoName, path, knownSha, sha -> {
                if (sha != null) {
                    githubContentService.deleteFile(githubProperty.owner(), repoName, path, "Delete " + path, sha,
                            githubProperty.branch());
                }
                return null;
            });
        } catch (GithubApiException e) {
            if (status(e) != NOT_FOUND) {
                throw e;
            }
        }
    }

    private <T> T commit(String repoName, String path, String knownSha, Function<String, T> operation) {
        String sha = knownSha;
        for (int attempt = 1; ; attempt++) {
            githubWriteThrottle.acquire();
            try {
                return operation.apply(sha);
            } catch (GithubApiException e) {
                int status = status(e);
                if (attempt > githubProperty.writeRetries() || !RETRYABLE.contains(status)) {
                    throw e;
                }
                if (STALE_SHA.contains(status)) {
                    sha = currentSha(repoName, path);
                }
                log.warn("Commit of {} to {} failed with {} (attempt {}), retrying", path, repoName, status, attempt);
                pause(githubProperty.retryBackoff().multipliedBy(attempt), e);
            }
        }
    }

    private String currentSha(String repoName, String path) {
        try {
            return githubContentService.getContent(githubProperty.owner(), repoName, path, githubProperty.branch())
                    .path("sha").asText(null);
        } catch (GithubApiException e) {
            if (status(e) == NOT_FOUND) {
                return null;
            }
            throw e;
        }
    }

    private static int status(GithubApiException e) {
        return e.getStatusCode() == null ? -1 : e.getStatusCode().value();
    }

    private static void pause(Duration duration, GithubApiException failure) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw failure;
        }
    }
}
