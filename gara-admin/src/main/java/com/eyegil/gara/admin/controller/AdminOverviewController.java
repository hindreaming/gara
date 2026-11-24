package com.eyegil.gara.admin.controller;

import com.eyegil.gara.admin.dto.OverviewResponse;
import com.eyegil.gara.admin.service.OverviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AdminOverviewController {

    private final OverviewService overviewService;

    @GetMapping(value = "/admin/api/overview", produces = MediaType.APPLICATION_JSON_VALUE)
    public OverviewResponse overview() {
        return overviewService.overview();
    }
}
