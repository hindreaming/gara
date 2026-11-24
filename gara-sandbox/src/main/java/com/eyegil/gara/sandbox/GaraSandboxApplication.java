package com.eyegil.gara.sandbox;

import com.eyegil.gara.GaraApplication;
import org.springframework.boot.SpringApplication;

/**
 * Runs Gara with the public sandbox page. Gara's component scan covers this module's package, so the sandbox's
 * beans join the application; Gara itself knows nothing about them.
 */
public final class GaraSandboxApplication {

    private GaraSandboxApplication() {
    }

    public static void main(String[] args) {
        SpringApplication.run(GaraApplication.class, args);
    }
}
