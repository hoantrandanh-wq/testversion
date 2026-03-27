package com.app.config;

import okhttp3.*;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Đọc error-YYYY-MM-DD.log từ disk, batch gửi lên Loggly mỗi 5 phút khi có internet.
 * Khi offline: bỏ qua, thử lại lần sau.
 * File hôm nay: luôn check và chỉ lưu sentLines.
 * File các ngày cũ: gửi xong thì đánh dấu complete để không check lại.
 */
@Component
public class LogglyBatchSender {

    private static final Logger log = LoggerFactory.getLogger(LogglyBatchSender.class);
    private static final int BATCH_SIZE = 50;
    private static final String SYNC_FILENAME = "loggly-sync.json";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static class FileSyncState {
        long sentLines;
        boolean complete;

        FileSyncState(long sentLines, boolean complete) {
            this.sentLines = sentLines;
            this.complete = complete;
        }
    }

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build();

    // Chạy sau 30s khởi động, sau đó mỗi 5 phút
    @Scheduled(fixedDelay = 300_000, initialDelay = 30_000)
    public void syncLogsToLoggly() {
        String token = resolveToken();
        if (token == null || token.isEmpty()) {
            return;
        }

        if (!isOnline()) {
            log.debug("Offline - skipping Loggly sync");
            return;
        }

        sendUnsentLogs(token);
    }

    private String resolveToken() {
        String token = System.getenv("LOGGLY_TOKEN");
        if (token == null || token.isEmpty()) {
            token = System.getProperty("LOGGLY_TOKEN");
        }
        return token;
    }

    private boolean isOnline() {
        try {
            Request request = new Request.Builder()
                    .url("https://logs-01.loggly.com")
                    .head()
                    .build();
            try (Response response = httpClient.newCall(request).execute()) {
                return response.code() < 500;
            }
        } catch (Exception e) {
            return false;
        }
    }

    private void sendUnsentLogs(String token) {
        File logDir = new File(System.getProperty("user.home") + "/.helloworld-app/logs");
        if (!logDir.exists()) return;

        File syncFile = new File(logDir, SYNC_FILENAME);
        Map<String, FileSyncState> states = loadStates(syncFile);
        boolean updated = false;

        String todayFileName = "error-" + LocalDate.now().format(DATE_FORMATTER) + ".log";

        // Chỉ xử lý file theo chuẩn error-YYYY-MM-DD.log
        File[] logFiles = logDir.listFiles(f ->
                f.getName().matches("error-\\d{4}-\\d{2}-\\d{2}\\.log")
        );
        if (logFiles == null || logFiles.length == 0) return;

        Arrays.sort(logFiles, Comparator.comparing(File::getName));

        for (File logFile : logFiles) {
            String fileName = logFile.getName();
            boolean isTodayFile = fileName.equals(todayFileName);

            FileSyncState state = states.get(fileName);
            if (state == null) {
                state = new FileSyncState(0L, false);
                states.put(fileName, state);
                updated = true;
            }

            if (!isTodayFile && state.complete) {
                continue;
            }

            // File hôm nay không bao giờ gắn complete
            if (isTodayFile && state.complete) {
                state.complete = false;
                updated = true;
            }

            long newSentLines = sendFileFromLine(logFile, state.sentLines, token);
            if (newSentLines > state.sentLines) {
                state.sentLines = newSentLines;
                updated = true;
            }

            if (!isTodayFile) {
                long totalLines = countLines(logFile);
                if (state.sentLines >= totalLines && !state.complete) {
                    state.complete = true;
                    updated = true;
                    log.info("Marked {} as complete", fileName);
                }
            }
        }

        if (updated) {
            saveStates(syncFile, states);
        }
    }

