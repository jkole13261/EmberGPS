package com.embergps.service;

import com.embergps.dto.GpsIngestRequest;
import com.embergps.dto.GpsPositionDto;
import com.embergps.exception.DeviceNotFoundException;
import com.embergps.model.GpsPosition;
import com.embergps.repository.DeviceRepository;
import com.embergps.repository.GpsPositionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class GpsIngestService {

    private final GpsPositionRepository positionRepository;
    private final DeviceRepository deviceRepository;
    private final Clock clock;

    /**
     * Persist an incoming GPS position.
     * The method is idempotent: duplicate (device_id, captured_at) pairs are silently ignored.
     *
     * @return the persisted (or pre-existing) position DTO
     */
    @Transactional
    public GpsPositionDto ingest(GpsIngestRequest req) {
        // Ensure the device is registered and active
        deviceRepository.findByDeviceId(req.getDeviceId())
                .filter(d -> d.isActive())
                .orElseThrow(() -> new DeviceNotFoundException(req.getDeviceId()));

        Instant capturedAt = req.getTimestamp();

        // Idempotency: skip duplicate (device, timestamp) pairs
        if (positionRepository.existsByDeviceIdAndCapturedAt(req.getDeviceId(), capturedAt)) {
            log.debug("Duplicate GPS position ignored for device={} at={}", req.getDeviceId(), capturedAt);
            return positionRepository
                    .findTopByDeviceIdOrderByCapturedAtDesc(req.getDeviceId())
                    .map(GpsPositionDto::from)
                    .orElseThrow();
        }

        GpsPosition position = GpsPosition.builder()
                .deviceId(req.getDeviceId())
                .capturedAt(capturedAt)
                .receivedAt(Instant.now(clock))
                .latitude(req.getLatitude())
                .longitude(req.getLongitude())
                .altitude(req.getAltitude())
                .speed(req.getSpeed())
                .heading(req.getHeading())
                .fixType(req.getFixType())
                .hdop(req.getHdop())
                .numSatellites(req.getSatellites())
                .build();

        GpsPosition saved = positionRepository.save(position);
        log.info("GPS ingested: device={} lat={} lon={} at={}",
                req.getDeviceId(), req.getLatitude(), req.getLongitude(), capturedAt);

        return GpsPositionDto.from(saved);
    }
}
