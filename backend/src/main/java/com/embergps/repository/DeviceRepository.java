package com.embergps.repository;

import com.embergps.model.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeviceRepository extends JpaRepository<Device, UUID> {

    Optional<Device> findByDeviceId(String deviceId);

    Optional<Device> findByApiKeyHash(String apiKeyHash);

    boolean existsByDeviceId(String deviceId);
}
