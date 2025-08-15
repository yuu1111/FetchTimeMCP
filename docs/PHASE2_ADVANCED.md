# Phase 2: 高度な機能実装

## 📋 概要
サマータイム対応、宗教的カレンダー対応、天文学的情報などの高度な機能を実装するフェーズです。

**期間**: 5-7日  
**優先度**: 中

## 🎯 目標
- サマータイム（DST）完全対応
- 宗教的カレンダー変換機能
- 天文学的情報取得
- 祝日・記念日情報取得
- キャッシング機構の実装

## 📦 実装タスク

### 1. 依存関係追加
```xml
<!-- pom.xml に追加 -->
<dependencies>
    <!-- 国際化カレンダー -->
    <dependency>
        <groupId>com.ibm.icu</groupId>
        <artifactId>icu4j</artifactId>
        <version>74.2</version>
    </dependency>
    
    <!-- キャッシング -->
    <dependency>
        <groupId>com.github.ben-manes.caffeine</groupId>
        <artifactId>caffeine</artifactId>
        <version>3.1.8</version>
    </dependency>
    
    <!-- 天文計算 -->
    <dependency>
        <groupId>org.shredzone.commons</groupId>
        <artifactId>commons-suncalc</artifactId>
        <version>3.7</version>
    </dependency>
</dependencies>
```

### 2. サマータイム処理

#### 2.1 DSTManager.java
```java
package com.github.yuu1111.services;

import java.time.*;
import java.time.zone.ZoneRules;

public class DSTManager {
    
    public DSTInfo getDSTInfo(String timezone, LocalDateTime dateTime) {
        ZoneId zoneId = ZoneId.of(timezone);
        ZoneRules rules = zoneId.getRules();
        ZonedDateTime zdt = dateTime.atZone(zoneId);
        
        boolean isDST = rules.isDaylightSavings(zdt.toInstant());
        Duration dstOffset = rules.getDaylightSavings(zdt.toInstant());
        
        // 次のDST切り替え日時を計算
        ZoneOffsetTransition nextTransition = rules.nextTransition(zdt.toInstant());
        
        return DSTInfo.builder()
            .isDSTActive(isDST)
            .dstOffset(dstOffset)
            .nextTransition(nextTransition)
            .build();
    }
}
```

### 3. 宗教的カレンダー実装

#### 3.1 CalendarService.java
```java
package com.github.yuu1111.services;

import com.ibm.icu.util.*;
import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.SimpleDateFormat;

public class CalendarService {
    
    public ReligiousCalendarInfo convertToReligiousCalendar(
            LocalDate date, CalendarType type) {
        
        Calendar gregorian = new GregorianCalendar();
        gregorian.set(date.getYear(), date.getMonthValue() - 1, date.getDayOfMonth());
        
        Calendar targetCalendar = createCalendar(type);
        targetCalendar.setTime(gregorian.getTime());
        
        return extractCalendarInfo(targetCalendar, type);
    }
    
    private Calendar createCalendar(CalendarType type) {
        return switch (type) {
            case ISLAMIC -> new IslamicCalendar();
            case HEBREW -> new HebrewCalendar();
            case BUDDHIST -> new BuddhistCalendar();
            case HINDU -> new IndianCalendar();
            case CHINESE -> new ChineseCalendar();
            case JAPANESE -> new JapaneseCalendar();
            default -> new GregorianCalendar();
        };
    }
}
```

#### 3.2 各宗教暦の実装

##### IslamicCalendarConverter.java
```java
package com.github.yuu1111.services.calendar;

import com.ibm.icu.util.IslamicCalendar;
import com.ibm.icu.util.ULocale;

public class IslamicCalendarConverter {
    
    public IslamicDate convert(LocalDate gregorianDate) {
        IslamicCalendar islamic = new IslamicCalendar(ULocale.forLanguageTag("ar"));
        
        // グレゴリオ暦からイスラム暦への変換
        islamic.set(
            gregorianDate.getYear(),
            gregorianDate.getMonthValue() - 1,
            gregorianDate.getDayOfMonth()
        );
        
        int year = islamic.get(IslamicCalendar.YEAR);
        int month = islamic.get(IslamicCalendar.MONTH) + 1;
        int day = islamic.get(IslamicCalendar.DAY_OF_MONTH);
        
        // 月名の取得
        String monthName = getIslamicMonthName(month);
        
        // 重要な日付の確認
        IslamicHoliday holiday = checkIslamicHoliday(month, day);
        
        return new IslamicDate(year, month, day, monthName, holiday);
    }
    
    private String getIslamicMonthName(int month) {
        String[] months = {
            "Muharram", "Safar", "Rabi' al-awwal", "Rabi' al-thani",
            "Jumada al-awwal", "Jumada al-thani", "Rajab", "Sha'ban",
            "Ramadan", "Shawwal", "Dhu al-Qi'dah", "Dhu al-Hijjah"
        };
        return months[month - 1];
    }
}
```

##### HebrewCalendarConverter.java
```java
package com.github.yuu1111.services.calendar;

import com.ibm.icu.util.HebrewCalendar;

public class HebrewCalendarConverter {
    
    public HebrewDate convert(LocalDate gregorianDate) {
        HebrewCalendar hebrew = new HebrewCalendar();
        
        // 安息日の計算
        boolean isShabbat = isShabbat(gregorianDate);
        
        // ユダヤ暦の祝日確認
        HebrewHoliday holiday = checkHebrewHoliday(hebrew);
        
        return new HebrewDate(
            hebrew.get(HebrewCalendar.YEAR),
            hebrew.get(HebrewCalendar.MONTH),
            hebrew.get(HebrewCalendar.DAY_OF_MONTH),
            isShabbat,
            holiday
        );
    }
}
```

