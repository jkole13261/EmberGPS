package com.embergps.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * A single GPS position report received from a Cradlepoint device.
 */
@Entity
@Table(name = "gps_positions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GpsPosition {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** FK to the device that sent this position. */
    @Column(name = "device_id", nullable = false)
    private String deviceId;

    /** Timestamp reported by the device (from the GPS module). */
    @Column(name = "captured_at", nullable = false)
    private Instant capturedAt;

    /** Timestamp when the server received this record. */
    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(name = "latitude", nullable = false)
    private Double latitude;

    @Column(name = "longitude", nullable = false)
    private Double longitude;

    /** Altitude in metres (may be null if not provided). */
    @Column(name = "altitude")
    private Double altitude;

    /** Speed in m/s (may be null if not provided). */
    @Column(name = "speed")
    private Double speed;

    /** Heading in degrees 0-360 (may be null if not provided). */
    @Column(name = "heading")
    private Double heading;

    /** Horizontal dilution of precision (may be null). */
    @Column(name = "hdop")
    private Double hdop;

    @Column(name = "num_satellites")
    private Integer numSatellites;

    /** 0 = no fix, 2 = 2D fix, 3 = 3D fix */
    @Column(name = "fix_type")
    private Integer fixType;
}
