package com.github.yuu1111.tools.impl;

import com.github.yuu1111.tools.ToolExecutionException;
import com.github.yuu1111.tools.ToolResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * GetCurrentTimeツールのテスト
 */
@DisplayName("GetCurrentTime Tool Tests")
class GetCurrentTimeTest {
    
    private GetCurrentTime tool;
    
    @BeforeEach
    void setUp() {
        tool = new GetCurrentTime();
    }
    
    @Test
    @DisplayName("ツール名と説明を正しく返す")
    void testToolMetadata() {
        assertThat(tool.getName()).isEqualTo("get_current_time");
        assertThat(tool.getDescription()).isNotBlank();
        assertThat(tool.getParameterSchema()).isNotNull();
        assertThat(tool.isCacheable()).isFalse();
    }
    
    @Test
    @DisplayName("デフォルトパラメータで実行できる")
    void testExecuteWithDefaults() throws ToolExecutionException {
        Map<String, Object> params = new HashMap<>();
        
        ToolResponse response = tool.execute(params);
        
        assertThat(response).isNotNull();
        assertThat(response.data()).containsKeys("timestamp", "timezone", "unix_timestamp");
        assertThat(response.data().get("timezone")).isEqualTo("UTC");
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"Asia/Tokyo", "America/New_York", "Europe/London", "UTC"})
    @DisplayName("様々なタイムゾーンで実行できる")
    void testExecuteWithDifferentTimezones(String timezone) throws ToolExecutionException {
        Map<String, Object> params = Map.of("timezone", timezone);
        
        ToolResponse response = tool.execute(params);
        
        assertThat(response.data().get("timezone")).isEqualTo(timezone);
        assertThat(response.data()).containsKey("unix_timestamp");
    }
    
    @Test
    @DisplayName("無効なタイムゾーンでエラーを返す")
    void testExecuteWithInvalidTimezone() {
        Map<String, Object> params = Map.of("timezone", "Invalid/Timezone");
        
        assertThatThrownBy(() -> tool.execute(params))
            .isInstanceOf(ToolExecutionException.class)
            .hasMessageContaining("Invalid timezone");
    }
    
    @Test
    @DisplayName("DST情報を含めることができる")
    void testExecuteWithDSTInfo() throws ToolExecutionException {
        Map<String, Object> params = Map.of(
            "timezone", "America/New_York",
            "include_dst", true
        );
        
        ToolResponse response = tool.execute(params);
        
        assertThat(response.data()).containsKey("dst_info");
        Map<String, Object> dstInfo = (Map<String, Object>) response.data().get("dst_info");
        assertThat(dstInfo).containsKeys("is_dst", "dst_offset");
    }
    
    @Test
    @DisplayName("詳細なタイムゾーン情報を含めることができる")
    void testExecuteWithZoneInfo() throws ToolExecutionException {
        Map<String, Object> params = Map.of(
            "timezone", "Asia/Tokyo",
            "include_zone_info", true
        );
        
        ToolResponse response = tool.execute(params);
        
        assertThat(response.data()).containsKey("zone_info");
        Map<String, Object> zoneInfo = (Map<String, Object>) response.data().get("zone_info");
        assertThat(zoneInfo).containsKeys("zone_id", "display_name");
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"ISO8601", "RFC3339", "UNIX", "HUMAN"})
    @DisplayName("異なるフォーマットで時刻を出力できる")
    void testExecuteWithDifferentFormats(String format) throws ToolExecutionException {
        Map<String, Object> params = Map.of(
            "timezone", "UTC",
            "format", format
        );
        
        ToolResponse response = tool.execute(params);
        
        assertThat(response.data()).containsKey("timestamp");
        Object timestamp = response.data().get("timestamp");
        
        if ("UNIX".equals(format)) {
            // UnixタイムスタンプはStringとして返される
            assertThat(timestamp).isInstanceOf(String.class);
            assertThat(timestamp.toString()).matches("\\d+");
        } else {
            assertThat(timestamp).isInstanceOf(String.class);
        }
    }
    
    @Test
    @DisplayName("カスタムフォーマットで時刻を出力できる")
    void testExecuteWithCustomFormat() throws ToolExecutionException {
        Map<String, Object> params = Map.of(
            "timezone", "Asia/Tokyo",
            "format", "CUSTOM",
            "custom_format", "yyyy年MM月dd日 HH時mm分ss秒"
        );
        
        ToolResponse response = tool.execute(params);
        
        String timestamp = (String) response.data().get("timestamp");
        assertThat(timestamp).matches("\\d{4}年\\d{2}月\\d{2}日 \\d{2}時\\d{2}分\\d{2}秒");
    }
    
    @Test
    @DisplayName("日付コンポーネントを含む")
    void testDateComponents() throws ToolExecutionException {
        Map<String, Object> params = Map.of("timezone", "UTC");
        
        ToolResponse response = tool.execute(params);
        
        assertThat(response.data()).containsKey("date_components");
        Map<String, Object> components = (Map<String, Object>) response.data().get("date_components");
        assertThat(components).containsKeys(
            "year", "month", "day", "hour", "minute", "second",
            "day_of_week", "day_of_year"
        );
    }
    
    @Test
    @DisplayName("タイムゾーン略称を正規化できる")
    void testTimezoneAbbreviationNormalization() throws ToolExecutionException {
        Map<String, Object> params = Map.of("timezone", "JST");
        
        ToolResponse response = tool.execute(params);
        
        // JSTはAsia/Tokyoに正規化される
        assertThat(response.data().get("timezone")).isEqualTo("Asia/Tokyo");
    }
    
    @Test
    @DisplayName("パラメータ検証が正しく動作する")
    void testParameterValidation() {
        // 無効な型のパラメータ
        Map<String, Object> invalidParams = Map.of(
            "timezone", 123,  // 数値は無効
            "format", "ISO8601"
        );
        
        var error = tool.validateParameters(invalidParams);
        assertThat(error).isNotNull();
        assertThat(error.message()).contains("timezone must be a string");
        
        // 無効なフォーマット
        Map<String, Object> invalidFormat = Map.of(
            "timezone", "UTC",
            "format", "INVALID_FORMAT"
        );
        
        error = tool.validateParameters(invalidFormat);
        assertThat(error).isNotNull();
        assertThat(error.message()).contains("Invalid format");
    }
}