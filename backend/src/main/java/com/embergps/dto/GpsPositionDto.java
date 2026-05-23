package com.embergps.dto;

import com.embergps.model.GpsPosition;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

/** API response object for a single GPS position. */
@Data
@Builder
public class GpsPositionDto {

    private UUID id;
    private String deviceId;
    private Instant capturedAt;
    private Instant receivedAt;
    private Double latitude;
    private Double longitude;
    private Double altitude;
    private Double speed;
    private Double heading;
    private Double hdop;
    private Integer numSatellites;
    private Integer fixType;

    public static GpsPositionDto from(GpsPosition p) {
        return GpsPositionDto.builder()
                .id(p.getId())
                .deviceId(p.getDeviceId())
                .capturedAt(p.getCapturedAt())
                .receivedAt(p.getReceivedAt())
                .latitude(p.getLatitude())
                .longitude(p.getLongitude())
                .altitude(p.getAltitude())
                .speed(p.getSpeed())
                .heading(p.getHeading())
                .hdop(p.getHdop())
                .numSatellites(p.getNumSatellites())
                .fixType(p.getFixType())
                .build();
    }
}
