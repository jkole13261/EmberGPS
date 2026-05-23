package com.embergps.service;

import com.embergps.dto.CreateDeviceRequest;
import com.embergps.dto.DeviceDto;
import com.embergps.exception.ConflictException;
import com.embergps.exception.DeviceNotFoundException;
import com.embergps.filter.ApiKeyAuthFilter;
import com.embergps.model.Device;
import com.embergps.repository.DeviceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeviceService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final DeviceRepository deviceRepository;

    /** Register a new device and return its plain API key (shown only once). */
    @Transactional
    public DeviceDto createDevice(CreateDeviceRequest req) {
        if (deviceRepository.existsByDeviceId(req.getDeviceId())) {
            throw new ConflictException("Device already registered: " + req.getDeviceId());
        }

        String plainKey = generateApiKey();
        String keyHash  = ApiKeyAuthFilter.sha256Hex(plainKey);

        Device device = Device.builder()
                .deviceId(req.getDeviceId())
                .name(req.getName())
                .description(req.getDescription())
                .apiKeyHash(keyHash)
                .active(true)
                .build();

        deviceRepository.save(device);
        log.info("Registered new device: {}", req.getDeviceId());

        return DeviceDto.from(device).toBuilder().apiKey(plainKey).build();
    }

    /** List all devices. */
    @Transactional(readOnly = true)
    public List<DeviceDto> listDevices() {
        return deviceRepository.findAll().stream()
                .map(DeviceDto::from)
                .toList();
    }

    /** Get a device by its device_id. */
    @Transactional(readOnly = true)
    public DeviceDto getDevice(String deviceId) {
        return deviceRepository.findByDeviceId(deviceId)
                .map(DeviceDto::from)
                .orElseThrow(() -> new DeviceNotFoundException(deviceId));
    }

    /** Deactivate a device so it can no longer post GPS data. */
    @Transactional
    public void deactivateDevice(String deviceId) {
        Device device = deviceRepository.findByDeviceId(deviceId)
                .orElseThrow(() -> new DeviceNotFoundException(deviceId));
        device.setActive(false);
        deviceRepository.save(device);
        log.info("Deactivated device: {}", deviceId);
    }

    /** Regenerate API key for an existing device. Returns new plain key. */
    @Transactional
    public DeviceDto regenerateKey(String deviceId) {
        Device device = deviceRepository.findByDeviceId(deviceId)
                .orElseThrow(() -> new DeviceNotFoundException(deviceId));

        String plainKey = generateApiKey();
        device.setApiKeyHash(ApiKeyAuthFilter.sha256Hex(plainKey));
        deviceRepository.save(device);
        log.info("Regenerated API key for device: {}", deviceId);

        return DeviceDto.from(device).toBuilder().apiKey(plainKey).build();
    }

    private static String generateApiKey() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return "emb_" + HexFormat.of().formatHex(bytes);
    }
}
