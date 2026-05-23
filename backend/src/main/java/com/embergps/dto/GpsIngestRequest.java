package com.embergps.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.Instant;

/**
 * GPS payload sent by a Cradlepoint R980 via its NCOS SDK script.
 *
 * <p>Example JSON:
 * <pre>{@code
 * {
 *   "device_id":  "CP12345678",
 *   "timestamp":  "2024-01-15T10:30:00Z",
 *   "latitude":   37.774929,
 *   "longitude":  -122.419415,
 *   "altitude":   52.1,
 *   "speed":      12.5,
 *   "heading":    180.0,
 *   "fix_type":   3,
 *   "hdop":       1.2,
 *   "satellites": 8
 * }
 * }</pre>
 */
@Data
public class GpsIngestRequest {

    @NotBlank(message = "device_id is required")
    @JsonProperty("device_id")
    private String deviceId;

    /** ISO-8601 UTC timestamp from the GPS module. */
    @NotNull(message = "timestamp is required")
    private Instant timestamp;

    @NotNull(message = "latitude is required")
    @DecimalMin(value = "-90.0",  message = "latitude must be >= -90")
    @DecimalMax(value = "90.0",   message = "latitude must be <= 90")
    private Double latitude;

    @NotNull(message = "longitude is required")
    @DecimalMin(value = "-180.0", message = "longitude must be >= -180")
    @DecimalMax(value = "180.0",  message = "longitude must be <= 180")
    private Double longitude;

    /** Altitude in metres above sea level. */
    private Double altitude;

    /** Speed in m/s. */
    private Double speed;

    /** Track heading in degrees (0–360). */
    private Double heading;

    /** 0 = no fix, 2 = 2-D fix, 3 = 3-D fix. */
    @JsonProperty("fix_type")
    @JsonAlias("fixType")
    private Integer fixType;

    /** Horizontal dilution of precision. */
    private Double hdop;

    /** Number of satellites used. */
    @JsonAlias("num_sat")
    private Integer satellites;
}
