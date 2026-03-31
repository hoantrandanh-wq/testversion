package com.app;

import com.app.file.service.FolderSecurityService;

import java.io.File;
import java.io.FileWriter;

public class TestMain {

    public static void main(String[] args) throws Exception {

        String path = System.getenv("LOCALAPPDATA") + "\\MyAppData\\Data";
        System.out.println("Test path: " + path);

        FolderSecurityService sec = new FolderSecurityService(path);

        sec.ensureExists();

        sec.ensureUnlocked();

        File file = new File(path + "\\a.txt");
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("hello");
        }

        System.out.println("✅ File created");

        Thread.sleep(3000);

        sec.lock();

        System.out.println("🔒 Locked - state=" + sec.isLocked());

        Thread.sleep(10000);

        sec.unlock();

        System.out.println("🔓 Unlocked - state=" + sec.isLocked());
    }
}