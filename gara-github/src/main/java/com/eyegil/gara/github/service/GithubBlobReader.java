package com.eyegil.gara.github.service;

import com.eyegil.gara.github.config.GithubProperty;
import com.eyegil.integration.github.client.service.GithubGitDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Base64;

/**
 * Reads file bytes by git blob sha: one request per read, for any file up to GitHub's 100MB limit. Reading by the
 * sha Gara stored at its last write also means a file edited on GitHub outside Gara is served as Gara wrote it,
 * matching the size and ETag in the catalog.
 */
@Service
@RequiredArgsConstructor
public class GithubBlobReader {

    private final GithubGitDataService githubGitDataService;
    private final GithubProperty githubProperty;

    public byte[] read(String repoName, String blobSha) {
        String content = githubGitDataService.getBlob(githubProperty.owner(), repoName, blobSha).path("content").asText("");
        return Base64.getMimeDecoder().decode(content);
    }
}
