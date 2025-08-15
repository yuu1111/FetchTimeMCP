package com.github.yuu1111.services.astronomy;

/**
 * 天文学的情報を保持するクラス
 */
public class AstronomicalInfo {

  // 太陽情報
  private String sunrise;
  private String sunset;
  private String solarNoon;
  private String dayLength;

  // トワイライト（薄明）
  private String civilDawn;
  private String civilDusk;
  private String nauticalDawn;
  private String nauticalDusk;
  private String astronomicalDawn;
  private String astronomicalDusk;

  // 月情報
  private String moonrise;
  private String moonset;
  private MoonPhaseType moonPhase;
  private double moonIllumination; // パーセンテージ (0-100)
  private double moonAge; // 月齢（日数）
  private double moonDistance; // 地球からの距離（km）

  // 太陽位置
  private double solarAzimuth; // 方位角（度）
  private double solarAltitude; // 高度（度）

  // Getters and Setters

  public String getSunrise() {
    return sunrise;
  }

  public void setSunrise(String sunrise) {
    this.sunrise = sunrise;
  }

  public String getSunset() {
    return sunset;
  }

  public void setSunset(String sunset) {
    this.sunset = sunset;
  }

  public String getSolarNoon() {
    return solarNoon;
  }

  public void setSolarNoon(String solarNoon) {
    this.solarNoon = solarNoon;
  }

  public String getDayLength() {
    return dayLength;
  }

  public void setDayLength(String dayLength) {
    this.dayLength = dayLength;
  }

  public String getCivilDawn() {
    return civilDawn;
  }

  public void setCivilDawn(String civilDawn) {
    this.civilDawn = civilDawn;
  }

  public String getCivilDusk() {
    return civilDusk;
  }

  public void setCivilDusk(String civilDusk) {
    this.civilDusk = civilDusk;
  }

  public String getNauticalDawn() {
    return nauticalDawn;
  }

  public void setNauticalDawn(String nauticalDawn) {
    this.nauticalDawn = nauticalDawn;
  }

  public String getNauticalDusk() {
    return nauticalDusk;
  }

  public void setNauticalDusk(String nauticalDusk) {
    this.nauticalDusk = nauticalDusk;
  }

  public String getAstronomicalDawn() {
    return astronomicalDawn;
  }

  public void setAstronomicalDawn(String astronomicalDawn) {
    this.astronomicalDawn = astronomicalDawn;
  }

  public String getAstronomicalDusk() {
    return astronomicalDusk;
  }

  public void setAstronomicalDusk(String astronomicalDusk) {
    this.astronomicalDusk = astronomicalDusk;
  }

  public String getMoonrise() {
    return moonrise;
  }

  public void setMoonrise(String moonrise) {
    this.moonrise = moonrise;
  }

  public String getMoonset() {
    return moonset;
  }

  public void setMoonset(String moonset) {
    this.moonset = moonset;
  }

  public MoonPhaseType getMoonPhase() {
    return moonPhase;
  }

  public void setMoonPhase(MoonPhaseType moonPhase) {
    this.moonPhase = moonPhase;
  }

  public double getMoonIllumination() {
    return moonIllumination;
  }

  public void setMoonIllumination(double moonIllumination) {
    this.moonIllumination = moonIllumination;
  }

  public double getMoonAge() {
    return moonAge;
  }

  public void setMoonAge(double moonAge) {
    this.moonAge = moonAge;
  }

  public double getMoonDistance() {
    return moonDistance;
  }

  public void setMoonDistance(double moonDistance) {
    this.moonDistance = moonDistance;
  }

  public double getSolarAzimuth() {
    return solarAzimuth;
  }

  public void setSolarAzimuth(double solarAzimuth) {
    this.solarAzimuth = solarAzimuth;
  }

  public double getSolarAltitude() {
    return solarAltitude;
  }

  public void setSolarAltitude(double solarAltitude) {
    this.solarAltitude = solarAltitude;
  }

  @Override
  public String toString() {
    return "AstronomicalInfo{" + "sunrise='" + sunrise + '\'' + ", sunset='" + sunset + '\''
        + ", moonPhase=" + moonPhase + ", moonIllumination=" + moonIllumination + "%"
        + ", dayLength='" + dayLength + '\'' + '}';
  }
}