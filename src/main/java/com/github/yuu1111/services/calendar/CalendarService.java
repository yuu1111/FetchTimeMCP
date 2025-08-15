package com.github.yuu1111.services.calendar;

import com.ibm.icu.util.BuddhistCalendar;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.ChineseCalendar;
import com.ibm.icu.util.GregorianCalendar;
import com.ibm.icu.util.HebrewCalendar;
import com.ibm.icu.util.IndianCalendar;
import com.ibm.icu.util.IslamicCalendar;
import com.ibm.icu.util.JapaneseCalendar;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 宗教的カレンダー変換サービス グレゴリオ暦を各種宗教暦に変換
 */
public class CalendarService {

  private static final Logger logger = LoggerFactory.getLogger(CalendarService.class);

  /**
   * グレゴリオ暦を宗教暦に変換
   */
  public ReligiousCalendarInfo convertToReligiousCalendar(LocalDate date, CalendarType type) {
    logger.debug("Converting {} to {} calendar", date, type);

    // グレゴリオ暦をセット
    GregorianCalendar gregorian = new GregorianCalendar();
    gregorian.set(date.getYear(), date.getMonthValue() - 1, date.getDayOfMonth());

    // 対象カレンダーを作成
    Calendar targetCalendar = createCalendar(type);
    targetCalendar.setTime(gregorian.getTime());

    // カレンダー情報を抽出
    return extractCalendarInfo(targetCalendar, type);
  }

  /**
   * カレンダータイプに応じたカレンダーインスタンスを作成
   */
  private Calendar createCalendar(CalendarType type) {
    return switch (type) {
      case ISLAMIC -> new IslamicCalendar();
      case HEBREW -> new HebrewCalendar();
      case BUDDHIST -> new BuddhistCalendar();
      case HINDU -> new IndianCalendar();
      case CHINESE -> new ChineseCalendar();
      case JAPANESE -> new JapaneseCalendar();
    };
  }

  /**
   * カレンダーから情報を抽出
   */
  private ReligiousCalendarInfo extractCalendarInfo(Calendar calendar, CalendarType type) {
    ReligiousCalendarInfo info = new ReligiousCalendarInfo();

    info.setCalendarType(type.toString());
    info.setCalendarName(getCalendarName(type));
    info.setYear(calendar.get(Calendar.YEAR));
    info.setMonth(calendar.get(Calendar.MONTH) + 1);
    info.setDay(calendar.get(Calendar.DAY_OF_MONTH));
    info.setEra(getEraName(calendar, type));
    info.setMonthName(getMonthName(calendar, type));
    info.setWeekDay(getWeekDayName(calendar, type));
    info.setLeapYear(isLeapYear(calendar));

    // 宗教的な祝日や記念日を設定
    setHolidaysAndObservances(info, calendar, type);

    return info;
  }

  /**
   * カレンダー名を取得
   */
  private String getCalendarName(CalendarType type) {
    return switch (type) {
      case ISLAMIC -> "Islamic Calendar (Hijri)";
      case HEBREW -> "Hebrew Calendar";
      case BUDDHIST -> "Buddhist Calendar";
      case HINDU -> "Hindu Calendar";
      case CHINESE -> "Chinese Calendar";
      case JAPANESE -> "Japanese Calendar";
    };
  }

  /**
   * 時代名を取得
   */
  private String getEraName(Calendar calendar, CalendarType type) {
    switch (type) {
      case ISLAMIC:
        return "AH"; // Anno Hegirae
      case HEBREW:
        return "AM"; // Anno Mundi
      case BUDDHIST:
        return "BE"; // Buddhist Era
      case JAPANESE:
        if (calendar instanceof JapaneseCalendar) {
          int era = calendar.get(Calendar.ERA);
          return getJapaneseEraName(era);
        }
        return "";
      default:
        return "";
    }
  }

  /**
   * 日本の元号名を取得
   */
  private String getJapaneseEraName(int era) {
    // 簡易的な実装（実際にはもっと多くの元号がある）
    return switch (era) {
      case 235 -> "令和"; // Reiwa
      case 234 -> "平成"; // Heisei
      case 233 -> "昭和"; // Showa
      case 232 -> "大正"; // Taisho
      case 231 -> "明治"; // Meiji
      default -> "Unknown";
    };
  }

