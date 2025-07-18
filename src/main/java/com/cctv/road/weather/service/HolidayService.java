package com.cctv.road.weather.service;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class HolidayService {

    @Value("${kma.api.key}")
    private String kmaApiKey;

    public List<String> getHolidayList(int year) {
        List<String> holidays = new ArrayList<>();

        try {
            URI uri = UriComponentsBuilder
                    .fromHttpUrl("https://apis.data.go.kr/B090041/openapi/service/SpcdeInfoService/getRestDeInfo")
                    .queryParam("solYear", year)
                    .queryParam("ServiceKey", URLEncoder.encode(kmaApiKey, StandardCharsets.UTF_8))
                    .queryParam("numOfRows", 100)
                    .queryParam("_type", "xml")
                    .build(true)
                    .toUri();
            // log.info("🔍 요청 연도: {}", year);

            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(uri.toString());
            NodeList items = doc.getElementsByTagName("item");

            for (int i = 0; i < items.getLength(); i++) {
                Element item = (Element) items.item(i);
                String locdate = item.getElementsByTagName("locdate").item(0).getTextContent();
                String formatted = locdate.replaceFirst("(\\d{4})(\\d{2})(\\d{2})", "$1-$2-$3");
                holidays.add(formatted);
            }
            // log.info("🔍 응답 XML: {}", doc.getDocumentElement().getTextContent());
            // log.info("📦 아이템 수: {}", items.getLength());

            // log.info("✅ {}년 공휴일 {}건 로드 완료", year, holidays.size());

        } catch (Exception e) {
            log.error("❌ 공휴일 API 실패", e);
        }

        return holidays;
    }
}
