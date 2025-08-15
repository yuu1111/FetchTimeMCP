package com.github.yuu1111.tools.impl;

import com.github.yuu1111.protocol.MCPError;
import com.github.yuu1111.tools.MCPTool;
import com.github.yuu1111.tools.ToolExecutionException;
import com.github.yuu1111.tools.ToolResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 現在時刻取得ツール
 * 指定されたタイムゾーンの現在時刻を取得
 */
public class GetCurrentTime implements MCPTool {
    private static final Logger logger = LoggerFactory.getLogger(GetCurrentTime.class);
    
    // サポートされている出力フォーマット
    public enum OutputFormat {
        ISO8601("ISO-8601", DateTimeFormatter.ISO_OFFSET_DATE_TIME),
        RFC3339("RFC-3339", DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX")),
        UNIX("Unix Timestamp", null),
        HUMAN("Human Readable", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z")),
        CUSTOM("Custom Format", null);
        
        private final String description;
        private final DateTimeFormatter formatter;
        
        OutputFormat(String description, DateTimeFormatter formatter) {
            this.description = description;
            this.formatter = formatter;
        }
    }
    
    @Override
    public String getName() {
        return "get_current_time";
    }
    
    @Override
    public String getDescription() {
        return "Get current time in specified timezone with various format options";
    }
    
    @Override
    public Map<String, Object> getParameterSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "timezone", Map.of(
                    "type", "string",
                    "description", "IANA timezone name (e.g., Asia/Tokyo, America/New_York)",
                    "default", "UTC"
                ),
                "format", Map.of(
                    "type", "string",
                    "enum", new String[]{"ISO8601", "RFC3339", "UNIX", "HUMAN", "CUSTOM"},
                    "description", "Output format for the timestamp",
                    "default", "ISO8601"
                ),
                "custom_format", Map.of(
                    "type", "string",
                    "description", "Custom date format pattern (when format=CUSTOM)"
                ),
                "include_dst", Map.of(
                    "type", "boolean",
                    "description", "Include DST information",
                    "default", false
                ),
                "include_offset", Map.of(
                    "type", "boolean",
                    "description", "Include UTC offset information",
                    "default", true
                ),
                "include_zone_info", Map.of(
                    "type", "boolean",
                    "description", "Include detailed timezone information",
                    "default", false
                )
            ),
            "required", new String[]{}
        );
    }
    
    @Override
    public ToolResponse execute(Map<String, Object> parameters) throws ToolExecutionException {
        logger.debug("Executing get_current_time with parameters: {}", parameters);
        
        try {
            // パラメータを取得
            String timezoneStr = getParameter(parameters, "timezone", "UTC");
            String formatStr = getParameter(parameters, "format", "ISO8601");
            String customFormat = getParameter(parameters, "custom_format", null);
            boolean includeDst = getParameter(parameters, "include_dst", false);
            boolean includeOffset = getParameter(parameters, "include_offset", true);
            boolean includeZoneInfo = getParameter(parameters, "include_zone_info", false);
            
            // タイムゾーンを解析
            ZoneId zoneId = parseTimezone(timezoneStr);
            
            // 現在時刻を取得
            ZonedDateTime now = ZonedDateTime.now(zoneId);
            Instant instant = now.toInstant();
            
            // レスポンスを構築
            ToolResponse.Builder builder = ToolResponse.builder();
            
            // 基本情報
            builder.withData("timestamp", formatTimestamp(now, formatStr, customFormat));
            builder.withData("timezone", zoneId.getId());
            builder.withData("unix_timestamp", instant.getEpochSecond());
            builder.withData("unix_timestamp_millis", instant.toEpochMilli());
            
            // オフセット情報
            if (includeOffset) {
                ZoneOffset offset = now.getOffset();
                builder.withData("utc_offset", offset.toString());
                builder.withData("utc_offset_seconds", offset.getTotalSeconds());
            }
            
            // DST情報
            if (includeDst) {
                Map<String, Object> dstInfo = getDSTInfo(zoneId, now);
                builder.withData("dst_info", dstInfo);
            }
            
            // 詳細なタイムゾーン情報
            if (includeZoneInfo) {
                Map<String, Object> zoneInfo = getDetailedZoneInfo(zoneId, now);
                builder.withData("zone_info", zoneInfo);
            }
            
            // 日付コンポーネント
            builder.withData("date_components", Map.of(
                "year", now.getYear(),
                "month", now.getMonthValue(),
                "day", now.getDayOfMonth(),
                "hour", now.getHour(),
                "minute", now.getMinute(),
                "second", now.getSecond(),
                "nano", now.getNano(),
                "day_of_week", now.getDayOfWeek().toString(),
                "day_of_year", now.getDayOfYear()
            ));
            
            // メタデータ
            builder.withMetadata("execution_time", System.currentTimeMillis());
            builder.withMetadata("timezone_valid", true);
            
            return builder.build();
            
        } catch (DateTimeParseException e) {
            throw ToolExecutionException.invalidParameter("custom_format", e.getMessage());
        } catch (Exception e) {
            logger.error("Error executing get_current_time", e);
            throw new ToolExecutionException("Failed to get current time: " + e.getMessage(), e);
        }
    }
    
    /**
     * タイムゾーンを解析
     */
    private ZoneId parseTimezone(String timezone) throws ToolExecutionException {
        try {
            // 一般的な略称をIANA形式に変換
            String normalized = normalizeTimezone(timezone);
            return ZoneId.of(normalized);
        } catch (DateTimeException e) {
            // 利用可能なタイムゾーンを提案
            String suggestions = findSimilarTimezones(timezone);
            throw new ToolExecutionException(
                MCPError.TIMEZONE_ERROR,
                "Invalid timezone: " + timezone,
                Map.of("suggestions", suggestions)
            );
        }
    }
    
    /**
     * タイムゾーン略称を正規化
     */
    private String normalizeTimezone(String timezone) {
        // よく使われる略称のマッピング
        Map<String, String> abbreviations = Map.of(
            "JST", "Asia/Tokyo",
            "EST", "America/New_York",
            "PST", "America/Los_Angeles",
            "GMT", "Europe/London",
            "CET", "Europe/Paris",
            "CST", "America/Chicago",
            "MST", "America/Denver",
            "AEST", "Australia/Sydney",
            "IST", "Asia/Kolkata"
        );
        
        String upper = timezone.toUpperCase();
        return abbreviations.getOrDefault(upper, timezone);
    }
    
    /**
     * 類似のタイムゾーンを検索
     */
    private String findSimilarTimezones(String input) {
        Set<String> availableZones = ZoneId.getAvailableZoneIds();
        String lower = input.toLowerCase();
        
        return availableZones.stream()
            .filter(zone -> zone.toLowerCase().contains(lower))
            .limit(5)
            .reduce((a, b) -> a + ", " + b)
            .orElse("No similar timezones found");
    }
    
    /**
     * タイムスタンプをフォーマット
     */
    private String formatTimestamp(ZonedDateTime dateTime, String format, String customFormat) 
            throws ToolExecutionException {
        try {
            OutputFormat outputFormat = OutputFormat.valueOf(format.toUpperCase());
            
            return switch (outputFormat) {
                case ISO8601 -> dateTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                case RFC3339 -> dateTime.format(outputFormat.formatter);
                case UNIX -> String.valueOf(dateTime.toInstant().getEpochSecond());
                case HUMAN -> dateTime.format(outputFormat.formatter);
                case CUSTOM -> {
                    if (customFormat == null || customFormat.isBlank()) {
                        throw new ToolExecutionException("custom_format is required when format=CUSTOM");
                    }
                    yield dateTime.format(DateTimeFormatter.ofPattern(customFormat));
                }
            };
        } catch (IllegalArgumentException e) {
            throw new ToolExecutionException("Invalid format: " + format);
        } catch (DateTimeException e) {
            throw new ToolExecutionException("Invalid custom format pattern: " + customFormat);
        }
    }
    
    /**
     * DST情報を取得
     */
    private Map<String, Object> getDSTInfo(ZoneId zoneId, ZonedDateTime dateTime) {
        Map<String, Object> dstInfo = new HashMap<>();
        
        try {
            var rules = zoneId.getRules();
            boolean isDst = rules.isDaylightSavings(dateTime.toInstant());
            Duration dstOffset = rules.getDaylightSavings(dateTime.toInstant());
            
            dstInfo.put("is_dst", isDst);
            dstInfo.put("dst_offset_seconds", dstOffset.getSeconds());
            dstInfo.put("dst_offset", dstOffset.toString());
            
            // 次のDST切り替え日時
            var nextTransition = rules.nextTransition(dateTime.toInstant());
            if (nextTransition != null) {
                dstInfo.put("next_transition", nextTransition.getInstant().toString());
                dstInfo.put("next_transition_type", 
                    nextTransition.isGap() ? "SPRING_FORWARD" : "FALL_BACK");
            }
            
        } catch (Exception e) {
            logger.warn("Failed to get DST info for timezone: {}", zoneId, e);
            dstInfo.put("error", "DST information not available");
        }
        
        return dstInfo;
    }
    
    /**
     * 詳細なタイムゾーン情報を取得
     */
    private Map<String, Object> getDetailedZoneInfo(ZoneId zoneId, ZonedDateTime dateTime) {
        Map<String, Object> info = new HashMap<>();
        
        try {
            var rules = zoneId.getRules();
            
            info.put("zone_id", zoneId.getId());
            info.put("zone_type", zoneId.getClass().getSimpleName());
            info.put("display_name", zoneId.getDisplayName(
                java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH));
            info.put("standard_offset", rules.getStandardOffset(dateTime.toInstant()).toString());
            info.put("has_fixed_offset", rules.isFixedOffset());
            
            // 履歴情報
            var transitions = rules.getTransitions();
            if (!transitions.isEmpty()) {
                info.put("total_transitions", transitions.size());
                var lastTransition = transitions.get(transitions.size() - 1);
                info.put("last_transition", lastTransition.getInstant().toString());
            }
            
        } catch (Exception e) {
            logger.warn("Failed to get detailed zone info for: {}", zoneId, e);
            info.put("error", "Detailed zone information not available");
        }
        
        return info;
    }
    
    /**
     * パラメータを取得（型安全）
     */
    @SuppressWarnings("unchecked")
    private <T> T getParameter(Map<String, Object> parameters, String key, T defaultValue) {
        Object value = parameters.get(key);
        if (value == null) {
            return defaultValue;
        }
        
        try {
            return (T) value;
        } catch (ClassCastException e) {
            logger.warn("Invalid type for parameter '{}': expected {}, got {}", 
                key, defaultValue.getClass().getSimpleName(), value.getClass().getSimpleName());
            return defaultValue;
        }
    }
    
    @Override
    public MCPError validateParameters(Map<String, Object> parameters) {
        // タイムゾーンの検証
        Object timezone = parameters.get("timezone");
        if (timezone != null && !(timezone instanceof String)) {
            return MCPError.invalidParams("timezone must be a string");
        }
        
        // フォーマットの検証
        Object format = parameters.get("format");
        if (format != null) {
            if (!(format instanceof String)) {
                return MCPError.invalidParams("format must be a string");
            }
            String formatStr = (String) format;
            try {
                OutputFormat.valueOf(formatStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                return MCPError.invalidParams("Invalid format: " + formatStr);
            }
        }
        
        return null;
    }
    
    @Override
    public boolean isCacheable() {
        return false; // 現在時刻はキャッシュ不可
    }
}