package com.app.config;

import java.io.File;
import java.nio.file.Files;
import java.util.UUID;

public class DeviceIdManager {

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

            String id = UUID.randomUUID().toString();
            file.getParentFile().mkdirs();
            Files.writeString(file.toPath(), id);

            return id;

        } catch (Exception e) {
            throw new RuntimeException("Cannot get deviceId", e);
        }
    }
}