package com.cctv.road.weather.service;

import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WeatherNewsService {
    private final JdbcTemplate jdbcTemplate;

    public List<Map<String, Object>> getRecentNews(int limit) {
        String sql = "SELECT title, url, published_at FROM weather_articles ORDER BY published_at DESC LIMIT ?";
        return jdbcTemplate.queryForList(sql, limit);
    }
}
