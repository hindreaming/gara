package com.eyegil.gara.sandbox.dto;

/**
 * @param fileUrl       public link to the file's content on GitHub
 * @param repositoryUrl the GitHub repository that holds the file
 * @param quota         the allowance left after this upload
 */
public record SandboxUpload(String name, long size, String fileUrl, String repositoryUrl, SandboxQuota quota) {
}
