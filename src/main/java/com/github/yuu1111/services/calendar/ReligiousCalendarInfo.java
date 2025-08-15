package com.github.yuu1111.services.calendar;

import java.util.HashMap;
import java.util.Map;

/**
 * 宗教暦情報を保持するクラス
 */
public class ReligiousCalendarInfo {
    
    private String calendarType;
    private String calendarName;
    private int year;
    private int month;
    private int day;
    private String era;
    private String monthName;
    private String weekDay;
    private boolean leapYear;
    private Map<String, String> holidays = new HashMap<>();
    private Map<String, String> observances = new HashMap<>();
    
    // Getters and Setters
    
    public String getCalendarType() {
        return calendarType;
    }
    
    public void setCalendarType(String calendarType) {
        this.calendarType = calendarType;
    }
    
    public String getCalendarName() {
        return calendarName;
    }
    
    public void setCalendarName(String calendarName) {
        this.calendarName = calendarName;
    }
    
    public int getYear() {
        return year;
    }
    
    public void setYear(int year) {
        this.year = year;
    }
    
    public int getMonth() {
        return month;
    }
    
    public void setMonth(int month) {
        this.month = month;
    }
    
    public int getDay() {
        return day;
    }
    
    public void setDay(int day) {
        this.day = day;
    }
    
    public String getEra() {
        return era;
    }
    
    public void setEra(String era) {
        this.era = era;
    }
    
    public String getMonthName() {
        return monthName;
    }
    
    public void setMonthName(String monthName) {
        this.monthName = monthName;
    }
    
    public String getWeekDay() {
        return weekDay;
    }
    
    public void setWeekDay(String weekDay) {
        this.weekDay = weekDay;
    }
    
    public boolean isLeapYear() {
        return leapYear;
    }
    
    public void setLeapYear(boolean leapYear) {
        this.leapYear = leapYear;
    }
    
    public Map<String, String> getHolidays() {
        return holidays;
    }
    
    public void setHolidays(Map<String, String> holidays) {
        this.holidays = holidays;
    }
    
    public Map<String, String> getObservances() {
        return observances;
    }
    
    public void setObservances(Map<String, String> observances) {
        this.observances = observances;
    }
    
    /**
     * Map形式で日付情報を返す
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("year", year);
        map.put("month", month);
        map.put("day", day);
        map.put("month_name", monthName);
        map.put("formatted", formatDate());
        return map;
    }
    
    /**
     * フォーマットされた日付文字列を返す
     */
    public String formatDate() {
        StringBuilder sb = new StringBuilder();
        
        if (era != null && !era.isEmpty()) {
            sb.append(era).append(" ");
        }
        
        sb.append(year).append("/");
        
        if (monthName != null && !monthName.isEmpty()) {
            sb.append(monthName);
        } else {
            sb.append(month);
        }
        
        sb.append("/").append(day);
        
        return sb.toString();
    }
    
    @Override
    public String toString() {
        return "ReligiousCalendarInfo{" +
                "calendarType='" + calendarType + '\'' +
                ", date=" + formatDate() +
                ", weekDay='" + weekDay + '\'' +
                ", leapYear=" + leapYear +
                ", holidays=" + holidays.size() +
                ", observances=" + observances.size() +
                '}';
    }
}