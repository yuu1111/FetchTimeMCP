package com.github.yuu1111.tools;

import java.time.Instant;
import java.util.Map;
import java.util.HashMap;

/**
 * ツール実行結果
 * Java 21のRecordを使用
 */
public record ToolResponse(
    Map<String, Object> data,
    Map<String, Object> metadata,
    Instant timestamp
) {
    
    /**
     * データのみでレスポンスを作成
     */
    public static ToolResponse of(Map<String, Object> data) {
        return new ToolResponse(data, new HashMap<>(), Instant.now());
    }
    
    /**
     * データとメタデータでレスポンスを作成
     */
    public static ToolResponse of(Map<String, Object> data, Map<String, Object> metadata) {
        return new ToolResponse(data, metadata, Instant.now());
    }
    
    /**
     * 単一の値でレスポンスを作成
     */
    public static ToolResponse single(String key, Object value) {
        Map<String, Object> data = new HashMap<>();
        data.put(key, value);
        return of(data);
    }
    
    /**
     * ビルダーパターンでレスポンスを構築
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * レスポンスビルダー
     */
    public static class Builder {
        private final Map<String, Object> data = new HashMap<>();
        private final Map<String, Object> metadata = new HashMap<>();
        
        public Builder withData(String key, Object value) {
            this.data.put(key, value);
            return this;
        }
        
        public Builder withAllData(Map<String, Object> data) {
            this.data.putAll(data);
            return this;
        }
        
        public Builder withMetadata(String key, Object value) {
            this.metadata.put(key, value);
            return this;
        }
        
        public Builder withCacheInfo(boolean cached, String cacheKey) {
            this.metadata.put("cached", cached);
            this.metadata.put("cacheKey", cacheKey);
            return this;
        }
        
        public Builder withApiInfo(String apiName, long responseTime) {
            this.metadata.put("api", apiName);
            this.metadata.put("responseTime", responseTime);
            return this;
        }
        
        public ToolResponse build() {
            return new ToolResponse(data, metadata, Instant.now());
        }
    }
}