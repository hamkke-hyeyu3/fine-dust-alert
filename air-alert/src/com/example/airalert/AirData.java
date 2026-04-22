package com.example.airalert;

public class AirData {

    private String dataTime;
    private int pm10;
    private int pm25;
    private String pm10Grade;
    private String pm25Grade;

    public AirData(String dataTime, int pm10, int pm25, String pm10Grade, String pm25Grade) {
        this.dataTime = dataTime;
        this.pm10 = pm10;
        this.pm25 = pm25;
        this.pm10Grade = pm10Grade;
        this.pm25Grade = pm25Grade;
    }

    public String getDataTime() {
        return dataTime;
    }

    public int getPm10() {
        return pm10;
    }

    public int getPm25() {
        return pm25;
    }

    public String getPm10Grade() {
        return pm10Grade;
    }

    public String getPm25Grade() {
        return pm25Grade;
    }
}