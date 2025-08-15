package com.github.yuu1111.tools.impl;

import com.github.yuu1111.services.astronomy.AstronomyService;
import com.github.yuu1111.tools.MCPTool;
import com.github.yuu1111.tools.ToolExecutionException;
import com.github.yuu1111.tools.ToolResponse;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 天文学的情報取得ツール
 * 日の出・日の入り、月相などの天文情報を提供
 */
public class GetAstronomicalInfo implements MCPTool {

    private static final Logger logger = LoggerFactory.getLogger(GetAstronomicalInfo.class);
    private final AstronomyService astronomyService;

    public GetAstronomicalInfo() {
        this.astronomyService = new AstronomyService();
    }

    @Override
    public String getName() {
        return "get_astronomical_info";
    }

    @Override
    public String getDescription() {
        return "Get astronomical information for a specific location and date";
    }

    @Override
    public Map<String, Object> getParameterSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "latitude", Map.of(
                    "type", "number",
                    "description", "Latitude of the location (-90 to 90)",
                    "minimum", -90,
                    "maximum", 90,
                    "example", 35.6762
                ),
                "longitude", Map.of(
                    "type", "number",
                    "description", "Longitude of the location (-180 to 180)",
                    "minimum", -180,
                    "maximum", 180,
                    "example", 139.6503
                ),
                "date", Map.of(
                    "type", "string",
                    "description", "Date in ISO 8601 format (YYYY-MM-DD)",
                    "example", "2024-01-15"
                ),
                "include_moon_phase", Map.of(
                    "type", "boolean",
                    "description", "Include moon phase information",
                    "default", true
                ),
                "include_twilight", Map.of(
                    "type", "boolean",
                    "description", "Include twilight times (civil, nautical, astronomical)",
                    "default", false
                )
            ),
            "required", new String[]{"latitude", "longitude", "date"}
        );
    }

    @Override
    public ToolResponse execute(Map<String, Object> parameters) throws ToolExecutionException {
        // パラメータ検証
        Double latitude = getDoubleParameter(parameters, "latitude");
        Double longitude = getDoubleParameter(parameters, "longitude");
        String dateStr = (String) parameters.get("date");

        if (latitude == null) {
            throw ToolExecutionException.invalidParameter("latitude", "parameter is required");
        }
        if (longitude == null) {
            throw ToolExecutionException.invalidParameter("longitude", "parameter is required");
        }
        if (dateStr == null || dateStr.isEmpty()) {
            throw ToolExecutionException.invalidParameter("date", "parameter is required");
        }

        // 緯度・経度の範囲チェック
        if (latitude < -90 || latitude > 90) {
            throw ToolExecutionException.invalidParameter("latitude", "must be between -90 and 90");
        }
        if (longitude < -180 || longitude > 180) {
            throw ToolExecutionException.invalidParameter("longitude", "must be between -180 and 180");
        }

        Boolean includeMoonPhase = (Boolean) parameters.getOrDefault("include_moon_phase", true);
        Boolean includeTwilight = (Boolean) parameters.getOrDefault("include_twilight", false);

        try {
            // 日付をパース
            LocalDate date = LocalDate.parse(dateStr);

            // 天文情報を取得
            var astronomicalInfo = astronomyService.getAstronomicalInfo(latitude, longitude, date);

            // レスポンスを構築
            Map<String, Object> response = new HashMap<>();
            response.put("location", Map.of(
                "latitude", latitude,
                "longitude", longitude
            ));
            response.put("date", dateStr);

            // 太陽情報
            Map<String, Object> sunInfo = new HashMap<>();
            sunInfo.put("sunrise", astronomicalInfo.getSunrise());
            sunInfo.put("sunset", astronomicalInfo.getSunset());
            sunInfo.put("solar_noon", astronomicalInfo.getSolarNoon());
            sunInfo.put("day_length", astronomicalInfo.getDayLength());

            if (includeTwilight) {
                sunInfo.put("twilight", Map.of(
                    "civil_dawn", astronomicalInfo.getCivilDawn(),
                    "civil_dusk", astronomicalInfo.getCivilDusk(),
                    "nautical_dawn", astronomicalInfo.getNauticalDawn(),
                    "nautical_dusk", astronomicalInfo.getNauticalDusk(),
                    "astronomical_dawn", astronomicalInfo.getAstronomicalDawn(),
                    "astronomical_dusk", astronomicalInfo.getAstronomicalDusk()
                ));
            }
            response.put("sun", sunInfo);

            // 月情報
            if (includeMoonPhase) {
                Map<String, Object> moonInfo = new HashMap<>();
                moonInfo.put("moonrise", astronomicalInfo.getMoonrise());
                moonInfo.put("moonset", astronomicalInfo.getMoonset());
                moonInfo.put("phase", astronomicalInfo.getMoonPhase().toString());
                moonInfo.put("illumination", astronomicalInfo.getMoonIllumination());
                moonInfo.put("age", astronomicalInfo.getMoonAge());
                moonInfo.put("distance", astronomicalInfo.getMoonDistance());
                response.put("moon", moonInfo);
            }

            // その他の情報
            response.put("solar_position", Map.of(
                "azimuth", astronomicalInfo.getSolarAzimuth(),
                "altitude", astronomicalInfo.getSolarAltitude()
            ));

            logger.info("Retrieved astronomical info for {},{} on {}", latitude, longitude, dateStr);
            return ToolResponse.of(response);

        } catch (DateTimeParseException e) {
            throw ToolExecutionException.invalidParameter("date", "Invalid format: " + dateStr);
        } catch (Exception e) {
            logger.error("Failed to get astronomical info", e);
            throw new ToolExecutionException("Failed to get astronomical info: " + e.getMessage(), e);
        }
    }

    private Double getDoubleParameter(Map<String, Object> parameters, String key) {
        Object value = parameters.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}