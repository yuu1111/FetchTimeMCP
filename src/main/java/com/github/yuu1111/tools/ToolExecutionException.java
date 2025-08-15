package com.github.yuu1111.tools;

import com.github.yuu1111.protocol.MCPError;

/**
 * ツール実行時の例外
 */
public class ToolExecutionException extends Exception {
    
    private final MCPError mcpError;
    
    /**
     * MCPエラー情報付きで例外を作成
     */
    public ToolExecutionException(MCPError mcpError) {
        super(mcpError.message());
        this.mcpError = mcpError;
    }
    
    /**
     * メッセージと原因例外で作成
     */
    public ToolExecutionException(String message, Throwable cause) {
        super(message, cause);
        this.mcpError = MCPError.internalError(message);
    }
    
    /**
     * メッセージのみで作成
     */
    public ToolExecutionException(String message) {
        super(message);
        this.mcpError = MCPError.internalError(message);
    }
    
    /**
     * エラーコードとメッセージで作成
     */
    public ToolExecutionException(int errorCode, String message) {
        super(message);
        this.mcpError = new MCPError(errorCode, message, null);
    }
    
    /**
     * エラーコード、メッセージ、詳細データで作成
     */
    public ToolExecutionException(int errorCode, String message, Object data) {
        super(message);
        this.mcpError = new MCPError(errorCode, message, data);
    }
    
    /**
     * MCPエラー情報を取得
     */
    public MCPError getMcpError() {
        return mcpError;
    }
    
    /**
     * よく使用する例外のファクトリメソッド
     */
    public static ToolExecutionException invalidTimezone(String timezone) {
        return new ToolExecutionException(
            MCPError.TIMEZONE_ERROR,
            "Invalid timezone: " + timezone
        );
    }
    
    public static ToolExecutionException apiFailure(String api, String reason) {
        return new ToolExecutionException(
            MCPError.API_ERROR,
            "API call failed: " + api,
            reason
        );
    }
    
    public static ToolExecutionException invalidParameter(String parameter, String reason) {
        return new ToolExecutionException(
            MCPError.INVALID_PARAMS,
            "Invalid parameter '" + parameter + "': " + reason
        );
    }
    
    public static ToolExecutionException rateLimitExceeded(String api) {
        return new ToolExecutionException(
            MCPError.RATE_LIMIT_ERROR,
            "Rate limit exceeded for API: " + api
        );
    }
}