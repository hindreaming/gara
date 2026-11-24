package com.eyegil.gara.sandbox.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.unit.DataSize;

import java.util.List;

/**
 * The public sandbox page. Its limits are global (shared by every visitor) and reset at midnight UTC.
 *
 * @param maxUploadsPerDay  files accepted per day, from everyone together
 * @param maxBytesPerDay    bytes accepted per day, from everyone together; also caps a single file
 * @param blockedExtensions file extensions refused outright (lowercase, without the dot)
 */
@ConfigurationProperties("gara.sandbox")
public record SandboxProperty(
        @DefaultValue("10") int maxUploadsPerDay,
        @DefaultValue("20MB") DataSize maxBytesPerDay,
        @DefaultValue({"exe", "msi", "dll", "scr", "com", "bat", "cmd", "ps1", "vbs", "sh", "jar", "apk"})
        List<String> blockedExtensions
) {
}
