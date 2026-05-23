package com.embergps.controller;

import com.embergps.dto.GpsIngestRequest;
import com.embergps.dto.GpsPositionDto;
import com.embergps.service.GpsIngestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Receives GPS position data posted by Cradlepoint R980 routers.
 *
 * <pre>
 * POST /api/v1/gps/ingest
 * Header: X-API-Key: emb_<device-key>
 * </pre>
 */
@RestController
@RequestMapping("/api/v1/gps")
@RequiredArgsConstructor
public class GpsIngestController {

    private final GpsIngestService ingestService;

    @PostMapping("/ingest")
    public ResponseEntity<GpsPositionDto> ingest(
            @Valid @RequestBody GpsIngestRequest request) {
        GpsPositionDto saved = ingestService.ingest(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }
}
