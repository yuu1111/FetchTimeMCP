package com.github.yuu1111.protocol;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * MCPプロトコルレスポンス (JSON-RPC 2.0準拠)
 * 成功時はresult、エラー時はerrorフィールドを持つ
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MCPResponse(
    @JsonProperty("jsonrpc") String jsonrpc,
    @JsonProperty("id") String id,
    @JsonProperty("result") Object result,
    @JsonProperty("error") MCPError error
) {
    
    /**
     * 成功レスポンスを作成
     */
    public static MCPResponse success(String id, Object result) {
        return new MCPResponse("2.0", id, result, null);
    }
    
    /**
     * エラーレスポンスを作成
     */
    public static MCPResponse error(String id, MCPError error) {
        return new MCPResponse("2.0", id, null, error);
    }
    
    /**
     * エラーレスポンスを作成（簡易版）
     */
    public static MCPResponse error(String id, int code, String message) {
        return error(id, new MCPError(code, message, null));
    }
    
    /**
     * 成功レスポンスかどうか
     */
    public boolean isSuccess() {
        return error == null && result != null;
    }
    
    /**
     * エラーレスポンスかどうか
     */
    public boolean isError() {
        return error != null;
    }
}