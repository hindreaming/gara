package com.eyegil.gara.sandbox.service;

import com.eyegil.gara.catalog.entity.Volume;
import com.eyegil.gara.catalog.util.VolumeNames;
import com.eyegil.gara.github.config.GithubProperty;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Public GitHub links for files in the (public) sandbox repositories.
 */
@Component
@RequiredArgsConstructor
public class SandboxLinks {

    private final GithubProperty githubProperty;

    public String repository(Volume volume) {
        return "https://github.com/" + githubProperty.owner() + "/" + volume.getRepoName();
    }

    public String file(Volume volume, String key) {
        String path = Arrays.stream(VolumeNames.repoPath(volume.getDirectory(), key).split("/"))
                .map(segment -> UriUtils.encodePathSegment(segment, StandardCharsets.UTF_8))
                .collect(Collectors.joining("/"));
        return "https://raw.githubusercontent.com/" + githubProperty.owner() + "/" + volume.getRepoName() + "/"
                + githubProperty.branch() + "/" + path;
    }
}
