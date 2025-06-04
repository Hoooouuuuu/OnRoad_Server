package com.cctv.road.map.dto;

import com.cctv.road.map.entity.Parking;
import lombok.Data;

@Data
public class ParkingDTO {

    private Long id;

    private String name;
    private String address;
    private String tel;
    private String payType;
    private String parkingType;

    private Integer capacity;
    private Integer currentCount;
    private Integer availableCount;

    private Integer baseCharge;
    private Integer baseMinutes;
    private Integer addCharge;
    private Integer addMinutes;
    private Integer dayMaxCharge;

    private String weekdayStart;
    private String weekdayEnd;
    private String holidayStart;
    private String holidayEnd;

    private Double lat;
    private Double lng;

    private String sourceType;

    // ✅ 정적 팩토리 메서드
    public static ParkingDTO fromEntity(Parking parking) {
        ParkingDTO dto = new ParkingDTO();
        dto.setId(parking.getId());
        dto.setName(parking.getName());
        dto.setAddress(parking.getAddress());
        dto.setTel(parking.getTel());
        dto.setPayType(parking.getPayType());
        dto.setParkingType(parking.getParkingType());

        dto.setCapacity(parking.getCapacity());
        dto.setCurrentCount(parking.getCurrentCount());
        dto.setAvailableCount(parking.getAvailableCount());

        dto.setBaseCharge(parking.getBaseCharge());
        dto.setBaseMinutes(parking.getBaseMinutes());
        dto.setAddCharge(parking.getAddCharge());
        dto.setAddMinutes(parking.getAddMinutes());
        dto.setDayMaxCharge(parking.getDayMaxCharge());

        dto.setWeekdayStart(parking.getWeekdayStart());
        dto.setWeekdayEnd(parking.getWeekdayEnd());
        dto.setHolidayStart(parking.getHolidayStart());
        dto.setHolidayEnd(parking.getHolidayEnd());

        dto.setLat(parking.getLat());
        dto.setLng(parking.getLng());

        dto.setSourceType(parking.getSourceType());

        return dto;
    }
}
