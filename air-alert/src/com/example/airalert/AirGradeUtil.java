package com.example.airalert;

public class AirGradeUtil {

    public static String getPm10Grade(int value) {
        if (value <= 22) return "좋음";
        if (value <= 45) return "보통";
        if (value <= 100) return "나쁨";
        return "매우나쁨";
    }

    public static String getPm25Grade(int value) {
        if (value <= 9) return "좋음";
        if (value <= 15) return "보통";
        if (value <= 50) return "나쁨";
        return "매우나쁨";
    }
}