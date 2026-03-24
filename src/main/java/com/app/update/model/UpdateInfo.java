package com.app.update.model;

import lombok.Getter;

@Getter
public class UpdateInfo {
    private String latestVersion;
    private String downloadUrl;
    private boolean hasUpdate;

    public UpdateInfo(String latestVersion, String downloadUrl, boolean hasUpdate) {
        this.latestVersion = latestVersion;
        this.downloadUrl = downloadUrl;
        this.hasUpdate = hasUpdate;
    }

}