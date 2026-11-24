package com.eyegil.gara.sandbox.controller;

import com.eyegil.gara.sandbox.config.SandboxConfig;
import com.eyegil.gara.sandbox.dto.SandboxQuota;
import com.eyegil.gara.sandbox.dto.SandboxUpload;
import com.eyegil.gara.sandbox.service.SandboxQuotaService;
import com.eyegil.gara.sandbox.service.SandboxUploadService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/**
 * Public, unauthenticated sandbox API. The file is the raw request body (one file per request), so a request over
 * the limits is refused from its Content-Length before any of it is read.
 */
@RestController
@RequiredArgsConstructor
public class SandboxController {

    private final SandboxQuotaService sandboxQuotaService;
    private final SandboxUploadService sandboxUploadService;

    @GetMapping({SandboxConfig.PATH, SandboxConfig.PATH + "/"})
    public ResponseEntity<Void> openPage() {
        return ResponseEntity.status(HttpStatus.FOUND).header(HttpHeaders.LOCATION, SandboxConfig.PAGE).build();
    }

    @GetMapping(SandboxConfig.PATH + "/api/quota")
    public SandboxQuota quota() {
        return sandboxQuotaService.quota();
    }

    @PostMapping(SandboxConfig.PATH + "/api/files")
    @ResponseStatus(HttpStatus.CREATED)
    public SandboxUpload upload(@RequestParam("name") String name,
                                @RequestParam(value = "type", required = false) String type,
                                HttpServletRequest request) throws IOException {
        return sandboxUploadService.upload(name, type, request.getContentLengthLong(), request.getInputStream());
    }
}
