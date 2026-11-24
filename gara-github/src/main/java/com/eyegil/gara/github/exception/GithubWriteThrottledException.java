package com.eyegil.gara.github.exception;

/**
 * The commit budget toward GitHub is used up for longer than the configured throttle wait.
 */
public class GithubWriteThrottledException extends RuntimeException {

    public GithubWriteThrottledException() {
        super("GitHub write budget exhausted; try again later");
    }
}
