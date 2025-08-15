# Phase 1: 基本実装

## 📋 概要
基本的な時間取得機能とMCPサーバーの実装を行うフェーズです。

**期間**: 3-5日  
**優先度**: 高

## 🎯 目標
- MCPサーバーの基本実装
- 現在時刻の取得機能
- タイムゾーン変換機能
- 基本的なエラーハンドリング

## 📦 実装タスク

### 1. プロジェクトセットアップ
```xml
<!-- pom.xml に追加 -->
<dependencies>
    <!-- MCP実装 -->
    <dependency>
        <groupId>com.fasterxml.jackson.core</groupId>
        <artifactId>jackson-databind</artifactId>
        <version>2.16.1</version>
    </dependency>
    
    <!-- HTTP通信 -->
    <dependency>
        <groupId>com.squareup.okhttp3</groupId>
        <artifactId>okhttp</artifactId>
        <version>4.12.0</version>
    </dependency>
    
    <!-- ロギング -->
    <dependency>
        <groupId>ch.qos.logback</groupId>
        <artifactId>logback-classic</artifactId>
        <version>1.4.14</version>
    </dependency>
</dependencies>
```

### 2. MCPサーバー基本実装

#### 2.1 MCPServer.java
```java
package com.github.yuu1111.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.ServerSocket;
import java.net.Socket;

public class MCPServer {
    private final int port;
    private final ObjectMapper objectMapper;
    private final ToolRegistry toolRegistry;
    
    public MCPServer(int port) {
        this.port = port;
        this.objectMapper = new ObjectMapper();
        this.toolRegistry = new ToolRegistry();
    }
    
    public void start() {
        // TCP/IPソケット実装
    }
}
```

#### 2.2 ToolRegistry.java
```java
package com.github.yuu1111.server;

import java.util.Map;
import java.util.HashMap;

public class ToolRegistry {
    private final Map<String, MCPTool> tools = new HashMap<>();
    
    public void register(String name, MCPTool tool) {
        tools.put(name, tool);
    }
    
    public MCPTool getTool(String name) {
        return tools.get(name);
    }
}
```

### 3. 基本ツール実装

#### 3.1 GetCurrentTime.java
```java
package com.github.yuu1111.tools;

import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class GetCurrentTime implements MCPTool {
    @Override
    public ToolResponse execute(ToolRequest request) {
        String timezone = request.getParameter("timezone", "UTC");
        ZoneId zoneId = ZoneId.of(timezone);
        ZonedDateTime now = ZonedDateTime.now(zoneId);
        
        return new ToolResponse(Map.of(
            "timestamp", now.toString(),
            "timezone", timezone,
            "unix_timestamp", now.toEpochSecond()
        ));
    }
}
```

#### 3.2 ConvertTimezone.java
```java
package com.github.yuu1111.tools;

public class ConvertTimezone implements MCPTool {
    @Override
    public ToolResponse execute(ToolRequest request) {
        // タイムゾーン変換ロジック
    }
}
```

### 4. API統合（WorldTimeAPI）

#### 4.1 WorldTimeAPIClient.java
```java
package com.github.yuu1111.api;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class WorldTimeAPIClient {
    private static final String BASE_URL = "http://worldtimeapi.org/api";
    private final OkHttpClient client;
    
    public WorldTimeAPIClient() {
        this.client = new OkHttpClient();
    }
    
    public TimeInfo getCurrentTime(String timezone) {
        String url = BASE_URL + "/timezone/" + timezone;
        Request request = new Request.Builder()
            .url(url)
            .build();
            
        try (Response response = client.newCall(request).execute()) {
            // レスポンス処理
        }
    }
}
```

## 🧪 テスト計画

### 単体テスト
```java
@Test
public void testGetCurrentTime() {
    GetCurrentTime tool = new GetCurrentTime();
    ToolRequest request = new ToolRequest();
    request.setParameter("timezone", "Asia/Tokyo");
    
    ToolResponse response = tool.execute(request);
    
    assertNotNull(response.getData().get("timestamp"));
    assertEquals("Asia/Tokyo", response.getData().get("timezone"));
}
```

### 統合テスト
- MCPサーバー起動確認
- ツール実行確認
- API連携確認

## 📊 成功基準

### 機能要件
- [ ] MCPサーバーが起動する
- [ ] `get_current_time`ツールが動作する
- [ ] `convert_timezone`ツールが動作する
- [ ] WorldTimeAPIと連携できる
- [ ] エラーハンドリングが適切

### 非機能要件
- [ ] レスポンスタイム < 200ms
- [ ] メモリ使用量 < 256MB
- [ ] 同時接続数 10以上対応

## 🚀 実行手順

```bash
# ビルド
mvn clean compile

# テスト実行
mvn test

# サーバー起動
java -cp target/classes com.github.yuu1111.Main

# 動作確認（別ターミナル）
curl -X POST http://localhost:3000/tools/get_current_time \
  -H "Content-Type: application/json" \
  -d '{"timezone": "Asia/Tokyo"}'
```

## 📝 チェックリスト

### 開発環境
- [ ] Java 21インストール済み
- [ ] Maven設定完了
- [ ] IDE設定完了

### 実装
- [ ] MCPServer.java作成
- [ ] ToolRegistry.java作成
- [ ] GetCurrentTime.java作成
- [ ] ConvertTimezone.java作成
- [ ] WorldTimeAPIClient.java作成

### テスト
- [ ] 単体テスト作成
- [ ] 統合テスト作成
- [ ] 手動テスト実施

### ドキュメント
- [ ] APIドキュメント更新
- [ ] README更新

## 🔗 関連ドキュメント
- [仕様書](./SPECIFICATION.md)
- [Phase 2: 高度な機能](./PHASE2_ADVANCED.md)

---

*Version: 1.0.0*  
*Last Updated: 2024-01-15*