  /**
   * 月名を取得
   */
  private String getMonthName(Calendar calendar, CalendarType type) {
    int month = calendar.get(Calendar.MONTH);

    switch (type) {
      case ISLAMIC:
        return getIslamicMonthName(month);
      case HEBREW:
        return getHebrewMonthName(calendar);
      case CHINESE:
        return getChineseMonthName(calendar);
      case JAPANESE:
        return getJapaneseMonthName(month);
      default:
        return String.valueOf(month + 1);
    }
  }

  /**
   * イスラム暦の月名
   */
  private String getIslamicMonthName(int month) {
    String[] months = {"Muharram", "Safar", "Rabi' al-awwal", "Rabi' al-thani", "Jumada al-awwal",
        "Jumada al-thani", "Rajab", "Sha'ban", "Ramadan", "Shawwal", "Dhu al-Qi'dah",
        "Dhu al-Hijjah"};
    return months[month];
  }

  /**
   * ヘブライ暦の月名
   */
  private String getHebrewMonthName(Calendar calendar) {
    if (calendar instanceof HebrewCalendar) {
      int month = calendar.get(Calendar.MONTH);
      String[] months = {"Tishrei", "Cheshvan", "Kislev", "Tevet", "Shevat", "Adar", "Nisan",
          "Iyar", "Sivan", "Tammuz", "Av", "Elul"};
      if (month < months.length) {
        return months[month];
      }
      // Adar IIの場合
      if (month == HebrewCalendar.ADAR_1) {
        return "Adar I";
      }
    }
    return "";
  }

  /**
   * 中国暦の月名
   */
  private String getChineseMonthName(Calendar calendar) {
    if (calendar instanceof ChineseCalendar) {
      int month = calendar.get(Calendar.MONTH);
      return (month + 1) + "月";
    }
    return "";
  }

  /**
   * 日本暦の月名
   */
  private String getJapaneseMonthName(int month) {
    String[] months = {"睦月", "如月", "弥生", "卯月", "皐月", "水無月", "文月", "葉月", "長月",
        "神無月", "霜月", "師走"};
    return months[month];
  }

  /**
   * 曜日名を取得
   */
  private String getWeekDayName(Calendar calendar, CalendarType type) {
    int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);

    if (type == CalendarType.HEBREW) {
      return getHebrewWeekDayName(dayOfWeek);
    }

