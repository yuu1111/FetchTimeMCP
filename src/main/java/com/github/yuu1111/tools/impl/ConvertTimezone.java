package com.github.yuu1111.tools.impl;

import com.github.yuu1111.tools.MCPTool;
import com.github.yuu1111.tools.ToolExecutionException;
import com.github.yuu1111.tools.ToolResponse;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * タイムゾーン変換ツール
 * 異なるタイムゾーン間で時刻を変換
 */
public class ConvertTimezone implements MCPTool {
    private static final Logger logger = LoggerFactory.getLogger(ConvertTimezone.class);

    private static final DateTimeFormatter DEFAULT_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private static final Set<String> COMMON_FORMATS = Set.of(
        "yyyy-MM-dd HH:mm:ss",
        "yyyy-MM-dd'T'HH:mm:ss",
        "yyyy-MM-dd'T'HH:mm:ssXXX",
        "yyyy-MM-dd'T'HH:mm:ss.SSS",
        "yyyy/MM/dd HH:mm:ss",
        "dd/MM/yyyy HH:mm:ss",
        "MM/dd/yyyy HH:mm:ss"
    );

    @Override
    public String getName() {
        return "convert_timezone";
    }

    @Override
    public String getDescription() {
        return "Convert datetime between different timezones with DST support";
    }

