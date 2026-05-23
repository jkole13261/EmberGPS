package com.embergps.controller;

import com.embergps.dto.CreateDeviceRequest;
import com.embergps.dto.DeviceDto;
import com.embergps.service.DeviceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin endpoints for managing registered Cradlepoint devices.
 * All endpoints require the {@code X-Admin-Key} header.
 *
 * <pre>
 * POST   /api/v1/admin/devices                       — register a device
 * GET    /api/v1/admin/devices                       — list all devices
 * GET    /api/v1/admin/devices/{deviceId}            — get device details
 * DELETE /api/v1/admin/devices/{deviceId}            — deactivate a device
 * POST   /api/v1/admin/devices/{deviceId}/regenerate-key — regenerate API key
 * </pre>
 */
@RestController
@RequestMapping("/api/v1/admin/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;

    @PostMapping
    public ResponseEntity<DeviceDto> createDevice(
            @Valid @RequestBody CreateDeviceRequest request) {
        DeviceDto created = deviceService.createDevice(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<List<DeviceDto>> listDevices() {
        return ResponseEntity.ok(deviceService.listDevices());
    }

    @GetMapping("/{deviceId}")
    public ResponseEntity<DeviceDto> getDevice(@PathVariable String deviceId) {
        return ResponseEntity.ok(deviceService.getDevice(deviceId));
    }

    @DeleteMapping("/{deviceId}")
    public ResponseEntity<Void> deactivateDevice(@PathVariable String deviceId) {
        deviceService.deactivateDevice(deviceId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{deviceId}/regenerate-key")
    public ResponseEntity<DeviceDto> regenerateKey(@PathVariable String deviceId) {
        return ResponseEntity.ok(deviceService.regenerateKey(deviceId));
    }
}
