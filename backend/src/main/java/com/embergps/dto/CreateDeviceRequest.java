package com.embergps.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** Request body for creating a new device. */
@Data
public class CreateDeviceRequest {

    @NotBlank(message = "device_id is required")
    private String deviceId;

    /** Optional human-readable label (e.g. "Truck 5"). */
    private String name;

    private String description;
}
