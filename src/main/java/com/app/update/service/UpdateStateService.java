package com.app.update.service;

import org.springframework.stereotype.Service;

import java.util.prefs.Preferences;

@Service
public class UpdateStateService {

    private final Preferences prefs = Preferences.userNodeForPackage(UpdateStateService.class);

    public void saveSkippedVersion(String version) {
        prefs.put("skippedVersion", version);
    }

    public String getSkippedVersion() {
        return prefs.get("skippedVersion", null);
    }
}
