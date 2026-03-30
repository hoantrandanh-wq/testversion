package com.app.service;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class FolderSecurityService {

    private static final String LOCKED_FOLDER_NAME =
            "System Data.{21EC2020-3AEA-1069-A2DD-08002B30309D}";

    private final Path parentPath;
    private final String unlockedFolderName;
    private final Path unlockedPath;
    private final Path lockedPath;

    public FolderSecurityService(String folderPath) {
        this.unlockedPath = Path.of(folderPath);
        Path parent = unlockedPath.getParent();
        if (parent == null) {
            throw new IllegalArgumentException("folderPath must have parent: " + folderPath);
        }
        this.parentPath = parent;
        this.unlockedFolderName = unlockedPath.getFileName().toString();
        this.lockedPath = parentPath.resolve(LOCKED_FOLDER_NAME);
    }

    // 🔥 đảm bảo folder tồn tại
    public void ensureExists() {
        File normalFolder = unlockedPath.toFile();
        if (normalFolder.exists() || lockedPath.toFile().exists()) {
            return;
        }

        try {
            Files.createDirectories(unlockedPath);
            System.out.println("[SECURITY] Create folder: true");
        } catch (Exception e) {
            System.out.println("[SECURITY] Create folder: false - " + e.getMessage());
        }
    }

    // =========================
    // 🔒 LOCK (FULL)
    // =========================
    public void lock() {
        File locked = lockedPath.toFile();
        if (locked.exists()) {
            System.out.println("[SECURITY] Already locked");
            return;
        }

        File folder = unlockedPath.toFile();

        if (!folder.exists()) {
            System.out.println("[SECURITY] Nothing to lock");
            return;
        }

        try {
            int renameCode = runCommand(new String[]{
                    "cmd", "/c", "ren", unlockedFolderName, LOCKED_FOLDER_NAME
            });
            if (renameCode != 0 || !lockedPath.toFile().exists()) {
                System.out.println("[SECURITY] LOCK FAILED (rename)");
                return;
            }

            runCommand(new String[]{"cmd", "/c", "attrib", "+h", "+s", lockedPath.toString()});
            applyDeleteProtection(lockedPath.toString());
            System.out.println("[SECURITY] LOCKED");
        } catch (Exception e) {
            System.out.println("[SECURITY] LOCK FAILED: " + e.getMessage());
        }
    }

    // =========================
    // 🔓 UNLOCK (FULL)
    // =========================
    public void unlock() {
        File locked = lockedPath.toFile();

        if (!locked.exists()) {
            System.out.println("[SECURITY] Already unlocked");
            return;
        }

        try {
            runCommand(new String[]{"cmd", "/c", "attrib", "-h", "-s", lockedPath.toString()});
            removeDeleteProtection(lockedPath.toString());

            int renameCode = runCommand(new String[]{
                    "cmd", "/c", "ren", LOCKED_FOLDER_NAME, unlockedFolderName
            });
            if (renameCode != 0 || !unlockedPath.toFile().exists()) {
                System.out.println("[SECURITY] UNLOCK FAILED (rename)");
                return;
            }

            System.out.println("[SECURITY] UNLOCKED");
        } catch (Exception e) {
            System.out.println("[SECURITY] UNLOCK FAILED: " + e.getMessage());
        }
    }

    // =========================
    // 🧠 trạng thái
    // =========================
    public boolean isLocked() {
        return lockedPath.toFile().exists();
    }

    public void ensureUnlocked() {
        if (isLocked()) unlock();
    }

    public void ensureLocked() {
        if (!isLocked()) lock();
    }

    // =========================
    // 🚫 ACL (PRIVATE)
    // =========================
    private void applyDeleteProtection(String path) {
        try {
            String user = System.getProperty("user.name");

            int code = runCommand(new String[]{"icacls", path, "/deny", user + ":(D)"});
            if (code == 0) {
                System.out.println("[SECURITY] Delete protected");
            } else {
                System.out.println("[SECURITY] Delete protection failed, code=" + code);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void removeDeleteProtection(String path) {
        try {
            String user = System.getProperty("user.name");
            int code = runCommand(new String[]{"icacls", path, "/remove:d", user});
            if (code == 0) {
                System.out.println("[SECURITY] Delete protection removed");
            } else {
                System.out.println("[SECURITY] Remove delete protection skipped/failed, code=" + code);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int runCommand(String[] command) throws Exception {
        Process process = new ProcessBuilder(command)
                .directory(parentPath.toFile())
                .redirectErrorStream(true)
                .start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
        int code = process.waitFor();
        if (!output.isEmpty()) {
            System.out.println("[SECURITY] " + output);
        }
        return code;
    }
}