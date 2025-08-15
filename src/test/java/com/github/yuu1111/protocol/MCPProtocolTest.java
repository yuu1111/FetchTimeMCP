package com.github.yuu1111.protocol;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * MCPプロトコルクラスのテスト
 */
@DisplayName("MCP Protocol Tests")
class MCPProtocolTest {
    
    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }
    
    @Test
    @DisplayName("MCPRequestの作成とバリデーション")
    void testMCPRequest() {
        MCPRequest request = MCPRequest.create(
            "test-id",
            "tools/get_current_time",
            Map.of("timezone", "Asia/Tokyo")
        );
        
        assertThat(request.jsonrpc()).isEqualTo("2.0");
        assertThat(request.id()).isEqualTo("test-id");
        assertThat(request.method()).isEqualTo("tools/get_current_time");
        assertThat(request.params()).containsEntry("timezone", "Asia/Tokyo");
        assertThat(request.isValid()).isTrue();
        assertThat(request.isToolExecution()).isTrue();
        assertThat(request.getToolName()).isEqualTo("get_current_time");
    }
    
    @Test
    @DisplayName("無効なMCPRequestの検証")
    void testInvalidMCPRequest() {
        MCPRequest invalidRequest = new MCPRequest(
            "1.0", // 無効なバージョン
            "test-id",
            "method",
            null
        );
        
        assertThat(invalidRequest.isValid()).isFalse();
        
        MCPRequest noIdRequest = new MCPRequest(
            "2.0",
            null, // IDなし
            "method",
            null
        );
        
        assertThat(noIdRequest.isValid()).isFalse();
    }
    
    @Test
    @DisplayName("MCPResponseの成功レスポンス作成")
    void testMCPResponseSuccess() {
        Map<String, Object> result = Map.of(
            "timestamp", "2024-01-15T12:00:00Z",
            "timezone", "UTC"
        );
        
        MCPResponse response = MCPResponse.success("test-id", result);
        
        assertThat(response.jsonrpc()).isEqualTo("2.0");
        assertThat(response.id()).isEqualTo("test-id");
        assertThat(response.result()).isEqualTo(result);
        assertThat(response.error()).isNull();
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.isError()).isFalse();
    }
    
    @Test
    @DisplayName("MCPResponseのエラーレスポンス作成")
    void testMCPResponseError() {
        MCPError error = MCPError.invalidParams("Invalid timezone");
        MCPResponse response = MCPResponse.error("test-id", error);
        
        assertThat(response.jsonrpc()).isEqualTo("2.0");
        assertThat(response.id()).isEqualTo("test-id");
        assertThat(response.result()).isNull();
        assertThat(response.error()).isEqualTo(error);
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.isError()).isTrue();
    }
    
    @Test
    @DisplayName("MCPErrorの標準エラー作成")
    void testMCPErrorStandard() {
        MCPError parseError = MCPError.parseError("Invalid JSON");
        assertThat(parseError.code()).isEqualTo(MCPError.PARSE_ERROR);
        assertThat(parseError.message()).isEqualTo("Parse error");
        assertThat(parseError.data()).isEqualTo("Invalid JSON");
        
        MCPError methodNotFound = MCPError.methodNotFound("unknown_method");
        assertThat(methodNotFound.code()).isEqualTo(MCPError.METHOD_NOT_FOUND);
        assertThat(methodNotFound.message()).contains("unknown_method");
        
        MCPError invalidParams = MCPError.invalidParams("Missing required parameter");
        assertThat(invalidParams.code()).isEqualTo(MCPError.INVALID_PARAMS);
        assertThat(invalidParams.data()).isEqualTo("Missing required parameter");
    }
    
    @Test
    @DisplayName("MCPErrorのカスタムエラー作成")
    void testMCPErrorCustom() {
        MCPError timezoneError = MCPError.timezoneError("Invalid timezone: Mars/Olympus");
        assertThat(timezoneError.code()).isEqualTo(MCPError.TIMEZONE_ERROR);
        assertThat(timezoneError.message()).isEqualTo("Timezone error");
        assertThat(timezoneError.data()).isEqualTo("Invalid timezone: Mars/Olympus");
        
        MCPError apiError = MCPError.apiError("WorldTimeAPI", "Connection timeout");
        assertThat(apiError.code()).isEqualTo(MCPError.API_ERROR);
        assertThat(apiError.message()).contains("WorldTimeAPI");
        assertThat(apiError.data()).isEqualTo("Connection timeout");
    }
    
    @Test
    @DisplayName("JSONシリアライゼーション - Request")
    void testRequestSerialization() throws Exception {
        MCPRequest request = MCPRequest.create(
            "123",
            "tools/get_current_time",
            Map.of("timezone", "UTC", "format", "ISO8601")
        );
        
        String json = objectMapper.writeValueAsString(request);
        assertThat(json).contains("\"jsonrpc\":\"2.0\"");
        assertThat(json).contains("\"id\":\"123\"");
        assertThat(json).contains("\"method\":\"tools/get_current_time\"");
        assertThat(json).contains("\"timezone\":\"UTC\"");
        
        MCPRequest deserialized = objectMapper.readValue(json, MCPRequest.class);
        assertThat(deserialized).isEqualTo(request);
    }
    
    @Test
    @DisplayName("JSONシリアライゼーション - Response")
    void testResponseSerialization() throws Exception {
        MCPResponse response = MCPResponse.success(
            "456",
            Map.of("result", "success", "timestamp", 1234567890L)
        );
        
        String json = objectMapper.writeValueAsString(response);
        assertThat(json).contains("\"jsonrpc\":\"2.0\"");
        assertThat(json).contains("\"id\":\"456\"");
        assertThat(json).contains("\"result\"");
        assertThat(json).doesNotContain("\"error\""); // Nullフィールドは含まれない
        
        MCPResponse deserialized = objectMapper.readValue(json, MCPResponse.class);
        assertThat(deserialized.id()).isEqualTo(response.id());
        assertThat(deserialized.isSuccess()).isTrue();
    }
    
    @Test
    @DisplayName("JSONシリアライゼーション - Error Response")
    void testErrorResponseSerialization() throws Exception {
        MCPError error = new MCPError(-32001, "Custom error", Map.of("detail", "Extra info"));
        MCPResponse response = MCPResponse.error("789", error);
        
        String json = objectMapper.writeValueAsString(response);
        assertThat(json).contains("\"jsonrpc\":\"2.0\"");
        assertThat(json).contains("\"id\":\"789\"");
        assertThat(json).contains("\"error\"");
        assertThat(json).contains("\"code\":-32001");
        assertThat(json).contains("\"message\":\"Custom error\"");
        assertThat(json).doesNotContain("\"result\""); // Nullフィールドは含まれない
        
        MCPResponse deserialized = objectMapper.readValue(json, MCPResponse.class);
        assertThat(deserialized.isError()).isTrue();
        assertThat(deserialized.error().code()).isEqualTo(-32001);
    }
    
    @Test
    @DisplayName("ツール実行リクエストの判定")
    void testToolExecutionDetection() {
        MCPRequest toolRequest = MCPRequest.create(
            "id",
            "tools/convert_timezone",
            Map.of()
        );
        assertThat(toolRequest.isToolExecution()).isTrue();
        assertThat(toolRequest.getToolName()).isEqualTo("convert_timezone");
        
        MCPRequest nonToolRequest = MCPRequest.create(
            "id",
            "ping",
            Map.of()
        );
        assertThat(nonToolRequest.isToolExecution()).isFalse();
        assertThat(nonToolRequest.getToolName()).isNull();
    }
}