# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## プロジェクト概要

FetchTimeMCPは、時間・日付情報を提供するMCPサーバーです。タイムゾーン、サマータイム、各種宗教的カレンダーに対応し、包括的な時間情報サービスを提供します。

## ビルド・実行コマンド

```bash
# ビルド
mvn clean compile

# パッケージング
mvn clean package

# テスト実行
mvn test

# 単体テストの実行
mvn test -Dtest=SpecificTestClass

# MCPサーバーの起動
java -jar target/FetchTimeMCP-1.0-SNAPSHOT.jar

# 依存関係のインストール
mvn dependency:resolve
```

## プロジェクト構造

```
FetchTimeMCP/
├── src/main/java/com/github/yuu1111/
│   ├── Main.java                 # エントリーポイント
│   ├── server/
│   │   ├── MCPServer.java       # MCPサーバー実装
│   │   └── ToolRegistry.java    # ツール登録管理
│   ├── tools/                   # MCPツール実装
│   │   ├── GetCurrentTime.java
│   │   ├── ConvertTimezone.java
│   │   ├── GetReligiousCalendar.java
│   │   └── GetAstronomicalInfo.java
│   ├── services/                # ビジネスロジック
│   │   ├── TimeService.java
│   │   ├── CalendarService.java
│   │   └── AstronomyService.java
│   ├── api/                     # 外部API統合
│   │   ├── WorldTimeAPIClient.java
│   │   ├── TimeZoneDBClient.java
│   │   └── HolidayAPIClient.java
│   └── models/                  # データモデル
│       ├── TimeInfo.java
│       ├── CalendarInfo.java
│       └── AstronomicalData.java
├── src/main/resources/
│   ├── application.properties   # 設定ファイル
│   └── timezone-data/          # タイムゾーンデータ
└── src/test/java/              # テストコード
```

## アーキテクチャ概要

### MCPサーバー実装
- `MCPServer`: WebSocketまたはHTTPでMCPプロトコルを処理
- `ToolRegistry`: 利用可能なツールの登録と管理
- 各ツールはインターフェースを実装し、自動登録される

### 外部API統合
- **WorldTimeAPI**: 現在時刻とタイムゾーン情報
- **TimeZoneDB**: 詳細なタイムゾーンデータベース
- **HolidayAPI**: 祝日・記念日情報
- **Astronomy API**: 天文学的時間情報

### キャッシング戦略
- 静的データ（タイムゾーンルール）: 24時間キャッシュ
- 準静的データ（祝日情報）: 7日間キャッシュ
- 動的データ（現在時刻）: キャッシュなし

## 開発ガイドライン

### コーディング規約
- Java 21の新機能を積極的に活用（Records、Pattern Matching、Virtual Threads）
- 日付時刻処理は`java.time`APIを使用
- 宗教的カレンダーは`ICU4J`ライブラリを使用

### エラーハンドリング
- カスタム例外クラスを使用（`TimeServiceException`等）
- 外部API失敗時はフォールバック戦略を実装
- すべてのエラーは適切にログ出力

### テスト方針
- 各ツールメソッドに対する単体テスト必須
- 外部APIはモックを使用
- タイムゾーン境界、DST切り替え等のエッジケースをカバー

## 主要な依存関係

```xml
<!-- MCP実装用 -->
<dependency>
    <groupId>com.github.mcp</groupId>
    <artifactId>mcp-java</artifactId>
    <version>1.0.0</version>
</dependency>

<!-- HTTP通信 -->
<dependency>
    <groupId>com.squareup.okhttp3</groupId>
    <artifactId>okhttp</artifactId>
    <version>4.12.0</version>
</dependency>

<!-- JSON処理 -->
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
    <version>2.16.1</version>
</dependency>

<!-- 国際化カレンダー処理 -->
<dependency>
    <groupId>com.ibm.icu</groupId>
    <artifactId>icu4j</artifactId>
    <version>74.2</version>
</dependency>

<!-- キャッシング -->
<dependency>
    <groupId>com.github.ben-manes.caffeine</groupId>
    <artifactId>caffeine</artifactId>
    <version>3.1.8</version>
</dependency>
```

## 環境変数

```bash
# 必須のAPI キー
WORLDTIME_API_KEY=your_api_key
TIMEZONEDB_API_KEY=your_api_key
HOLIDAY_API_KEY=your_api_key

# オプション設定
MCP_SERVER_PORT=3000
MCP_SERVER_HOST=localhost
CACHE_ENABLED=true
LOG_LEVEL=INFO
```

## デバッグとトラブルシューティング

### ログ設定
```properties
# src/main/resources/logback.xml
logging.level.com.github.yuu1111=DEBUG
logging.level.com.github.yuu1111.api=TRACE
```

### よくある問題と解決方法

1. **API接続エラー**
   - APIキーが正しく設定されているか確認
   - ネットワーク接続を確認
   - APIのレート制限を確認

2. **タイムゾーン変換エラー**
   - IANAタイムゾーン名が正しいか確認
   - タイムゾーンデータが最新か確認

3. **宗教的カレンダー変換エラー**
   - ICU4Jライブラリのバージョン確認
   - ロケール設定の確認

## 実装優先順位

1. **Phase 1**: 基本的な時間取得機能
   - 現在時刻の取得
   - タイムゾーン変換
   - 基本的なMCPサーバー実装

2. **Phase 2**: 高度な機能
   - サマータイム対応
   - 宗教的カレンダー対応
   - 天文学的情報

3. **Phase 3**: 最適化と拡張
   - キャッシング実装
   - パフォーマンス最適化
   - 追加API統合

## コントリビューション

新機能追加時は以下を確認：
- 仕様書（SPECIFICATION.md）との整合性
- 単体テストの追加
- ドキュメントの更新
- コードレビューの実施