    /**
     * Đọc file từ dòng skipLines, gửi batch lên Loggly.
     * Trả về số dòng đã gửi thành công.
     */
    private long sendFileFromLine(File logFile, long skipLines, String token) {
        if (!logFile.exists() || logFile.length() == 0) return skipLines;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(logFile), StandardCharsets.UTF_8))) {

            // Skip các dòng đã gửi
            long currentLine = 0;
            while (currentLine < skipLines) {
                if (reader.readLine() == null) return skipLines; // file ngắn hơn offset
                currentLine++;
            }

            // Đọc và gửi batch
            List<String> batch = new ArrayList<>();
            long lastSuccessLine = skipLines;
            String line;

            while ((line = reader.readLine()) != null) {
                currentLine++;
                if (!line.trim().isEmpty()) {
                    batch.add(line);
                }

                if (batch.size() >= BATCH_SIZE) {
                    if (!sendBatch(batch, token)) return lastSuccessLine;
                    lastSuccessLine = currentLine;
                    batch.clear();
                }
            }

            // Gửi phần còn lại
            if (!batch.isEmpty()) {
                if (sendBatch(batch, token)) {
                    lastSuccessLine = currentLine;
                }
            }

            return lastSuccessLine;

        } catch (Exception e) {
            log.warn("Failed to read log file {}: {}", logFile.getName(), e.getMessage());
            return skipLines;
        }
    }

    private boolean sendBatch(List<String> lines, String token) {
        try {
            String payload = String.join("\n", lines);
            String bulkUrl = "https://logs-01.loggly.com/bulk/" + token + "/tag/java-app/";

            RequestBody body = RequestBody.create(payload, MediaType.parse("text/plain"));
            Request request = new Request.Builder()
                    .url(bulkUrl)
                    .post(body)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    log.debug("Loggly sync: sent {} lines", lines.size());
                    return true;
                } else {
                    log.warn("Loggly bulk failed: HTTP {}", response.code());
                    return false;
                }
            }
        } catch (Exception e) {
            log.warn("Loggly send error: {}", e.getMessage());
            return false;
        }
    }

    private long countLines(File file) {
        if (!file.exists()) return 0L;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            long count = 0;
            while (reader.readLine() != null) {
                count++;
            }
            return count;
        } catch (Exception e) {
            log.warn("Failed to count lines for {}: {}", file.getName(), e.getMessage());
            return 0L;
        }
    }

    private Map<String, FileSyncState> loadStates(File syncFile) {
        Map<String, FileSyncState> states = new HashMap<>();
        if (!syncFile.exists()) return states;

        try {
            String content = new String(Files.readAllBytes(syncFile.toPath()), StandardCharsets.UTF_8);
            JSONObject root = new JSONObject(content);

            // New format: {"files": {"error-2026-03-26.log": {"sentLines": 12, "complete": true}}}
            if (root.has("files") && root.get("files") instanceof JSONObject) {
                JSONObject files = root.getJSONObject("files");
                for (String fileName : files.keySet()) {
                    JSONObject fileState = files.optJSONObject(fileName);
                    if (fileState == null) continue;
                    long sentLines = fileState.optLong("sentLines", 0L);
                    boolean complete = fileState.optBoolean("complete", false);
                    states.put(fileName, new FileSyncState(sentLines, complete));
                }
                return states;
            }

            // Backward compatibility: old format {"error-2026-03-26.log": 12}
            for (String fileName : root.keySet()) {
                Object value = root.get(fileName);
                if (value instanceof Number) {
                    states.put(fileName, new FileSyncState(((Number) value).longValue(), false));
                }
            }
        } catch (Exception e) {
            log.warn("Failed to load loggly-sync.json: {}", e.getMessage());
        }
        return states;
    }

    private void saveStates(File syncFile, Map<String, FileSyncState> states) {
        try {
            JSONObject files = new JSONObject();
            for (Map.Entry<String, FileSyncState> entry : states.entrySet()) {
                JSONObject fileState = new JSONObject();
                fileState.put("sentLines", entry.getValue().sentLines);
                fileState.put("complete", entry.getValue().complete);
                files.put(entry.getKey(), fileState);
            }

            JSONObject root = new JSONObject();
            root.put("files", files);
            Files.write(syncFile.toPath(), root.toString().getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.warn("Failed to save loggly-sync.json: {}", e.getMessage());
        }
    }
}
