-- V1: Create devices and GPS positions tables

-- Enable UUID generation
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Registered Cradlepoint devices
CREATE TABLE devices (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    device_id     VARCHAR(128) NOT NULL UNIQUE,
    name          VARCHAR(256),
    description   TEXT,
    api_key_hash  VARCHAR(64)  NOT NULL,  -- SHA-256 hex of the API key
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_devices_device_id ON devices (device_id);
CREATE INDEX idx_devices_api_key_hash ON devices (api_key_hash);

-- GPS position records
CREATE TABLE gps_positions (
    id             UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    device_id      VARCHAR(128) NOT NULL REFERENCES devices (device_id) ON DELETE CASCADE,
    captured_at    TIMESTAMPTZ  NOT NULL,   -- timestamp from the device/payload
    received_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    latitude       DOUBLE PRECISION NOT NULL,
    longitude      DOUBLE PRECISION NOT NULL,
    altitude       DOUBLE PRECISION,
    speed          DOUBLE PRECISION,        -- m/s
    heading        DOUBLE PRECISION,        -- degrees 0-360
    hdop           DOUBLE PRECISION,        -- horizontal dilution of precision
    num_satellites INTEGER,
    fix_type       INTEGER,                 -- 0=no fix, 2=2D, 3=3D
    CONSTRAINT uq_device_captured_at UNIQUE (device_id, captured_at)
);

CREATE INDEX idx_gps_device_id     ON gps_positions (device_id);
CREATE INDEX idx_gps_captured_at   ON gps_positions (captured_at DESC);
CREATE INDEX idx_gps_device_time   ON gps_positions (device_id, captured_at DESC);
