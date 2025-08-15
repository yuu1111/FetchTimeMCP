package com.github.yuu1111.services.astronomy;

/**
 * 月相のタイプ
 */
public enum MoonPhaseType {
    NEW_MOON("New Moon", "新月", "🌑"),
    WAXING_CRESCENT("Waxing Crescent", "三日月（上弦前）", "🌒"),
    FIRST_QUARTER("First Quarter", "上弦の月", "🌓"),
    WAXING_GIBBOUS("Waxing Gibbous", "十三夜月", "🌔"),
    FULL_MOON("Full Moon", "満月", "🌕"),
    WANING_GIBBOUS("Waning Gibbous", "十八夜月", "🌖"),
    LAST_QUARTER("Last Quarter", "下弦の月", "🌗"),
    WANING_CRESCENT("Waning Crescent", "有明月", "🌘"),
    UNKNOWN("Unknown", "不明", "❓");
    
    private final String englishName;
    private final String japaneseName;
    private final String emoji;
    
    MoonPhaseType(String englishName, String japaneseName, String emoji) {
        this.englishName = englishName;
        this.japaneseName = japaneseName;
        this.emoji = emoji;
    }
    
    public String getEnglishName() {
        return englishName;
    }
    
    public String getJapaneseName() {
        return japaneseName;
    }
    
    public String getEmoji() {
        return emoji;
    }
    
    @Override
    public String toString() {
        return englishName;
    }
}