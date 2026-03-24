package com.app.update.service;

import com.app.update.model.UpdateInfo;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class UpdateService {

    // ⚠️ Sửa lại đúng repo của bạn
    private static final String GITHUB_API = "https://api.github.com/repos/hoantrandanh-wq/testversion/releases";
    private static final String CURRENT_VERSION = "v1.0.18";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // File lưu trạng thái: ngày check lần cuối + version đã bỏ qua
    private static final Path PREFS_FILE = Path.of(
            System.getProperty("user.home"), ".helloworld-app", "update-prefs.json"
    );

    // Kiểm tra xem tuần này đã check chưa
    public boolean shouldCheckThisWeek() {
        try {
            if (!Files.exists(PREFS_FILE)) return true;
            String content = Files.readString(PREFS_FILE);
            JSONObject prefs = new JSONObject(content);
            if (!prefs.has("lastCheckDate")) return true;

            LocalDate lastCheck = LocalDate.parse(prefs.getString("lastCheckDate"), FORMATTER);
            LocalDate now = LocalDate.now();

            // Khác tuần thì check lại
            return !isSameWeek(lastCheck, now);
        } catch (Exception e) {
            return true;
        }
    }

    // Lưu ngày check hôm nay
    public void saveCheckDate() {
        try {
            Files.createDirectories(PREFS_FILE.getParent());
            JSONObject prefs = loadPrefs();
            prefs.put("lastCheckDate", LocalDate.now().format(FORMATTER));
            Files.writeString(PREFS_FILE, prefs.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Lưu version người dùng đã bỏ qua
    public void saveSkippedVersion(String version) {
        try {
            Files.createDirectories(PREFS_FILE.getParent());
            JSONObject prefs = loadPrefs();
            prefs.put("skippedVersion", version);
            Files.writeString(PREFS_FILE, prefs.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Lấy version đã bỏ qua
    public String getSkippedVersion() {
        try {
            JSONObject prefs = loadPrefs();
            return prefs.optString("skippedVersion", "");
        } catch (Exception e) {
            return "";
        }
    }

    // Gọi GitHub API lấy release mới nhất
    public UpdateInfo checkLatestVersion() {
        try {
            System.out.println("Bắt đầu kiểm tra phiên bản...");

            // Tạo HttpClient không kiểm tra SSL
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(java.time.Duration.ofSeconds(5))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GITHUB_API))
                    .header("Accept", "application/vnd.github.v3+json")
                    .header("User-Agent", "Java-App")
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                System.out.println("Lỗi HTTP: " + response.statusCode());
                return new UpdateInfo(CURRENT_VERSION, "", false);
            }

            JSONArray releases = new JSONArray(response.body());
            if (releases.isEmpty()) {
                return new UpdateInfo(CURRENT_VERSION, "", false);
            }

            JSONObject latest = releases.getJSONObject(0);
            String latestVersion = latest.getString("tag_name");
            String downloadUrl = "";

            JSONArray assets = latest.getJSONArray("assets");
            for (int i = 0; i < assets.length(); i++) {
                JSONObject asset = assets.getJSONObject(i);
                if (asset.getString("name").endsWith(".exe")) {
                    downloadUrl = asset.getString("browser_download_url");
                    break;
                }
            }

            System.out.println("Phiên bản mới nhất: " + latestVersion);
            boolean hasUpdate = !latestVersion.equals(CURRENT_VERSION);
            return new UpdateInfo(latestVersion, downloadUrl, hasUpdate);

        } catch (Exception e) {
            System.out.println("Lỗi check version: " + e.getMessage());
            e.printStackTrace();
            return new UpdateInfo(CURRENT_VERSION, "", false);
        }
    }

    // Download file .exe mới về Desktop
    public File downloadInstaller(String downloadUrl) throws Exception {
        URL url = URI.create(downloadUrl).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(10000);

        Path dest = Path.of(
                System.getProperty("user.home"), "Desktop", "HelloWorldApp-update.exe"
        );

        try (InputStream in = conn.getInputStream();
             OutputStream out = Files.newOutputStream(dest)) {
            in.transferTo(out);
        }
        return dest.toFile();
    }

    // --- Helpers ---

    private JSONObject loadPrefs() {
        try {
            if (!Files.exists(PREFS_FILE)) return new JSONObject();
            return new JSONObject(Files.readString(PREFS_FILE));
        } catch (Exception e) {
            return new JSONObject();
        }
    }

    private boolean isSameWeek(LocalDate d1, LocalDate d2) {
        // Cùng năm và cùng số tuần trong năm
        java.time.temporal.WeekFields wf = java.time.temporal.WeekFields.ISO;
        return d1.get(wf.weekOfWeekBasedYear()) == d2.get(wf.weekOfWeekBasedYear())
                && d1.getYear() == d2.getYear();
    }

}