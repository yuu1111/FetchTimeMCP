package com.github.yuu1111.services.astronomy;

import org.shredzone.commons.suncalc.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.time.Duration;

/**
 * 天文学的情報サービス
 * 日の出・日の入り、月相などの天文情報を計算
 */
public class AstronomyService {
    
    private static final Logger logger = LoggerFactory.getLogger(AstronomyService.class);
    
    /**
     * 指定された位置と日付の天文情報を取得
     */
    public AstronomicalInfo getAstronomicalInfo(double latitude, double longitude, LocalDate date) {
        logger.debug("Calculating astronomical info for lat:{}, lon:{}, date:{}", 
                    latitude, longitude, date);
        
        AstronomicalInfo info = new AstronomicalInfo();
        
        // 太陽情報の計算
        calculateSunInfo(info, latitude, longitude, date);
        
        // 月情報の計算
        calculateMoonInfo(info, latitude, longitude, date);
        
        // 太陽位置の計算
        calculateSolarPosition(info, latitude, longitude, date);
        
        return info;
    }
    
    /**
     * 太陽情報を計算
     */
    private void calculateSunInfo(AstronomicalInfo info, double latitude, double longitude, LocalDate date) {
        try {
            // 日の出・日の入り計算
            SunTimes sunTimes = SunTimes.compute()
                .on(date)
                .at(latitude, longitude)
                .execute();
            
            // 基本的な太陽時刻
            if (sunTimes.getRise() != null) {
                info.setSunrise(sunTimes.getRise().toString());
            }
            if (sunTimes.getSet() != null) {
                info.setSunset(sunTimes.getSet().toString());
            }
            if (sunTimes.getNoon() != null) {
                info.setSolarNoon(sunTimes.getNoon().toString());
            }
            
            // 日長計算
            if (sunTimes.getRise() != null && sunTimes.getSet() != null) {
                Duration dayLength = Duration.between(sunTimes.getRise(), sunTimes.getSet());
                info.setDayLength(formatDuration(dayLength));
            }
            
            // トワイライト計算
            calculateTwilight(info, latitude, longitude, date);
            
        } catch (Exception e) {
            logger.error("Failed to calculate sun info", e);
            // 極地や特殊な条件での例外処理
            info.setSunrise("N/A");
            info.setSunset("N/A");
            info.setSolarNoon("N/A");
            info.setDayLength("N/A");
        }
    }
    
    /**
     * トワイライト（薄明）時刻を計算
     */
    private void calculateTwilight(AstronomicalInfo info, double latitude, double longitude, LocalDate date) {
        try {
            // 市民薄明
            SunTimes civilTwilight = SunTimes.compute()
                .on(date)
                .at(latitude, longitude)
                .twilight(SunTimes.Twilight.CIVIL)
                .execute();
            
            if (civilTwilight.getRise() != null) {
                info.setCivilDawn(civilTwilight.getRise().toString());
            }
            if (civilTwilight.getSet() != null) {
                info.setCivilDusk(civilTwilight.getSet().toString());
            }
            
            // 航海薄明
            SunTimes nauticalTwilight = SunTimes.compute()
                .on(date)
                .at(latitude, longitude)
                .twilight(SunTimes.Twilight.NAUTICAL)
                .execute();
            
            if (nauticalTwilight.getRise() != null) {
                info.setNauticalDawn(nauticalTwilight.getRise().toString());
            }
            if (nauticalTwilight.getSet() != null) {
                info.setNauticalDusk(nauticalTwilight.getSet().toString());
            }
            
            // 天文薄明
            SunTimes astronomicalTwilight = SunTimes.compute()
                .on(date)
                .at(latitude, longitude)
                .twilight(SunTimes.Twilight.ASTRONOMICAL)
                .execute();
            
            if (astronomicalTwilight.getRise() != null) {
                info.setAstronomicalDawn(astronomicalTwilight.getRise().toString());
            }
            if (astronomicalTwilight.getSet() != null) {
                info.setAstronomicalDusk(astronomicalTwilight.getSet().toString());
            }
            
        } catch (Exception e) {
            logger.warn("Failed to calculate twilight times", e);
        }
    }
    
