package com.example.airalert;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class AirAlertMain {

    private static final String CONFIG_FILE = "config.properties";

    public static void main(String[] args) {
        Properties config;
        try {
            config = loadConfig();
        } catch (IOException e) {
            System.err.println("설정 파일(" + CONFIG_FILE + ")을 읽을 수 없습니다. "
                    + "config.properties.example 파일을 참고해 config.properties를 만들어 주세요.");
            e.printStackTrace();
            return;
        }

        String serviceKey = requireProperty(config, "airkorea.serviceKey");
        String stationName = requireProperty(config, "airkorea.stationName");
        String webhookUrl = requireProperty(config, "mattermost.webhookUrl");

        AirKoreaClient airKoreaClient = new AirKoreaClient(serviceKey, stationName);
        MattermostClient mattermostClient = new MattermostClient(webhookUrl);

        try {
            AirData airData = airKoreaClient.fetchAirData();
            String message = buildMessage(airData, stationName);
            mattermostClient.sendMessage(message);
            System.out.println("알림 전송 성공");
        } catch (Exception e) {
            e.printStackTrace();

            String errorMessage = "[역삼동 미세먼지 알림]\n"
                    + "현재 측정값을 확인할 수 없습니다.\n"
                    + "측정소 점검 또는 데이터 수집 지연 가능성이 있습니다.";

            try {
                mattermostClient.sendMessage(errorMessage);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private static String buildMessage(AirData airData, String stationName) {
        String pm10Guide = guideMessageForPm10(airData.getPm10());
        String pm25Guide = guideMessageForPm25(airData.getPm25());

        String pm10Icon = getGradeIcon(airData.getPm10Grade());
        String pm25Icon = getGradeIcon(airData.getPm25Grade());

        return "### [🐶 킁킁: 오늘 공기는 어떨까? 내가 미리 냄새 맡고 알려줄게! 🐾]\n\n"
                + " "
                + "**기준시각:** " + airData.getDataTime() + "\n **측정소:** " + stationName + "\n\n"
                + " "
                + "| 구분 | 미세먼지(PM10) | 초미세먼지(PM2.5) |\n"
                + "| :--- | :---: | :---: |\n"
                + "| **상태** | " + pm10Icon + " **" + airData.getPm10Grade() + "** | " + pm25Icon + " **"
                + airData.getPm25Grade() + "** |\n"
                + "| **현재 농도** | " + airData.getPm10() + " ㎍/㎥ | " + airData.getPm25() + " ㎍/㎥ |\n"
                + "| **킁킁이 진단** | " + pm10Guide + " | " + pm25Guide + " |";
    }

    // 등급에 따른 색상 이모지를 반환하는 헬퍼 메서드
    private static String getGradeIcon(String grade) {
        if (grade == null)
            return "⚪";
        if (grade.contains("좋음"))
            return "🔵"; // 파랑 (Clean)
        if (grade.contains("보통"))
            return "🟢"; // 초록 (Moderate)
        if (grade.contains("나쁨"))
            return "🟠"; // 주황 (Bad)
        if (grade.contains("매우"))
            return "🔴"; // 빨강 (Very Bad)
        return "⚪";
    }

    private static String guideMessageForPm10(int pm10) {
        if (pm10 <= 22)
            return "🌿 공기가 너무 향기로워! 지금 당장 산책 가도 좋아!";
        if (pm10 <= 45)
            return "🐕 나쁘지 않은 공기야. 가벼운 외출은 괜찮아!";
        if (pm10 <= 100)
            return "😷 콜록! 먼지 냄새가 나기 시작했어. 마스크 꼭 챙겨! 집에 얼른 돌아가!";
        return "🚨 공기가 너무 매워! 오늘은 꼭꼭 숨어있자!";
    }

    private static Properties loadConfig() throws IOException {
        Properties props = new Properties();
        try (InputStream in = new FileInputStream(CONFIG_FILE)) {
            props.load(in);
        }
        return props;
    }

    private static String requireProperty(Properties props, String key) {
        String value = props.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException(
                    "필수 설정값이 비어있습니다: " + key + " (" + CONFIG_FILE + " 확인)");
        }
        return value.trim();
    }

    private static String guideMessageForPm25(int pm25) {
        if (pm25 <= 9)
            return "💎 아주 작은 먼지도 안 보여! 보석처럼 맑은 공기야.";
        if (pm25 <= 15)
            return "🧐 아주 깨끗하진 않지만 이 정도면 딱 좋아!";
        if (pm25 <= 50)
            return "⚠️ 눈에 안 보이는 나쁜 녀석들이 숨어있어! 조심해!";
        return "🚫 숨쉬기 너무 힘들어! 오늘은 절대 밖으로 나가면 안 돼!";
    }
}
