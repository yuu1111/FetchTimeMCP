package com.github.yuu1111.protocol;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * MCPエラー情報 (JSON-RPC 2.0準拠)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MCPError(@JsonProperty("code") int code, @JsonProperty("message") String message,
                       @JsonProperty("data") Object data) {

  // 標準エラーコード (JSON-RPC 2.0)
  public static final int PARSE_ERROR = -32700;
  public static final int INVALID_REQUEST = -32600;
  public static final int METHOD_NOT_FOUND = -32601;
  public static final int INVALID_PARAMS = -32602;
  public static final int INTERNAL_ERROR = -32603;

  // カスタムエラーコード (FetchTimeMCP固有)
  public static final int TIMEZONE_ERROR = -32001;
  public static final int API_ERROR = -32002;
  public static final int CALENDAR_ERROR = -32003;
  public static final int RATE_LIMIT_ERROR = -32004;
  public static final int AUTHENTICATION_ERROR = -32005;

  /**
   * よく使用するエラーのファクトリメソッド
   */
  public static MCPError parseError(String details) {
    return new MCPError(PARSE_ERROR, "Parse error", details);
  }

  public static MCPError invalidRequest(String details) {
    return new MCPError(INVALID_REQUEST, "Invalid request", details);
  }

  public static MCPError methodNotFound(String method) {
    return new MCPError(METHOD_NOT_FOUND, "Method not found: " + method, null);
  }

  public static MCPError invalidParams(String details) {
    return new MCPError(INVALID_PARAMS, "Invalid parameters", details);
  }

  public static MCPError internalError(String details) {
    return new MCPError(INTERNAL_ERROR, "Internal error", details);
  }

  public static MCPError timezoneError(String details) {
    return new MCPError(TIMEZONE_ERROR, "Timezone error", details);
  }

  public static MCPError apiError(String api, String details) {
    return new MCPError(API_ERROR, "API error: " + api, details);
  }
}