    /**
     * 月情報を計算
     */
    private void calculateMoonInfo(AstronomicalInfo info, double latitude, double longitude, LocalDate date) {
        try {
            // 月の出・月の入り
            MoonTimes moonTimes = MoonTimes.compute()
                .on(date)
                .at(latitude, longitude)
                .execute();
            
            if (moonTimes.getRise() != null) {
                info.setMoonrise(moonTimes.getRise().toString());
            }
            if (moonTimes.getSet() != null) {
                info.setMoonset(moonTimes.getSet().toString());
            }
            
            // 月相計算
            MoonPhase moonPhase = MoonPhase.compute()
                .on(date)
                .execute();
            
            // 月相の判定
            MoonPhaseType phaseType = calculateMoonPhaseType(moonPhase);
            info.setMoonPhase(phaseType);
            
            // 月の照度
            MoonIllumination moonIllumination = MoonIllumination.compute()
                .on(date)
                .execute();
            
            info.setMoonIllumination(moonIllumination.getFraction() * 100); // パーセンテージに変換
            
            // 月齢計算
            double moonAge = calculateMoonAge(date);
            info.setMoonAge(moonAge);
            
            // 月の距離
            MoonPosition moonPosition = MoonPosition.compute()
                .on(date.atStartOfDay(ZoneId.of("UTC")))
                .at(latitude, longitude)
                .execute();
            
            info.setMoonDistance(moonPosition.getDistance());
            
        } catch (Exception e) {
            logger.error("Failed to calculate moon info", e);
            info.setMoonrise("N/A");
            info.setMoonset("N/A");
            info.setMoonPhase(MoonPhaseType.UNKNOWN);
        }
    }
    
    /**
     * 月相タイプを判定
     */
    private MoonPhaseType calculateMoonPhaseType(MoonPhase moonPhase) {
        // 新月から次の新月までの周期での位置（0-1）
        ZonedDateTime nextNewMoon = moonPhase.getTime();
        
        // 簡易的な月相判定（実際はもっと正確な計算が必要）
        // ここでは照度ベースで判定
        MoonIllumination illumination = MoonIllumination.compute()
            .on(nextNewMoon.toLocalDate())
            .execute();
        
        double fraction = illumination.getFraction();
        double angle = illumination.getAngle();
        
        if (fraction < 0.02) {
            return MoonPhaseType.NEW_MOON;
        } else if (fraction < 0.35) {
            return angle > 0 ? MoonPhaseType.WAXING_CRESCENT : MoonPhaseType.WANING_CRESCENT;
        } else if (fraction < 0.65) {
            return angle > 0 ? MoonPhaseType.FIRST_QUARTER : MoonPhaseType.LAST_QUARTER;
        } else if (fraction < 0.98) {
            return angle > 0 ? MoonPhaseType.WAXING_GIBBOUS : MoonPhaseType.WANING_GIBBOUS;
        } else {
            return MoonPhaseType.FULL_MOON;
        }
    }
    
    /**
     * 月齢を計算（簡易版）
     */
    private double calculateMoonAge(LocalDate date) {
        // 基準となる新月（2000年1月6日）
        LocalDate referenceNewMoon = LocalDate.of(2000, 1, 6);
        
        // 月の周期（約29.53日）
        double lunarCycle = 29.530588;
        
        // 経過日数
        long daysSinceReference = java.time.temporal.ChronoUnit.DAYS.between(referenceNewMoon, date);
        
        // 月齢を計算
        double moonAge = daysSinceReference % lunarCycle;
        if (moonAge < 0) {
            moonAge += lunarCycle;
        }
        
        return Math.round(moonAge * 10.0) / 10.0; // 小数点1位まで
    }
    
    /**
     * 太陽位置を計算
     */
    private void calculateSolarPosition(AstronomicalInfo info, double latitude, double longitude, LocalDate date) {
        try {
            // 正午の太陽位置を計算
            LocalDateTime noon = date.atTime(12, 0);
            ZonedDateTime zonedNoon = noon.atZone(ZoneId.of("UTC"));
            
            SunPosition sunPosition = SunPosition.compute()
                .on(zonedNoon)
                .at(latitude, longitude)
                .execute();
            
            info.setSolarAzimuth(Math.round(sunPosition.getAzimuth() * 10.0) / 10.0);
            info.setSolarAltitude(Math.round(sunPosition.getAltitude() * 10.0) / 10.0);
            
        } catch (Exception e) {
            logger.error("Failed to calculate solar position", e);
            info.setSolarAzimuth(0.0);
            info.setSolarAltitude(0.0);
        }
    }
    
    /**
     * Duration を時間:分:秒の形式にフォーマット
     */
    private String formatDuration(Duration duration) {
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
}