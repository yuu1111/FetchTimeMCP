package com.github.yuu1111.tools.impl;

import com.github.yuu1111.protocol.MCPError;
import com.github.yuu1111.services.calendar.CalendarService;
import com.github.yuu1111.tools.MCPTool;
import com.github.yuu1111.tools.ToolResponse;
import com.github.yuu1111.tools.ToolExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

/**
 * 宗教的カレンダー変換ツール
 * グレゴリオ暦を各種宗教暦に変換
 */
public class GetReligiousCalendar implements MCPTool {
    
    private static final Logger logger = LoggerFactory.getLogger(GetReligiousCalendar.class);
    private final CalendarService calendarService;
    
    public GetReligiousCalendar() {
        this.calendarService = new CalendarService();
    }
    
    @Override
    public String getName() {
        return "get_religious_calendar";
    }
    
    @Override
    public String getDescription() {
        return "Convert Gregorian date to various religious calendars";
    }
    
    @Override
    public Map<String, Object> getParameterSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "date", Map.of(
                    "type", "string",
                    "description", "Date in ISO 8601 format (YYYY-MM-DD)",
                    "example", "2024-01-15"
                ),
                "calendar_type", Map.of(
                    "type", "string",
                    "description", "Type of religious calendar",
                    "enum", new String[]{"islamic", "hebrew", "buddhist", "hindu", "chinese", "japanese"},
                    "example", "islamic"
                ),
                "include_holidays", Map.of(
                    "type", "boolean",
                    "description", "Include religious holidays and observances",
                    "default", true
                )
            ),
            "required", new String[]{"date", "calendar_type"}
        );
    }
    
    @Override
    public ToolResponse execute(Map<String, Object> parameters) throws ToolExecutionException {
        // パラメータ検証
        String dateStr = (String) parameters.get("date");
        if (dateStr == null || dateStr.isEmpty()) {
            throw ToolExecutionException.invalidParameter("date", "parameter is required");
        }
        
        String calendarType = (String) parameters.get("calendar_type");
        if (calendarType == null || calendarType.isEmpty()) {
            throw ToolExecutionException.invalidParameter("calendar_type", "parameter is required");
        }
        
        Boolean includeHolidays = (Boolean) parameters.getOrDefault("include_holidays", true);
        
        try {
            // 日付をパース
            LocalDate date = LocalDate.parse(dateStr);
            
            // カレンダータイプを検証
            CalendarService.CalendarType type;
            try {
                type = CalendarService.CalendarType.valueOf(calendarType.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw ToolExecutionException.invalidParameter("calendar_type", "Invalid type: " + calendarType);
            }
            
            // 宗教暦に変換
            var calendarInfo = calendarService.convertToReligiousCalendar(date, type);
            
            // レスポンスを構築
            Map<String, Object> response = new HashMap<>();
            response.put("gregorian_date", dateStr);
            response.put("calendar_type", calendarType);
            response.put("converted_date", calendarInfo.toMap());
            
            if (includeHolidays) {
                response.put("holidays", calendarInfo.getHolidays());
                response.put("observances", calendarInfo.getObservances());
            }
            
            response.put("metadata", Map.of(
                "calendar_name", calendarInfo.getCalendarName(),
                "era", calendarInfo.getEra(),
                "week_day", calendarInfo.getWeekDay(),
                "is_leap_year", calendarInfo.isLeapYear()
            ));
            
            logger.info("Converted {} to {} calendar", dateStr, calendarType);
            return ToolResponse.of(response);
            
        } catch (DateTimeParseException e) {
            throw ToolExecutionException.invalidParameter("date", "Invalid format: " + dateStr);
        } catch (Exception e) {
            logger.error("Failed to convert calendar", e);
            throw new ToolExecutionException("Failed to convert calendar: " + e.getMessage(), e);
        }
    }
}