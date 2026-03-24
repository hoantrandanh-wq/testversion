package com.app.update.model;

public class UpdateInfo {
    private String latestVersion;
    private String downloadUrl;
    private boolean hasUpdate;

    public UpdateInfo(String latestVersion, String downloadUrl, boolean hasUpdate) {
        this.latestVersion = latestVersion;
        this.downloadUrl = downloadUrl;
        this.hasUpdate = hasUpdate;
    }

    public String getLatestVersion() { return latestVersion; }
    public String getDownloadUrl() { return downloadUrl; }
    public boolean isHasUpdate() { return hasUpdate; }
}