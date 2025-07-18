package com.cctv.road.weather.controller;

import com.cctv.road.weather.util.GeoUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/geo")
public class GeoController {

  @GetMapping("/is-korea")
  public ResponseEntity<Boolean> isKorea(@RequestParam double lat, @RequestParam double lon) {
    try {
      boolean inRange = lat >= 33.0 && lat <= 39.5 && lon >= 124.5 && lon <= 131.0;
      if (!inRange)
        return ResponseEntity.ok(false);

      GeoUtil.RegionCodes region = GeoUtil.getRegionCodes(lat, lon);
      return ResponseEntity.ok(region != null);
    } catch (Exception e) {
      return ResponseEntity.ok(false); // 예외 나면 false 처리
    }
  }

}
