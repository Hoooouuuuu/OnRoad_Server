package com.cctv.road.weather.dto;

import lombok.Data;

@Data
public class MidTermTemperatureResponse {
    private Response response;

    @Data
    public static class Response {
        private Header header;
        private Body body;
    }

    @Data
    public static class Header {
        private String resultCode;
        private String resultMsg;
    }

    @Data
    public static class Body {
        private String dataType;
        private Items items;
        private int pageNo;
        private int numOfRows;
        private int totalCount;
    }

    @Data
    public static class Items {
        private TemperatureItem item;
    }

    @Data
    public static class TemperatureItem {
        private String regId;
        private int taMin4, taMin5, taMin6, taMin7, taMin8, taMin9, taMin10;
        private int taMax4, taMax5, taMax6, taMax7, taMax8, taMax9, taMax10;
    }
}
