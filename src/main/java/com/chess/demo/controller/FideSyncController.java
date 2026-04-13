package com.chess.demo.controller;

import com.chess.demo.service.FideImportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal/fide")
public class FideSyncController {

    private final FideImportService fideImportService;

    public FideSyncController(FideImportService fideImportService) {
        this.fideImportService = fideImportService;
    }

    @PostMapping("/sync")
    public ResponseEntity<FideImportService.FideImportSummary> sync(@RequestParam String timeControl,
                                                                    @RequestParam(required = false) String source) {
        return ResponseEntity.ok(fideImportService.sync(timeControl, source));
    }
}
