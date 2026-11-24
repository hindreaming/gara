package com.eyegil.gara.s3.web;

import com.eyegil.gara.s3.config.ReservedPath;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Every {@link ReservedPath} declared by the application's modules; no bucket may be named after one.
 */
@Component
public class ReservedPaths {

    private final List<String> paths;

    public ReservedPaths(List<ReservedPath> reservedPaths) {
        this.paths = reservedPaths.stream().map(ReservedPath::path).toList();
    }

    public boolean isReserved(String path) {
        return paths.stream().anyMatch(reserved -> path.equals(reserved) || path.startsWith(reserved + "/"));
    }
}
