package com.eyegil.gara;

import com.eyegil.integration.github.client.config.error.GithubApiException;
import com.eyegil.integration.github.client.dto.CommitFileRequest;
import com.eyegil.integration.github.client.dto.GithubFileResponse;
import com.eyegil.integration.github.client.dto.GithubFileResponse.GithubCommit;
import com.eyegil.integration.github.client.dto.GithubFileResponse.GithubFileContent;
import com.eyegil.integration.github.client.service.GithubContentService;
import com.eyegil.integration.github.client.service.GithubGitDataService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;

import java.net.URI;
import java.util.Base64;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

/**
 * In-memory stand-in for the contents and git blob APIs of GitHub, wired into mocked github-client services.
 * Files are keyed {@code repo/path}. Like GitHub, it rejects overwrites and deletes that carry a stale blob sha.
 */
public class FakeGithub {

    private final Map<String, StoredFile> files = new ConcurrentHashMap<>();
    private final Map<String, byte[]> blobs = new ConcurrentHashMap<>();
    private final AtomicInteger failingPuts = new AtomicInteger();

    public void install(GithubContentService contents, GithubGitDataService gitData) {
        when(gitData.getBlob(anyString(), anyString(), anyString()))
                .thenAnswer(invocation -> blob(invocation.getArgument(2)));
        when(contents.createOrUpdateFile(anyString(), anyString(), anyString(), any()))
                .thenAnswer(invocation -> commit(invocation.getArgument(1), invocation.getArgument(2), invocation.getArgument(3)));
        doAnswer(invocation -> {
            delete(invocation.getArgument(1), invocation.getArgument(2), invocation.getArgument(4));
            return null;
        }).when(contents).deleteFile(anyString(), anyString(), anyString(), anyString(), anyString(), anyString());
        when(contents.getContent(anyString(), anyString(), anyString(), anyString()))
                .thenAnswer(invocation -> read(invocation.getArgument(1), invocation.getArgument(2)));
    }

    /**
     * Commits a file the way the contents API does; usable from tests that wrap the default behavior.
     */
    public GithubFileResponse commit(String repo, String path, CommitFileRequest request) {
        if (failingPuts.getAndUpdate(n -> Math.max(0, n - 1)) > 0) {
            throw error(409);
        }
        StoredFile existing = files.get(repo + "/" + path);
        if (existing != null ? !existing.sha().equals(request.sha()) : request.sha() != null) {
            throw error(existing != null ? 409 : 422);
        }
        String sha = UUID.randomUUID().toString().replace("-", "").substring(0, 32);
        byte[] content = Base64.getDecoder().decode(request.content());
        files.put(repo + "/" + path, new StoredFile(content, sha));
        blobs.put(sha, content);
        return new GithubFileResponse(new GithubFileContent(path, path, sha, null, null), new GithubCommit("commit", null));
    }

    /**
     * Makes the next {@code count} commits fail with a conflict, as a concurrent writer would cause.
     */
    public void failNextPuts(int count) {
        failingPuts.set(count);
    }

    /**
     * Changes a file behind Gara's back, so the sha Gara stored becomes stale.
     */
    public void editOutsideGara(String repoPath, byte[] content) {
        files.put(repoPath, new StoredFile(content, "external"));
    }

    public Set<String> paths() {
        return files.keySet();
    }

    public byte[] content(String repoPath) {
        return files.get(repoPath).content();
    }

    private void delete(String repo, String path, String sha) {
        StoredFile existing = files.get(repo + "/" + path);
        if (existing == null) {
            throw error(404);
        }
        if (!existing.sha().equals(sha)) {
            throw error(409);
        }
        files.remove(repo + "/" + path);
    }

    private JsonNode read(String repo, String path) {
        StoredFile file = files.get(repo + "/" + path);
        if (file == null) {
            throw error(404);
        }
        ObjectNode node = JsonNodeFactory.instance.objectNode();
        node.put("sha", file.sha());
        node.put("encoding", "base64");
        node.put("content", Base64.getEncoder().encodeToString(file.content()));
        return node;
    }

    private JsonNode blob(String sha) {
        byte[] content = blobs.get(sha);
        if (content == null) {
            throw error(404);
        }
        ObjectNode node = JsonNodeFactory.instance.objectNode();
        node.put("sha", sha);
        node.put("encoding", "base64");
        node.put("content", Base64.getEncoder().encodeToString(content));
        return node;
    }

    private static GithubApiException error(int status) {
        return new GithubApiException(URI.create("https://api.github.com"), HttpMethod.PUT, HttpStatusCode.valueOf(status), "fake " + status);
    }

    private record StoredFile(byte[] content, String sha) {
    }
}
