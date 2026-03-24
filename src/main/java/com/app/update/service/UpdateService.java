package com.app.update.service;

import com.app.update.model.UpdateInfo;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;

@Service
public class UpdateService {
    private static final String UPDATE_URL = "https://your-username.github.io/your-repo/latest.json";

    public boolean isOnline() {
        try {
            new URL("https://www.google.com").openConnection().connect();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public UpdateInfo getLastVersion() {
        try {
            URL url = new URL(UPDATE_URL);
            BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));

            StringBuilder result = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line);
            }
            JSONObject json = new JSONObject(result.toString());
            return new UpdateInfo(json.getString("version"), json.getString("downloadUrl"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public int compareVersion(String v1, String v2) {
        String[] a = v1.split("\\.");
        String[] b = v2.split("\\.");

        for (int i = 0; i < Math.max(a.length, b.length); i++) {
            int num1 = i < a.length ? Integer.parseInt(a[i]) : 0;
            int num2 = i < b.length ? Integer.parseInt(b[i]) : 0;

            if (num1 != num2) return num1 - num2;
        }
        return 0;
    }

}
