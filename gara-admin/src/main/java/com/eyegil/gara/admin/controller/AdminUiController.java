package com.eyegil.gara.admin.controller;

import com.eyegil.gara.admin.config.AdminConfig;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminUiController {

    @GetMapping({"/admin", "/admin/"})
    public ResponseEntity<Void> openUi() {
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, AdminConfig.UI_PATH + "index.html")
                .build();
    }
}
