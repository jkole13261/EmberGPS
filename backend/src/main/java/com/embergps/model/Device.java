package com.embergps.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * A registered Cradlepoint device allowed to post GPS data.
 */
@Entity
@Table(name = "devices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Unique identifier sent by the router (e.g. serial number). */
    @Column(name = "device_id", nullable = false, unique = true)
    private String deviceId;

    /** Human-readable label (e.g. "Truck 5"). */
    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description;

    /**
     * SHA-256 hex digest of the device's API key.
     * The plain key is shown only once at creation time and never stored.
     */
    @Column(name = "api_key_hash", nullable = false, length = 64)
    private String apiKeyHash;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    @UpdateTimestamp
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }
}
