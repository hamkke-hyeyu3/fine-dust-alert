package com.example.airalert;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;

public class AirKoreaClient {

    private static final OpenApiError[] OPEN_API_ERRORS = {
            new OpenApiError("Unauthorized",
                    "API 인증키가 존재하지 않거나 유효하지 않습니다. 공공데이터포털에서 발급받은 인증키 정보를 확인해 주세요."),
            new OpenApiError("Forbidden",
                    "API 서비스에 대한 신청내역이 확인되지 않습니다. 해당 API의 활용신청 여부와 승인 상태를 확인해 주세요."),
            new OpenApiError("API not found",
                    "API 서비스가 존재하지 않습니다. 호출 URL에 오타가 없는지, 폐기된 API는 아닌지 확인해 주세요."),
            new OpenApiError("Error forwarding request to backend server",
                    "기관 API 서버와의 연결에 실패했습니다. 일시적인 네트워크 오류일 수 있으니 잠시 후 다시 시도해 주세요."),
            new OpenApiError("Error receiving response from backend server",
                    "기관 API 서버로부터 응답을 받지 못했습니다. 문제가 계속될 경우, '관리부서 전화번호' 혹은 '오류신고 및 문의'를 통해 제공기관에 문의바랍니다."),
            new OpenApiError("API rate limit exceeded",
                    "현재 많은 사용자가 API를 호출하고 있어, 서버의 최대 동시 요청 수를 초과하였습니다. 잠시 후 다시 호출해주시기 바랍니다."),
            new OpenApiError("API token quota exceeded",
                    "API 서비스의 일일 호출 허용량을 초과하였습니다. 초기화된 이후 다시 이용 바랍니다."),
            new OpenApiError("Unexpected error",
                    "일시적인 시스템 오류가 발생하였습니다. 문제가 반복될 경우 활용지원센터로 문의바랍니다.")
    };

    private final String serviceKey;
    private final String stationName;

    public AirKoreaClient(String serviceKey, String stationName) {
        this.serviceKey = serviceKey;
        this.stationName = stationName;
    }

    public AirData fetchAirData() throws Exception {
        StringBuilder urlBuilder = new StringBuilder(
                "http://apis.data.go.kr/B552584/ArpltnInforInqireSvc/getMsrstnAcctoRltmMesureDnsty");

        urlBuilder.append("?serviceKey=").append(serviceKey);
        urlBuilder.append("&returnType=json");
        urlBuilder.append("&numOfRows=1");
        urlBuilder.append("&pageNo=1");
        urlBuilder.append("&stationName=").append(URLEncoder.encode(stationName, "UTF-8"));
        urlBuilder.append("&dataTerm=DAILY");
        urlBuilder.append("&ver=1.3");

        URL url = new URL(urlBuilder.toString());
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);

        try {
            int responseCode = conn.getResponseCode();
            String response = readResponse(responseCode == 200 ? conn.getInputStream() : conn.getErrorStream());
            OpenApiError openApiError = findOpenApiError(response, responseCode);

            if (openApiError != null) {
                logOpenApiError(openApiError, responseCode, response);
                throw new RuntimeException("오픈API 오류: " + openApiError.message + " - " + openApiError.description);
            }

            if (responseCode != 200) {
                throw new RuntimeException("에어코리아 API 호출 실패. HTTP CODE: " + responseCode);
            }

            return parseAirData(response);
        } finally {
            conn.disconnect();
        }
    }

    private String readResponse(InputStream inputStream) throws Exception {
        if (inputStream == null) {
            return "";
        }

        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
        StringBuilder response = new StringBuilder();
        String line;

        try {
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
        } finally {
            br.close();
        }

        return response.toString();
    }

    private OpenApiError findOpenApiError(String response, int responseCode) {
        String normalizedResponse = response == null ? "" : response.toLowerCase();

        for (OpenApiError error : OPEN_API_ERRORS) {
            if (normalizedResponse.contains(error.message.toLowerCase())) {
                return error;
            }
        }

        if (responseCode == 401) {
            return findOpenApiErrorByMessage("Unauthorized");
        }

        if (responseCode == 403) {
            return findOpenApiErrorByMessage("Forbidden");
        }

        if (responseCode == 404) {
            return findOpenApiErrorByMessage("API not found");
        }

        if (responseCode == 429) {
            return findOpenApiErrorByMessage("API rate limit exceeded");
        }

        return null;
    }

    private OpenApiError findOpenApiErrorByMessage(String message) {
        for (OpenApiError error : OPEN_API_ERRORS) {
            if (error.message.equals(message)) {
                return error;
            }
        }

        return null;
    }

    private void logOpenApiError(OpenApiError error, int responseCode, String response) {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

        System.err.println("[오픈API 오류] 시간: " + timestamp);
        System.err.println("[오픈API 오류] 에러메시지: " + error.message);
        System.err.println("[오픈API 오류] 설명: " + error.description);
        System.err.println("[오픈API 오류] HTTP 상태: " + responseCode);

        String responsePreview = abbreviate(response, 300);
        if (responsePreview.length() > 0) {
            System.err.println("[오픈API 오류] 응답내용: " + responsePreview);
        }
    }

    private String abbreviate(String text, int maxLength) {
        if (text == null) {
            return "";
        }

        String normalizedText = text.replace('\r', ' ').replace('\n', ' ').trim();
        if (normalizedText.length() <= maxLength) {
            return normalizedText;
        }

        return normalizedText.substring(0, maxLength) + "...";
    }

    private AirData parseAirData(String json) {
        String dataTime = extractValue(json, "\"dataTime\":\"", "\"");
        String pm10ValueStr = extractValue(json, "\"pm10Value\":\"", "\"");
        String pm25ValueStr = extractValue(json, "\"pm25Value\":\"", "\"");

        if (pm10ValueStr == null || pm25ValueStr == null) {
            throw new RuntimeException("미세먼지 데이터를 찾을 수 없습니다.");
        }

        if ("-".equals(pm10ValueStr) || "-".equals(pm25ValueStr)) {
            throw new RuntimeException("측정값이 비어 있습니다.");
        }

        int pm10 = Integer.parseInt(pm10ValueStr.trim());
        int pm25 = Integer.parseInt(pm25ValueStr.trim());

        String pm10Grade = AirGradeUtil.getPm10Grade(pm10);
        String pm25Grade = AirGradeUtil.getPm25Grade(pm25);

        return new AirData(dataTime, pm10, pm25, pm10Grade, pm25Grade);
    }

    private String extractValue(String text, String startToken, String endToken) {
        int startIndex = text.indexOf(startToken);
        if (startIndex == -1) {
            return null;
        }

        startIndex += startToken.length();
        int endIndex = text.indexOf(endToken, startIndex);

        if (endIndex == -1) {
            return null;
        }

        return text.substring(startIndex, endIndex);
    }

    private static class OpenApiError {
        private final String message;
        private final String description;

        private OpenApiError(String message, String description) {
            this.message = message;
            this.description = description;
        }
    }
}
