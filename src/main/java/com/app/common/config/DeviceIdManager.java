package com.app.common.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.util.UUID;

public class DeviceIdManager {

    private static final Logger log = LoggerFactory.getLogger(DeviceIdManager.class);

    private final String filePath;

    public DeviceIdManager(String appDir) {
        this.filePath = appDir + "/config/device.id";
    }

    public String getDeviceId() {
        try {
            File file = new File(filePath);

            if (file.exists()) {
                return Files.readString(file.toPath()).trim();
            }

            File parentDir = file.getParentFile();
            if (!parentDir.exists() && !parentDir.mkdirs()) {
                throw new RuntimeException("Failed to create config directory: " + parentDir.getAbsolutePath());
            }

            String id = UUID.randomUUID().toString();
            Files.writeString(file.toPath(), id);
            log.info("Device ID generated: {}", id);

            return id;

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Cannot get deviceId", e);
        }
    }
}