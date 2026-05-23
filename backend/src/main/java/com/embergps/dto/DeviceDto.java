package com.embergps.dto;

import com.embergps.model.Device;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

/** API response object for a device. Never exposes the raw API key or its hash. */
@Data
@Builder(toBuilder = true)
public class DeviceDto {

    private UUID id;
    private String deviceId;
    private String name;
    private String description;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;

    /**
     * Present only in the create / key-regenerate response.
     * Null in all other responses — the key is shown exactly once.
     */
    private String apiKey;

    public static DeviceDto from(Device device) {
        return DeviceDto.builder()
                .id(device.getId())
                .deviceId(device.getDeviceId())
                .name(device.getName())
                .description(device.getDescription())
                .active(device.isActive())
                .createdAt(device.getCreatedAt())
                .updatedAt(device.getUpdatedAt())
                .build();
    }
}
