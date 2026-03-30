package com.app.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * Quản lý thư mục Data bảo mật và thư mục Temp làm việc.
 *
 * Luồng sử dụng:
 *   init()              → gọi khi app khởi động
 *   openFileToTemp()    → unlock Data, copy file ra Temp, lock Data ngay
 *   saveFileFromTemp()  → unlock Data, copy file từ Temp về Data, lock Data ngay
 *   shutdown()          → lock Data, xóa Temp khi tắt app
 */
@Service
public class DataFolderManager {

    private static final Logger log = LoggerFactory.getLogger(DataFolderManager.class);

    private final File dataDir;
    private final File tempDir;
    private final FolderSecurityService security;

    public DataFolderManager(@Value("${user.home}") String userHome) {
        String appDir = userHome + "/.helloworld-app";
        this.dataDir = new File(appDir + "/data");
        this.tempDir = new File(appDir + "/temp");
        this.security = new FolderSecurityService(dataDir.getAbsolutePath());
    }

    /**
     * Gọi khi app khởi động:
     * - Đảm bảo thư mục data tồn tại
     * - Re-lock nếu crash lần trước để hở
     * - Xóa temp thừa từ lần chạy trước
     */
    public void init() {
        security.ensureExists();
        security.ensureLocked();
        clearTemp();
        log.info("DataFolderManager initialized, data={}", dataDir.getAbsolutePath());
    }

    /**
     * Gọi khi tắt app:
     * - Đảm bảo data đã khóa
     * - Xóa temp
     */
    public void shutdown() {
        security.ensureLocked();
        clearTemp();
        log.info("DataFolderManager shut down");
    }

    /**
     * Unlock Data, copy 1 file theo tên ra Temp, lock Data ngay lập tức.
     *
     * @param fileName tên file cần đọc (ví dụ: "video.mp4")
     * @return File trong Temp để app đọc
     */
    public File openFileToTemp(String fileName) throws IOException {
        tempDir.mkdirs();
        File dest = new File(tempDir, fileName);

        security.ensureUnlocked();
        try {
            File source = new File(dataDir, fileName);
            Files.copy(source.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } finally {
            security.ensureLocked();
        }

        log.info("Opened {} to temp", fileName);
        return dest;
    }

    /**
     * Unlock Data, copy 1 file từ Temp về Data, lock Data ngay lập tức.
     *
     * @param fileName tên file cần ghi lại (phải đang có trong Temp)
     */
    public void saveFileFromTemp(String fileName) throws IOException {
        File source = new File(tempDir, fileName);

        security.ensureUnlocked();
        try {
            File dest = new File(dataDir, fileName);
            Files.copy(source.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } finally {
            security.ensureLocked();
        }

        log.info("Saved {} from temp to Data", fileName);
    }

    /**
     * Lấy đường dẫn file trong Temp (không unlock Data).
     * Dùng sau khi đã gọi openFileToTemp().
     */
    public File getTempFile(String fileName) {
        return new File(tempDir, fileName);
    }

    public File getDataDir() {
        return dataDir;
    }

    public File getTempDir() {
        return tempDir;
    }

    public boolean isDataLocked() {
        return security.isLocked();
    }

    // ==========================================
    // PRIVATE HELPERS
    // ==========================================

    private void clearTemp() {
        try {
            if (tempDir.exists()) {
                deleteDirectory(tempDir);
                log.debug("Temp cleared");
            }
        } catch (Exception e) {
            log.warn("Failed to clear temp: {}", e.getMessage());
        }
    }

    private void deleteDirectory(File dir) {
        if (!dir.exists()) return;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    deleteDirectory(f);
                } else {
                    f.delete();
                }
            }
        }
        dir.delete();
    }
}
