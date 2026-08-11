package com.likelion.duckswell.domain.weather.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.likelion.duckswell.domain.weather.dto.WeatherResponse;
import com.likelion.duckswell.domain.weather.exception.WeatherErrorCode;
import com.likelion.duckswell.global.exception.CustomException;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * WeatherAPI.com current.json 호출 클라이언트.
 * 기온/날씨상태/습도/자외선/미세먼지(aqi=yes)를 조회한다.
 */
@Slf4j
@Component
public class WeatherClient {

    private static final String CURRENT_WEATHER_URL = "https://api.weatherapi.com/v1/current.json";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String apiKey;

    public WeatherClient(@Value("${weather.api-key:}") String apiKey) {
        this.apiKey = apiKey;
    }

    public WeatherResponse getCurrentWeather(double lat, double lon) {
        if (apiKey.isBlank()) {
            log.error("weather.api-key 설정이 없습니다. application-local.yml 또는 application-prod.yml에 채워주세요.");
            throw new CustomException(WeatherErrorCode.WEATHER_FETCH_FAILED);
        }

        URI uri = URI.create(CURRENT_WEATHER_URL
                + "?key=" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8)
                + "&q=" + URLEncoder.encode(lat + "," + lon, StandardCharsets.UTF_8)
                + "&aqi=yes");

        String responseBody;
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .timeout(REQUEST_TIMEOUT)
                    .GET()
                    .build();
            HttpResponse<String> httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            responseBody = httpResponse.body();

            if (httpResponse.statusCode() >= 400) {
                log.error("WeatherAPI 호출 실패: status={}, body={}", httpResponse.statusCode(), responseBody);
                throw new CustomException(WeatherErrorCode.WEATHER_FETCH_FAILED);
            }
        } catch (IOException | InterruptedException e) {
            log.error("WeatherAPI 호출 실패", e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new CustomException(WeatherErrorCode.WEATHER_FETCH_FAILED);
        }

        return parseResponse(responseBody);
    }

    private WeatherResponse parseResponse(String responseBody) {
        try {
            JsonNode current = objectMapper.readTree(responseBody).path("current");
            JsonNode airQuality = current.path("air_quality");

            return new WeatherResponse(
                    current.path("temp_c").asDouble(),
                    current.path("condition").path("text").asText(),
                    current.path("humidity").asInt(),
                    current.path("uv").asDouble(),
                    airQuality.path("pm10").asDouble(),
                    airQuality.path("pm2_5").asDouble(),
                    airQuality.path("us-epa-index").asInt()
            );
        } catch (Exception e) {
            log.error("WeatherAPI 응답 파싱 실패: {}", responseBody, e);
            throw new CustomException(WeatherErrorCode.WEATHER_FETCH_FAILED);
        }
    }
}