    String[] weekDays = {"", "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday",
        "Saturday"};
    return weekDays[dayOfWeek];
  }

  /**
   * ヘブライ暦の曜日名
   */
  private String getHebrewWeekDayName(int dayOfWeek) {
    String[] weekDays = {"", "Yom Rishon", "Yom Sheni", "Yom Shlishi", "Yom Revi'i", "Yom Chamishi",
        "Yom Shishi", "Shabbat"};
    return weekDays[dayOfWeek];
  }

  /**
   * 閏年かどうかを判定
   */
  private boolean isLeapYear(Calendar calendar) {
    if (calendar instanceof ChineseCalendar chinese) {
      return chinese.getActualMaximum(Calendar.MONTH) > 11;
    }
    if (calendar instanceof HebrewCalendar hebrew) {
      return HebrewCalendar.isLeapYear(hebrew.get(Calendar.YEAR));
    }
    if (calendar instanceof IslamicCalendar) {
      // イスラム暦の閏年計算
      int year = calendar.get(Calendar.YEAR);
      return (year * 11 + 14) % 30 < 11;
    }
    return false;
  }

  /**
   * 祝日と記念日を設定
   */
  private void setHolidaysAndObservances(ReligiousCalendarInfo info, Calendar calendar,
      CalendarType type) {
    Map<String, String> holidays = new HashMap<>();
    Map<String, String> observances = new HashMap<>();

    switch (type) {
      case ISLAMIC:
        setIslamicHolidays(holidays, observances, calendar);
        break;
      case HEBREW:
        setHebrewHolidays(holidays, observances, calendar);
        break;
      case BUDDHIST:
        setBuddhistHolidays(holidays, observances, calendar);
        break;
      case CHINESE:
        setChineseHolidays(holidays, observances, calendar);
        break;
      case JAPANESE:
        setJapaneseHolidays(holidays, observances, calendar);
        break;
    }

    info.setHolidays(holidays);
    info.setObservances(observances);
  }

  /**
   * イスラム暦の祝日設定
   */
  private void setIslamicHolidays(Map<String, String> holidays, Map<String, String> observances,
      Calendar calendar) {
    int month = calendar.get(Calendar.MONTH);
    int day = calendar.get(Calendar.DAY_OF_MONTH);

    // ラマダン
    if (month == 8) { // Ramadan
      observances.put("Ramadan", "Month of fasting");
    }

    // イード・アル＝フィトル
    if (month == 9 && day == 1) { // Shawwal 1
      holidays.put("Eid al-Fitr", "Festival of Breaking the Fast");
    }

    // イード・アル＝アドハー
    if (month == 11 && day == 10) { // Dhu al-Hijjah 10
      holidays.put("Eid al-Adha", "Festival of Sacrifice");
    }

    // ムハンマド生誕祭
    if (month == 2 && day == 12) { // Rabi' al-awwal 12
      holidays.put("Mawlid al-Nabi", "Prophet Muhammad's Birthday");
    }
  }

  /**
   * ヘブライ暦の祝日設定
   */
  private void setHebrewHolidays(Map<String, String> holidays, Map<String, String> observances,
      Calendar calendar) {
    int month = calendar.get(Calendar.MONTH);
    int day = calendar.get(Calendar.DAY_OF_MONTH);
    int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);

    // 安息日
    if (dayOfWeek == Calendar.SATURDAY) {
      observances.put("Shabbat", "Sabbath");
    }

    // ロシュ・ハシャナー（新年）
    if (month == HebrewCalendar.TISHRI && (day == 1 || day == 2)) {
      holidays.put("Rosh Hashanah", "Jewish New Year");
    }

    // ヨム・キプル（贖罪の日）
    if (month == HebrewCalendar.TISHRI && day == 10) {
      holidays.put("Yom Kippur", "Day of Atonement");
    }

    // ペサハ（過越祭）
    if (month == HebrewCalendar.NISAN && day >= 15 && day <= 22) {
      holidays.put("Pesach", "Passover");
    }
  }

  /**
   * 仏教暦の祝日設定
   */
  private void setBuddhistHolidays(Map<String, String> holidays, Map<String, String> observances,
      Calendar calendar) {
    // 簡易的な実装
    int month = calendar.get(Calendar.MONTH);
    int day = calendar.get(Calendar.DAY_OF_MONTH);

    // ヴェサク（仏誕節）
    if (month == 4 && day == 15) { // 通常5月の満月
      holidays.put("Vesak", "Buddha's Birthday");
    }
  }

  /**
   * 中国暦の祝日設定
   */
  private void setChineseHolidays(Map<String, String> holidays, Map<String, String> observances,
      Calendar calendar) {
    if (calendar instanceof ChineseCalendar) {
      int month = calendar.get(Calendar.MONTH);
      int day = calendar.get(Calendar.DAY_OF_MONTH);

      // 春節
      if (month == 0 && day == 1) {
        holidays.put("Spring Festival", "Chinese New Year");
      }

      // 中秋節
      if (month == 7 && day == 15) {
        holidays.put("Mid-Autumn Festival", "Moon Festival");
      }
    }
  }

  /**
   * 日本暦の祝日設定
   */
  private void setJapaneseHolidays(Map<String, String> holidays, Map<String, String> observances,
      Calendar calendar) {
    // 簡易的な実装（実際の祝日計算はもっと複雑）
    int month = calendar.get(Calendar.MONTH);
    int day = calendar.get(Calendar.DAY_OF_MONTH);

    if (month == 0 && day == 1) {
      holidays.put("元日", "New Year's Day");
    }
  }

  public enum CalendarType {
    ISLAMIC, HEBREW, BUDDHIST, HINDU, CHINESE, JAPANESE
  }
}