### 4. 天文学的情報

#### 4.1 AstronomyService.java
```java
package com.github.yuu1111.services;

import org.shredzone.commons.suncalc.SunTimes;
import org.shredzone.commons.suncalc.MoonPhase;
import org.shredzone.commons.suncalc.MoonTimes;

public class AstronomyService {
    
    public AstronomicalInfo getAstronomicalInfo(
            double latitude, double longitude, LocalDate date) {
        
        // 日の出・日の入り計算
        SunTimes sunTimes = SunTimes.compute()
            .on(date)
            .at(latitude, longitude)
            .execute();
            
        // 月相計算
        MoonPhase moonPhase = MoonPhase.compute()
            .on(date)
            .execute();
            
        // 月の出・月の入り
        MoonTimes moonTimes = MoonTimes.compute()
            .on(date)
            .at(latitude, longitude)
            .execute();
            
        return AstronomicalInfo.builder()
            .sunrise(sunTimes.getRise())
            .sunset(sunTimes.getSet())
            .solarNoon(sunTimes.getNoon())
            .moonPhase(calculateMoonPhase(moonPhase))
            .moonrise(moonTimes.getRise())
            .moonset(moonTimes.getSet())
            .build();
    }
    
    private MoonPhaseType calculateMoonPhase(MoonPhase phase) {
        double illumination = phase.getIllumination();
        
        if (illumination < 0.05) return MoonPhaseType.NEW_MOON;
        if (illumination < 0.45) return MoonPhaseType.WAXING_CRESCENT;
        if (illumination < 0.55) return MoonPhaseType.FIRST_QUARTER;
        if (illumination < 0.95) return MoonPhaseType.WAXING_GIBBOUS;
        if (illumination >= 0.95) return MoonPhaseType.FULL_MOON;
        
        // 欠けていく月相も同様に計算
        return MoonPhaseType.WANING_GIBBOUS;
    }
}
```

### 5. キャッシング実装

#### 5.1 CacheManager.java
```java
package com.github.yuu1111.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;

public class CacheManager {
    private final Cache<String, Object> staticCache;
    private final Cache<String, Object> semiStaticCache;
    
    public CacheManager() {
        // 静的データ用キャッシュ（24時間）
        this.staticCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofHours(24))
            .build();
            
        // 準静的データ用キャッシュ（7日間）
        this.semiStaticCache = Caffeine.newBuilder()
            .maximumSize(500)
            .expireAfterWrite(Duration.ofDays(7))
            .build();
    }
    
    public void cacheTimezoneData(String timezone, TimezoneData data) {
        staticCache.put("tz:" + timezone, data);
    }
    
    public void cacheHolidayData(String key, HolidayData data) {
        semiStaticCache.put("holiday:" + key, data);
    }
}
```

## 🧪 テスト計画

### 宗教的カレンダーテスト
```java
@Test
public void testIslamicCalendarConversion() {
    CalendarService service = new CalendarService();
    LocalDate gregorian = LocalDate.of(2024, 1, 15);
    
    IslamicDate islamic = service.convertToIslamic(gregorian);
    
    // 2024年1月15日 = イスラム暦1445年7月3日
    assertEquals(1445, islamic.getYear());
    assertEquals(7, islamic.getMonth());
    assertEquals(3, islamic.getDay());
}

@Test
public void testRamadanDetection() {
    // ラマダン期間の検出テスト
}
```

### DSTテスト
```java
@Test
public void testDSTTransition() {
    DSTManager manager = new DSTManager();
    
    // 2024年のDST開始日（米国）
    LocalDateTime springForward = LocalDateTime.of(2024, 3, 10, 2, 0);
    DSTInfo info = manager.getDSTInfo("America/New_York", springForward);
    
    assertTrue(info.isDSTActive());
    assertEquals(Duration.ofHours(1), info.getDstOffset());
}
```

## 📊 成功基準

### 機能要件
- [ ] 全宗教暦への変換が正確
- [ ] DSTの切り替えが正確に検出される
- [ ] 天文学的情報が正確
- [ ] キャッシュが適切に動作

### パフォーマンス要件
- [ ] カレンダー変換 < 50ms
- [ ] キャッシュヒット率 > 80%
- [ ] メモリ使用量 < 512MB

## 🚀 実行例

```bash
# 宗教的カレンダー変換
curl -X POST http://localhost:3000/tools/get_religious_calendar \
  -H "Content-Type: application/json" \
  -d '{
    "date": "2024-01-15",
    "calendar_type": "islamic",
    "include_holidays": true
  }'

# 天文学的情報取得
curl -X POST http://localhost:3000/tools/get_astronomical_info \
  -H "Content-Type: application/json" \
  -d '{
    "latitude": 35.6762,
    "longitude": 139.6503,
    "date": "2024-01-15",
    "include_moon_phase": true
  }'
```

## 📝 チェックリスト

### 実装
- [ ] DSTManager実装
- [ ] CalendarService実装
- [ ] 各宗教暦コンバーター実装
- [ ] AstronomyService実装
- [ ] CacheManager実装

### テスト
- [ ] 宗教暦変換テスト
- [ ] DST境界テスト
- [ ] 天文計算テスト
- [ ] キャッシュ動作テスト

### 検証
- [ ] イスラム暦の精度確認
- [ ] ユダヤ暦の安息日計算確認
- [ ] 月相計算の精度確認

## 🔗 関連ドキュメント
- [Phase 1: 基本実装](./PHASE1_BASIC.md)
- [Phase 3: 最適化と拡張](./PHASE3_OPTIMIZATION.md)

---

*Version: 1.0.0*  
*Last Updated: 2024-01-15*