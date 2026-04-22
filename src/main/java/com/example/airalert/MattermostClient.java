package com.example.airalert;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MattermostClient {

    private final String webhookUrl;

    public MattermostClient(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    public void sendMessage(String message) throws Exception {
        URL url = new URL(webhookUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setDoOutput(true);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);

        String escapedMessage = message.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n");

        String jsonBody = "{\"text\":\"" + escapedMessage + "\"}";

        OutputStream os = conn.getOutputStream();
        os.write(jsonBody.getBytes("UTF-8"));
        os.flush();
        os.close();

        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            throw new RuntimeException("Mattermost 전송 실패. HTTP CODE: " + responseCode);
        }

        conn.disconnect();
    }
}