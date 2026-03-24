package com.app.update.service;

import com.app.update.model.UpdateInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;

@Service
public class UpdateChecker {

    private static final String CURRENT_VERSION = "1.0.0";

    @Autowired
    private UpdateService updateService;

    @Autowired
    private UpdateStateService stateService;

    public void checkForUpdateAsync(Consumer<UpdateInfo> callback) {

        new Thread(() -> {
            if (!updateService.isOnline()) return;

            UpdateInfo latest = updateService.getLastVersion();
            if (latest == null) return;

            if (updateService.compareVersion(CURRENT_VERSION, latest.getVersion()) < 0) {

                String skipped = stateService.getSkippedVersion();

                if (!latest.getVersion().equals(skipped)) {
                    callback.accept(latest);
                }
            }

        }).start();
    }
}