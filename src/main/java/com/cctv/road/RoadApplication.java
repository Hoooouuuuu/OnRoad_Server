package com.cctv.road;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.scheduling.annotation.EnableScheduling;

import io.github.cdimascio.dotenv.Dotenv;

@EnableScheduling
@SpringBootApplication
public class RoadApplication {

	public static void main(String[] args) {
		Dotenv dotenv = Dotenv.load();

		Map<String, Object> props = new HashMap<>();
		props.put("DB_URL", dotenv.get("DB_URL"));
		props.put("DB_USERNAME", dotenv.get("DB_USERNAME"));
		props.put("DB_PASSWORD", dotenv.get("DB_PASSWORD"));
		props.put("MAIL_USERNAME", dotenv.get("MAIL_USERNAME"));
		props.put("MAIL_PASSWORD", dotenv.get("MAIL_PASSWORD"));

		new SpringApplicationBuilder(RoadApplication.class)
				.properties(props)
				.run(args);
	}
}
