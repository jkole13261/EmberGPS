package com.embergps.controller;

import com.embergps.dto.GpsPositionDto;
import com.embergps.dto.PagedPositionResponse;
import com.embergps.service.GpsQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

/**
 * Query endpoints for GPS positions.
 *
 * <pre>
 * GET /api/v1/gps/latest                         — latest position for all devices
 * GET /api/v1/gps/latest/{deviceId}              — latest position for one device
 * GET /api/v1/gps/history/{deviceId}?from=&to=   — paginated history
 * </pre>
 *
 * All endpoints require either {@code X-API-Key} (device key) or {@code X-Admin-Key}.
 */
@RestController
@RequestMapping("/api/v1/gps")
@RequiredArgsConstructor
public class GpsQueryController {

    private final GpsQueryService queryService;

    /** Latest position for every registered device. */
    @GetMapping("/latest")
    public ResponseEntity<List<GpsPositionDto>> latestAll() {
        return ResponseEntity.ok(queryService.getLatestAll());
    }

    /** Latest position for a single device. */
    @GetMapping("/latest/{deviceId}")
    public ResponseEntity<GpsPositionDto> latest(@PathVariable String deviceId) {
        return queryService.getLatest(deviceId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    /**
     * Paginated history for a device.
     *
     * @param deviceId device identifier
     * @param from     ISO-8601 start time (optional, defaults to epoch)
     * @param to       ISO-8601 end time (optional, defaults to now)
     * @param page     zero-based page index (default 0)
     * @param size     page size (default 200, max 1000)
     */
    @GetMapping("/history/{deviceId}")
    public ResponseEntity<PagedPositionResponse> history(
            @PathVariable String deviceId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0")   int page,
            @RequestParam(defaultValue = "200") int size) {
        return ResponseEntity.ok(queryService.getHistory(deviceId, from, to, page, size));
    }
}
