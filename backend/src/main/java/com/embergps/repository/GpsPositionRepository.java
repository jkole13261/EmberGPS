package com.embergps.repository;

import com.embergps.model.GpsPosition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GpsPositionRepository extends JpaRepository<GpsPosition, UUID> {

    /** Latest position for a specific device. */
    Optional<GpsPosition> findTopByDeviceIdOrderByCapturedAtDesc(String deviceId);

    /** Paged history for a device within a time range. */
    Page<GpsPosition> findByDeviceIdAndCapturedAtBetweenOrderByCapturedAtDesc(
            String deviceId, Instant from, Instant to, Pageable pageable);

    /** All active device IDs that have sent data. */
    @Query("SELECT DISTINCT g.deviceId FROM GpsPosition g")
    List<String> findDistinctDeviceIds();

    /** Latest position for every device (using a subquery). */
    @Query("""
            SELECT g FROM GpsPosition g
            WHERE g.capturedAt = (
                SELECT MAX(g2.capturedAt) FROM GpsPosition g2 WHERE g2.deviceId = g.deviceId
            )
            """)
    List<GpsPosition> findLatestForAllDevices();

    /** Delete records older than the given cutoff (for data retention). */
    @Modifying
    @Query("DELETE FROM GpsPosition g WHERE g.capturedAt < :cutoff")
    int deleteOlderThan(@Param("cutoff") Instant cutoff);

    /** Check if a duplicate already exists (idempotency). */
    boolean existsByDeviceIdAndCapturedAt(String deviceId, Instant capturedAt);
}
