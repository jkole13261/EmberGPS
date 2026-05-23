package com.embergps.service;

import com.embergps.repository.GpsPositionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Scheduled job that removes GPS positions older than the configured retention period.
 * Runs at 02:00 UTC every day.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DataRetentionService {

    private final GpsPositionRepository positionRepository;
    private final Clock clock;

    @Value("${app.data-retention-days:90}")
    private int retentionDays;

    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void purgeOldPositions() {
        if (retentionDays <= 0) {
            log.debug("Data retention disabled (retentionDays={})", retentionDays);
            return;
        }
        Instant cutoff = Instant.now(clock).minus(retentionDays, ChronoUnit.DAYS);
        int deleted = positionRepository.deleteOlderThan(cutoff);
        log.info("Data retention: deleted {} GPS positions older than {} days (before {})",
                deleted, retentionDays, cutoff);
    }
}
