package com.cctv.road.weather.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

@Service
public class StationMapLoader {

    private final Map<String, String> regionToStationMap;

    public StationMapLoader() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        InputStream is = new ClassPathResource("json/air.json").getInputStream();
        this.regionToStationMap = objectMapper.readValue(is, new TypeReference<>() {});
    }

    public String getStation(String regionName) {
        return regionToStationMap.get(regionName);
    }

    public Map<String, String> getAll() {
        return regionToStationMap;
    }
}
