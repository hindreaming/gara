package com.eyegil.gara.catalog.util;

import org.springframework.util.StringUtils;

public final class VolumeNames {

    private VolumeNames() {
    }

    /**
     * Path of a file inside a repository, given the volume's directory ("" for the root).
     */
    public static String repoPath(String directory, String path) {
        return StringUtils.hasText(directory) ? directory + "/" + path : path;
    }

    /**
     * Name of the n-th volume of a bucket, counting the original repository as volume 1.
     */
    public static String numbered(String firstRepoName, int volumeNumber) {
        return volumeNumber == 1 ? firstRepoName : firstRepoName + "-" + volumeNumber;
    }
}
