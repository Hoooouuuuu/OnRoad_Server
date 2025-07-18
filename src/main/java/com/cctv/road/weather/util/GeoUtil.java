package com.cctv.road.weather.util;

import java.util.Comparator;
import java.util.List;

public class GeoUtil {

    // ✅ 좌표 변환용 클래스
    public static class GridXY {
        public final int nx;
        public final int ny;

        public GridXY(int nx, int ny) {
            this.nx = nx;
            this.ny = ny;
        }

        @Override
        public String toString() {
            return "GridXY{nx=" + nx + ", ny=" + ny + '}';
        }
    }

    // ✅ 위도/경도 → 기상청 격자좌표 변환
    public static GridXY convertGRID(double lat, double lon) {
        double RE = 6371.00877;
        double GRID = 5.0;
        double SLAT1 = 30.0;
        double SLAT2 = 60.0;
        double OLON = 126.0;
        double OLAT = 38.0;
        double XO = 43;
        double YO = 136;

        double DEGRAD = Math.PI / 180.0;

        double re = RE / GRID;
        double slat1 = SLAT1 * DEGRAD;
        double slat2 = SLAT2 * DEGRAD;
        double olon = OLON * DEGRAD;
        double olat = OLAT * DEGRAD;

        double sn = Math.log(Math.cos(slat1) / Math.cos(slat2)) /
                Math.log(Math.tan(Math.PI * 0.25 + slat2 * 0.5) /
                         Math.tan(Math.PI * 0.25 + slat1 * 0.5));
        double sf = Math.pow(Math.tan(Math.PI * 0.25 + slat1 * 0.5), sn) *
                    Math.cos(slat1) / sn;
        double ro = re * sf / Math.pow(Math.tan(Math.PI * 0.25 + olat * 0.5), sn);

        double ra = re * sf / Math.pow(Math.tan(Math.PI * 0.25 + lat * DEGRAD * 0.5), sn);
        double theta = lon * DEGRAD - olon;
        if (theta > Math.PI) theta -= 2.0 * Math.PI;
        if (theta < -Math.PI) theta += 2.0 * Math.PI;
        theta *= sn;

        int nx = (int) Math.floor(ra * Math.sin(theta) + XO + 0.5);
        int ny = (int) Math.floor(ro - ra * Math.cos(theta) + YO + 0.5);

        return new GridXY(nx, ny);
    }

    // ✅ 기존 단일 regId (중기 날씨용만) – 백워드 호환용
    public static String getMidTermRegionId(double lat, double lon) {
        List<Region> regions = List.of(
            new Region("11B00000", "서울/경기", 37.5665, 126.9780),
            new Region("11D10000", "강원도", 37.8228, 128.1555),
            new Region("11C10000", "충북", 36.6358, 127.4917),
            new Region("11C20000", "충남", 36.5184, 126.8000),
            new Region("11F10000", "전북", 35.8200, 127.1088),
            new Region("11F20000", "전남", 34.8161, 126.4629),
            new Region("11H10000", "경북", 36.0190, 128.3456),
            new Region("11H20000", "경남", 35.4606, 128.2132),
            new Region("11G00000", "제주", 33.3846, 126.5530)
        );

        return regions.stream()
            .min(Comparator.comparingDouble(r -> haversine(lat, lon, r.lat, r.lon)))
            .map(r -> r.regId)
            .orElse("11B00000");
    }

    // ✅ 날씨 + 기온 코드 함께 반환하는 클래스
    public static class RegionCodes {
        public final String name;
        public final String landRegId; // 날씨용
        public final String taRegId;   // 기온용

        public RegionCodes(String name, String landRegId, String taRegId) {
            this.name = name;
            this.landRegId = landRegId;
            this.taRegId = taRegId;
        }

        @Override
        public String toString() {
            return name + " → 날씨용: " + landRegId + ", 기온용: " + taRegId;
        }
    }

    // ✅ 주요 도시 정보 DB (위도/경도 + 코드 포함)
    private static final List<RegionInfo> REGION_DB = List.of(
        new RegionInfo("서울", "11B00000", "11H10701", 37.5665, 126.9780),
        new RegionInfo("부산", "11H20000", "11H20201", 35.1796, 129.0756),
        new RegionInfo("대구", "11H10000", "11H10701", 35.8714, 128.6014),
        new RegionInfo("인천", "11B00000", "11H10501", 37.4563, 126.7052),
        new RegionInfo("광주", "11F20000", "11H20401", 35.1595, 126.8526),
        new RegionInfo("대전", "11C20000", "11H10601", 36.3504, 127.3845),
        new RegionInfo("울산", "11H20000", "11H20301", 35.5384, 129.3114),
        new RegionInfo("제주", "11G00000", "11H20601", 33.4996, 126.5312),
        new RegionInfo("수원", "11B00000", "11H10601", 37.2636, 127.0286),
        new RegionInfo("춘천", "11D10000", "11H10101", 37.8813, 127.7298),
        new RegionInfo("전주", "11F10000", "11H10401", 35.8242, 127.1480),
        new RegionInfo("창원", "11H20000", "11H20101", 35.2281, 128.6811),
        new RegionInfo("청주", "11C10000", "11H10501", 36.6424, 127.4890)
    );

    // ✅ 위도/경도로 가장 가까운 도시의 날씨 + 기온용 코드 자동 추정
    public static RegionCodes getRegionCodes(double lat, double lon) {
        RegionInfo closest = REGION_DB.stream()
            .min(Comparator.comparingDouble(r -> haversine(lat, lon, r.lat, r.lon)))
            .orElseThrow();

        return new RegionCodes(closest.name, closest.landRegId, closest.taRegId);
    }

    // ✅ 거리 계산용 (위도/경도)
    private static double haversine(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                 + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                 * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    // ✅ 중기예보 단일코드용 Region (기존 버전과 호환)
    private static class Region {
        String regId;
        String name;
        double lat;
        double lon;

        Region(String regId, String name, double lat, double lon) {
            this.regId = regId;
            this.name = name;
            this.lat = lat;
            this.lon = lon;
        }
    }

    // ✅ 중기예보 land + ta 코드 포함된 버전
    private static class RegionInfo {
        String name;
        String landRegId;
        String taRegId;
        double lat;
        double lon;

        RegionInfo(String name, String landRegId, String taRegId, double lat, double lon) {
            this.name = name;
            this.landRegId = landRegId;
            this.taRegId = taRegId;
            this.lat = lat;
            this.lon = lon;
        }
    }
}
