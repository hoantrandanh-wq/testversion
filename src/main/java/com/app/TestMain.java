package com.app;

import com.app.service.FolderSecurityService;

import java.io.File;
import java.io.FileWriter;

public class TestMain {

    public static void main(String[] args) throws Exception {

        String path = System.getenv("LOCALAPPDATA") + "\\MyAppData\\Data";
        System.out.println("Test path: " + path);

        FolderSecurityService sec = new FolderSecurityService(path);

        // 1. đảm bảo folder tồn tại
        sec.ensureExists();

        // 2. unlock để thao tác
        sec.ensureUnlocked();

        // 3. tạo file test
        File file = new File(path + "\\a.txt");
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("hello");
        }

        System.out.println("✅ File created");

        // 🔴 QUAN TRỌNG: không mở Explorer vào folder này

        Thread.sleep(3000);

        // 4. lock
        sec.lock();

        System.out.println("🔒 Locked - state=" + sec.isLocked());

        Thread.sleep(10000);

        // 5. unlock
        sec.unlock();

        System.out.println("🔓 Unlocked - state=" + sec.isLocked());
    }
}