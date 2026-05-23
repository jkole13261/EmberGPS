package com.embergps.service;

import com.embergps.dto.GpsPositionDto;
import com.embergps.dto.PagedPositionResponse;
import com.embergps.exception.DeviceNotFoundException;
import com.embergps.repository.DeviceRepository;
import com.embergps.repository.GpsPositionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GpsQueryService {

    private final GpsPositionRepository positionRepository;
    private final DeviceRepository deviceRepository;

    /** Latest position for every registered device. */
    @Transactional(readOnly = true)
    public List<GpsPositionDto> getLatestAll() {
        return positionRepository.findLatestForAllDevices().stream()
                .map(GpsPositionDto::from)
                .toList();
    }

    /** Latest position for a specific device. */
    @Transactional(readOnly = true)
    public Optional<GpsPositionDto> getLatest(String deviceId) {
        if (!deviceRepository.existsByDeviceId(deviceId)) {
            throw new DeviceNotFoundException(deviceId);
        }
        return positionRepository.findTopByDeviceIdOrderByCapturedAtDesc(deviceId)
                .map(GpsPositionDto::from);
    }

    /**
     * Paginated GPS history for a device within an optional time range.
     *
     * @param deviceId device identifier
     * @param from     start of range (inclusive); defaults to epoch
     * @param to       end of range (inclusive); defaults to now
     * @param page     zero-based page index
     * @param size     page size (max 1000)
     */
    @Transactional(readOnly = true)
    public PagedPositionResponse getHistory(
            String deviceId, Instant from, Instant to, int page, int size) {

        if (!deviceRepository.existsByDeviceId(deviceId)) {
            throw new DeviceNotFoundException(deviceId);
        }

        Instant effectiveFrom = from != null ? from : Instant.EPOCH;
        Instant effectiveTo   = to   != null ? to   : Instant.now();
        int     effectiveSize = Math.min(size, 1000);

        Pageable pageable = PageRequest.of(page, effectiveSize);
        Page<GpsPositionDto> resultPage = positionRepository
                .findByDeviceIdAndCapturedAtBetweenOrderByCapturedAtDesc(
                        deviceId, effectiveFrom, effectiveTo, pageable)
                .map(GpsPositionDto::from);

        return PagedPositionResponse.builder()
                .positions(resultPage.getContent())
                .totalElements(resultPage.getTotalElements())
                .totalPages(resultPage.getTotalPages())
                .page(page)
                .size(effectiveSize)
                .build();
    }
}
