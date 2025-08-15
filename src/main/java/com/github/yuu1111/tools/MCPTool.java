package com.github.yuu1111.tools;

import com.github.yuu1111.protocol.MCPError;
import java.util.Map;

/**
 * MCPツールのインターフェース
 * すべてのツールはこのインターフェースを実装する
 */
public interface MCPTool {
    
    /**
     * ツールの名前を取得
     */
    String getName();
    
    /**
     * ツールの説明を取得
     */
    String getDescription();
    
    /**
     * ツールのパラメータスキーマを取得 (JSON Schema形式)
     */
    Map<String, Object> getParameterSchema();
    
    /**
     * ツールを実行
     * 
     * @param parameters ツールのパラメータ
     * @return 実行結果
     * @throws ToolExecutionException ツール実行時のエラー
     */
    ToolResponse execute(Map<String, Object> parameters) throws ToolExecutionException;
    
    /**
     * パラメータの検証
     * 
     * @param parameters 検証するパラメータ
     * @return 検証結果（nullの場合は成功、エラーがある場合はMCPError）
     */
    default MCPError validateParameters(Map<String, Object> parameters) {
        // デフォルト実装：基本的な検証のみ
        if (parameters == null) {
            return MCPError.invalidParams("Parameters cannot be null");
        }
        
        // サブクラスで詳細な検証を実装
        return null;
    }
    
    /**
     * ツールがキャッシュ可能かどうか
     */
    default boolean isCacheable() {
        return false;
    }
    
    /**
     * キャッシュのTTL（秒）
     */
    default int getCacheTTL() {
        return 0;
    }
}