    @Override
    public Map<String, Object> getParameterSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "datetime", Map.of(
                    "type", "string",
                    "description", "DateTime to convert (ISO format or common formats)"
                ),
                "from_timezone", Map.of(
                    "type", "string",
                    "description", "Source timezone (IANA format)"
                ),
                "to_timezone", Map.of(
                    "type", "string",
                    "description", "Target timezone (IANA format) or array for multiple"
                ),
                "to_timezones", Map.of(
                    "type", "array",
                    "items", Map.of("type", "string"),
                    "description", "Multiple target timezones for batch conversion"
                ),
                "format", Map.of(
                    "type", "string",
                    "description", "Output format pattern",
                    "default", "ISO8601"
                ),
                "include_dst_info", Map.of(
                    "type", "boolean",
                    "description", "Include DST information",
                    "default", false
                ),
                "include_time_difference", Map.of(
                    "type", "boolean",
                    "description", "Include time difference calculation",
                    "default", true
                ),
                "relative_time", Map.of(
                    "type", "object",
                    "description", "Convert relative time (e.g., '3 hours from now')",
                    "properties", Map.of(
                        "amount", Map.of("type", "integer"),
                        "unit", Map.of(
                            "type", "string",
                            "enum", new String[]{"MINUTES", "HOURS", "DAYS", "WEEKS", "MONTHS"}
                        )
                    )
                )
            ),
            "required", new String[]{}
        );
    }

    @Override
    public ToolResponse execute(Map<String, Object> parameters) throws ToolExecutionException {
        logger.debug("Executing convert_timezone with parameters: {}", parameters);

        try {
            // パラメータ取得
            String datetimeStr = getParameter(parameters, "datetime", null);
            String fromTimezone = getParameter(parameters, "from_timezone", "UTC");
            String toTimezone = getParameter(parameters, "to_timezone", null);
            List<String> toTimezones = getListParameter(parameters, "to_timezones");
            String outputFormat = getParameter(parameters, "format", "ISO8601");
            boolean includeDstInfo = getParameter(parameters, "include_dst_info", false);
            boolean includeTimeDiff = getParameter(parameters, "include_time_difference", true);
            Map<String, Object> relativeTime = getMapParameter(parameters, "relative_time");

            // 変換対象の日時を決定
            ZonedDateTime sourceDateTime = determineSourceDateTime(
                datetimeStr, fromTimezone, relativeTime
            );

            // 変換先タイムゾーンのリストを作成
            List<String> targetZones = determineTargetZones(toTimezone, toTimezones);
            if (targetZones.isEmpty()) {
                throw new ToolExecutionException("No target timezone specified");
            }

            // レスポンスビルダー
            ToolResponse.Builder builder = ToolResponse.builder();

            // 元の日時情報
            builder.withData("source", Map.of(
                "datetime", formatDateTime(sourceDateTime, outputFormat),
                "timezone", sourceDateTime.getZone().getId(),
                "unix_timestamp", sourceDateTime.toInstant().getEpochSecond(),
                "offset", sourceDateTime.getOffset().toString()
            ));

            // 単一タイムゾーン変換
            if (targetZones.size() == 1) {
                String targetZone = targetZones.get(0);
                Map<String, Object> result = convertToTimezone(
                    sourceDateTime, targetZone, outputFormat, includeDstInfo, includeTimeDiff
                );
                builder.withData("target", result);
            }
            // 複数タイムゾーン変換
            else {
                List<Map<String, Object>> results = targetZones.stream()
                    .map(tz -> {
                        try {
                            return convertToTimezone(
                                sourceDateTime, tz, outputFormat, includeDstInfo, includeTimeDiff
                            );
                        } catch (Exception e) {
                            logger.warn("Failed to convert to timezone: {}", tz, e);
                            return Map.<String, Object>of(
                                "timezone", tz,
                                "error", e.getMessage()
                            );
                        }
                    })
                    .collect(Collectors.toList());

                builder.withData("targets", results);
            }

            // 追加情報
            if (includeTimeDiff && targetZones.size() > 1) {
                builder.withData("time_matrix", createTimeMatrix(sourceDateTime, targetZones));
            }

            // メタデータ
            builder.withMetadata("conversion_count", targetZones.size());
            builder.withMetadata("execution_time", System.currentTimeMillis());

            return builder.build();

        } catch (DateTimeParseException e) {
            throw new ToolExecutionException("Invalid datetime format: " + e.getMessage());
        } catch (DateTimeException e) {
            throw ToolExecutionException.invalidTimezone(e.getMessage());
        } catch (Exception e) {
            logger.error("Error executing convert_timezone", e);
            throw new ToolExecutionException("Conversion failed: " + e.getMessage(), e);
        }
    }

    /**
     * ソース日時を決定
     */
    private ZonedDateTime determineSourceDateTime(
            String datetimeStr, String fromTimezone, Map<String, Object> relativeTime)
            throws ToolExecutionException {

        ZoneId fromZone = parseTimezone(fromTimezone);

        // 相対時間が指定されている場合
        if (relativeTime != null && !relativeTime.isEmpty()) {
            return calculateRelativeTime(fromZone, relativeTime);
        }

        // 日時文字列が指定されていない場合は現在時刻
        if (datetimeStr == null || datetimeStr.isBlank()) {
            return ZonedDateTime.now(fromZone);
        }

        // 日時文字列をパース
        return parseDateTime(datetimeStr, fromZone);
    }

    /**
     * 相対時間を計算
     */
    private ZonedDateTime calculateRelativeTime(ZoneId zone, Map<String, Object> relativeTime) {
        ZonedDateTime now = ZonedDateTime.now(zone);

        Integer amount = (Integer) relativeTime.get("amount");
        String unit = (String) relativeTime.get("unit");

        if (amount == null || unit == null) {
            return now;
        }

        return switch (unit.toUpperCase()) {
            case "MINUTES" -> now.plusMinutes(amount);
            case "HOURS" -> now.plusHours(amount);
            case "DAYS" -> now.plusDays(amount);
            case "WEEKS" -> now.plusWeeks(amount);
            case "MONTHS" -> now.plusMonths(amount);
            default -> now;
        };
    }

    /**
     * 日時文字列をパース
     */
    private ZonedDateTime parseDateTime(String datetimeStr, ZoneId zone)
            throws ToolExecutionException {

        // ISO形式を試す
        try {
            if (datetimeStr.contains("T") && (datetimeStr.contains("+") || datetimeStr.contains("Z"))) {
                return ZonedDateTime.parse(datetimeStr);
            }
        } catch (DateTimeParseException ignored) {}

        // タイムゾーン情報なしのISO形式
        try {
            if (datetimeStr.contains("T")) {
                LocalDateTime ldt = LocalDateTime.parse(datetimeStr);
                return ldt.atZone(zone);
            }
        } catch (DateTimeParseException ignored) {}

        // 一般的なフォーマットを試す
        for (String pattern : COMMON_FORMATS) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
                LocalDateTime ldt = LocalDateTime.parse(datetimeStr, formatter);
                return ldt.atZone(zone);
            } catch (DateTimeParseException ignored) {}
        }

        // Unix timestampを試す
        try {
            long timestamp = Long.parseLong(datetimeStr);
            // 10桁ならば秒、13桁ならばミリ秒と判断
            if (datetimeStr.length() == 10) {
                return ZonedDateTime.ofInstant(Instant.ofEpochSecond(timestamp), zone);
            } else if (datetimeStr.length() == 13) {
                return ZonedDateTime.ofInstant(Instant.ofEpochMilli(timestamp), zone);
            }
        } catch (NumberFormatException ignored) {}

        throw new ToolExecutionException("Unable to parse datetime: " + datetimeStr);
    }

    /**
     * ターゲットタイムゾーンのリストを決定
     */
    private List<String> determineTargetZones(String toTimezone, List<String> toTimezones) {
        List<String> zones = new ArrayList<>();

        if (toTimezone != null && !toTimezone.isBlank()) {
            zones.add(toTimezone);
        }

        if (toTimezones != null && !toTimezones.isEmpty()) {
            zones.addAll(toTimezones);
        }

        // 重複を除去
        return zones.stream().distinct().collect(Collectors.toList());
    }

    /**
     * タイムゾーンに変換
     */
    private Map<String, Object> convertToTimezone(
            ZonedDateTime sourceDateTime,
            String targetTimezone,
            String outputFormat,
            boolean includeDstInfo,
            boolean includeTimeDiff) throws ToolExecutionException {

        ZoneId targetZone = parseTimezone(targetTimezone);
        ZonedDateTime targetDateTime = sourceDateTime.withZoneSameInstant(targetZone);

        Map<String, Object> result = new HashMap<>();
        result.put("datetime", formatDateTime(targetDateTime, outputFormat));
        result.put("timezone", targetZone.getId());
        result.put("unix_timestamp", targetDateTime.toInstant().getEpochSecond());
        result.put("offset", targetDateTime.getOffset().toString());

        // 時差情報
        if (includeTimeDiff) {
            Duration diff = Duration.between(
                sourceDateTime.toOffsetDateTime(),
                targetDateTime.toOffsetDateTime()
            );

            int offsetDiff = targetDateTime.getOffset().getTotalSeconds() -
                           sourceDateTime.getOffset().getTotalSeconds();

            result.put("time_difference", Map.of(
                "offset_difference_seconds", offsetDiff,
                "offset_difference_hours", offsetDiff / 3600.0,
                "offset_difference", formatDuration(offsetDiff),
                "same_instant", true
            ));
        }

        // DST情報
        if (includeDstInfo) {
            result.put("dst_info", getDSTInfo(targetZone, targetDateTime));
        }

        // 日付コンポーネント
        result.put("components", Map.of(
            "date", targetDateTime.toLocalDate().toString(),
            "time", targetDateTime.toLocalTime().toString(),
            "day_of_week", targetDateTime.getDayOfWeek().toString()
        ));

        return result;
    }

    /**
     * 時差マトリックスを作成
     */
    private Map<String, Map<String, String>> createTimeMatrix(
            ZonedDateTime baseTime, List<String> timezones) {

        Map<String, Map<String, String>> matrix = new HashMap<>();

        for (String tz1 : timezones) {
            Map<String, String> row = new HashMap<>();
            ZoneId zone1 = ZoneId.of(tz1);
            ZonedDateTime time1 = baseTime.withZoneSameInstant(zone1);

            for (String tz2 : timezones) {
                if (tz1.equals(tz2)) {
                    row.put(tz2, "0h");
                } else {
                    ZoneId zone2 = ZoneId.of(tz2);
                    ZonedDateTime time2 = baseTime.withZoneSameInstant(zone2);

                    int offsetDiff = time2.getOffset().getTotalSeconds() -
                                   time1.getOffset().getTotalSeconds();
                    row.put(tz2, formatDuration(offsetDiff));
                }
            }

            matrix.put(tz1, row);
        }

        return matrix;
    }

    /**
     * 時間差をフォーマット
     */
    private String formatDuration(int seconds) {
        int hours = seconds / 3600;
        int minutes = (Math.abs(seconds) % 3600) / 60;

        if (minutes == 0) {
            return String.format("%+dh", hours);
        } else {
            return String.format("%+dh%02dm", hours, minutes);
        }
    }

    /**
     * 日時をフォーマット
     */
    private String formatDateTime(ZonedDateTime dateTime, String format) {
        if ("ISO8601".equalsIgnoreCase(format)) {
            return dateTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        } else if ("UNIX".equalsIgnoreCase(format)) {
            return String.valueOf(dateTime.toInstant().getEpochSecond());
        } else {
            try {
                return dateTime.format(DateTimeFormatter.ofPattern(format));
            } catch (Exception e) {
                return dateTime.format(DEFAULT_FORMATTER);
            }
        }
    }

    /**
     * DST情報を取得
     */
    private Map<String, Object> getDSTInfo(ZoneId zoneId, ZonedDateTime dateTime) {
        Map<String, Object> info = new HashMap<>();

        try {
            var rules = zoneId.getRules();
            boolean isDst = rules.isDaylightSavings(dateTime.toInstant());
            info.put("is_dst", isDst);

            if (isDst) {
                Duration dstOffset = rules.getDaylightSavings(dateTime.toInstant());
                info.put("dst_offset", dstOffset.toString());
            }
        } catch (Exception e) {
            info.put("available", false);
        }

        return info;
    }

    /**
     * タイムゾーンをパース
     */
    private ZoneId parseTimezone(String timezone) throws ToolExecutionException {
        try {
            return ZoneId.of(timezone);
        } catch (Exception e) {
            throw ToolExecutionException.invalidTimezone(timezone);
        }
    }

    /**
     * パラメータ取得（型安全）
     */
    @SuppressWarnings("unchecked")
    private <T> T getParameter(Map<String, Object> parameters, String key, T defaultValue) {
        Object value = parameters.get(key);
        return value != null ? (T) value : defaultValue;
    }

    /**
     * リストパラメータ取得
     */
    @SuppressWarnings("unchecked")
    private List<String> getListParameter(Map<String, Object> parameters, String key) {
        Object value = parameters.get(key);
        if (value instanceof List) {
            return (List<String>) value;
        }
        return new ArrayList<>();
    }

    /**
     * マップパラメータ取得
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> getMapParameter(Map<String, Object> parameters, String key) {
        Object value = parameters.get(key);
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        return null;
    }

    @Override
    public boolean isCacheable() {
        return true; // 同じ変換はキャッシュ可能
    }

    @Override
    public int getCacheTTL() {
        return 300; // 5分間キャッシュ
    }
}