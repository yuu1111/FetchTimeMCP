# Phase 3: 最適化と拡張

## 📋 概要
パフォーマンス最適化、追加機能実装、プロダクション対応を行う最終フェーズです。

**期間**: 3-5日  
**優先度**: 低～中

## 🎯 目標
- パフォーマンス最適化
- ビジネス時間計算機能
- 会議スケジューリング機能
- 監視・ロギング強化
- プロダクション環境対応

## 📦 実装タスク

### 1. パフォーマンス最適化

#### 1.1 Virtual Threads対応
```java
package com.github.yuu1111.server;

import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

public class OptimizedMCPServer {
    // Java 21のVirtual Threadsを使用
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    
    public void handleRequest(Socket client) {
        executor.submit(() -> {
            try (client) {
                processRequest(client);
            } catch (Exception e) {
                logger.error("Request handling failed", e);
            }
        });
    }
}
```

#### 1.2 バッチ処理最適化
```java
package com.github.yuu1111.services;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class BatchTimeService {
    
    public CompletableFuture<List<TimeInfo>> getBatchTimes(List<String> timezones) {
        List<CompletableFuture<TimeInfo>> futures = timezones.stream()
            .map(tz -> CompletableFuture.supplyAsync(() -> getTime(tz)))
            .toList();
            
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> futures.stream()
                .map(CompletableFuture::join)
                .toList());
    }
}
```

### 2. ビジネス時間計算

#### 2.1 BusinessHoursCalculator.java
```java
package com.github.yuu1111.services.business;

import java.time.*;
import java.util.List;

public class BusinessHoursCalculator {
    
    public Duration calculateBusinessHours(
            ZonedDateTime start, 
            ZonedDateTime end,
            BusinessHoursConfig config) {
        
        Duration totalDuration = Duration.ZERO;
        ZonedDateTime current = start;
        
        while (current.isBefore(end)) {
            if (isBusinessDay(current, config)) {
                LocalTime businessStart = config.getStartTime();
                LocalTime businessEnd = config.getEndTime();
                
                LocalTime dayStart = maxTime(current.toLocalTime(), businessStart);
                LocalTime dayEnd = minTime(
                    end.toLocalDate().equals(current.toLocalDate()) 
                        ? end.toLocalTime() 
                        : LocalTime.MAX,
                    businessEnd
                );
                
                if (dayStart.isBefore(dayEnd)) {
                    totalDuration = totalDuration.plus(
                        Duration.between(dayStart, dayEnd)
                    );
                }
            }
            
            current = current.plusDays(1).with(LocalTime.MIN);
        }
        
        return totalDuration;
    }
    
    private boolean isBusinessDay(ZonedDateTime date, BusinessHoursConfig config) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        
        // 週末チェック
        if (config.getWeekends().contains(dayOfWeek)) {
            return false;
        }
        
        // 祝日チェック
        if (config.getHolidays().contains(date.toLocalDate())) {
            return false;
        }
        
        return true;
    }
}
```

### 3. 会議スケジューリング

#### 3.1 MeetingScheduler.java
```java
package com.github.yuu1111.services.meeting;

import java.time.*;
import java.util.*;

public class MeetingScheduler {
    
    public List<MeetingSlot> findOptimalMeetingTimes(
            List<Participant> participants,
            Duration meetingDuration,
            LocalDate startDate,
            LocalDate endDate) {
        
        List<MeetingSlot> optimalSlots = new ArrayList<>();
        
        // 各参加者の利用可能時間を取得
        Map<Participant, List<TimeSlot>> availability = 
            getParticipantAvailability(participants, startDate, endDate);
        
        // 共通の利用可能時間を見つける
        List<TimeSlot> commonSlots = findCommonSlots(availability, meetingDuration);
        
        // スコアリングして最適な時間を選択
        for (TimeSlot slot : commonSlots) {
            double score = calculateSlotScore(slot, participants);
            optimalSlots.add(new MeetingSlot(slot, score));
        }
        
        // スコアでソート
        optimalSlots.sort(Comparator.comparing(MeetingSlot::getScore).reversed());
        
        return optimalSlots.stream()
            .limit(5)  // Top 5の時間帯を返す
            .toList();
    }
    
    private double calculateSlotScore(TimeSlot slot, List<Participant> participants) {
        double score = 100.0;
        
        for (Participant participant : participants) {
            ZonedDateTime localTime = slot.getStart()
                .withZoneSameInstant(participant.getTimezone());
            
            LocalTime time = localTime.toLocalTime();
            
            // 早朝・深夜はスコアを下げる
            if (time.isBefore(LocalTime.of(6, 0))) {
                score -= 30;
            } else if (time.isBefore(LocalTime.of(9, 0))) {
                score -= 10;
            } else if (time.isAfter(LocalTime.of(21, 0))) {
                score -= 20;
            } else if (time.isAfter(LocalTime.of(18, 0))) {
                score -= 5;
            }
            
            // ランチタイムを避ける
            if (time.isAfter(LocalTime.of(12, 0)) && 
                time.isBefore(LocalTime.of(13, 0))) {
                score -= 15;
            }
        }
        
        return score / participants.size();
    }
}
```

### 4. 監視とメトリクス

#### 4.1 MetricsCollector.java
```java
package com.github.yuu1111.monitoring;

import io.micrometer.core.instrument.*;

public class MetricsCollector {
    private final MeterRegistry registry;
    private final Counter requestCounter;
    private final Timer responseTimer;
    private final Gauge cacheHitRate;
    
    public MetricsCollector(MeterRegistry registry) {
        this.registry = registry;
        
        this.requestCounter = Counter.builder("mcp.requests.total")
            .description("Total number of MCP requests")
            .register(registry);
            
        this.responseTimer = Timer.builder("mcp.response.time")
            .description("Response time for MCP requests")
            .register(registry);
            
        this.cacheHitRate = Gauge.builder("mcp.cache.hit.rate", this, 
            MetricsCollector::calculateCacheHitRate)
            .description("Cache hit rate")
            .register(registry);
    }
    
    public void recordRequest(String toolName) {
        requestCounter.increment();
        Tag.of("tool", toolName);
    }
    
    public Timer.Sample startTimer() {
        return Timer.start(registry);
    }
    
    public void recordResponse(Timer.Sample sample, String toolName) {
        sample.stop(Timer.builder("mcp.response.time")
            .tag("tool", toolName)
            .register(registry));
    }
}
```

#### 4.2 HealthCheck.java
```java
package com.github.yuu1111.monitoring;

public class HealthCheck {
    
    public HealthStatus check() {
        HealthStatus.Builder builder = HealthStatus.builder();
        
        // API接続チェック
        builder.withDetail("worldTimeAPI", checkWorldTimeAPI());
        builder.withDetail("timeZoneDB", checkTimeZoneDB());
        
        // キャッシュ状態
        builder.withDetail("cacheSize", getCacheSize());
        builder.withDetail("cacheHitRate", getCacheHitRate());
        
        // システムリソース
        builder.withDetail("memoryUsage", getMemoryUsage());
        builder.withDetail("threadCount", getThreadCount());
        
        return builder.build();
    }
}
```

### 5. プロダクション設定

#### 5.1 application-prod.properties
```properties
# サーバー設定
server.port=${MCP_SERVER_PORT:3000}
server.host=${MCP_SERVER_HOST:0.0.0.0}
server.max-connections=100
server.request-timeout=30000

# API設定
worldtime.api.key=${WORLDTIME_API_KEY}
worldtime.api.timeout=5000
worldtime.api.retry-count=3

timezonedb.api.key=${TIMEZONEDB_API_KEY}
timezonedb.api.timeout=5000

# キャッシュ設定
cache.enabled=true
cache.static.size=1000
cache.static.ttl=86400
cache.semi-static.size=500
cache.semi-static.ttl=604800

# ロギング
logging.level.root=WARN
logging.level.com.github.yuu1111=INFO
logging.pattern.console=%d{yyyy-MM-dd HH:mm:ss} - %msg%n
logging.file.name=/var/log/fetchtime-mcp/application.log
logging.file.max-size=10MB
logging.file.max-history=30

# メトリクス
management.endpoints.web.exposure.include=health,metrics,prometheus
management.metrics.export.prometheus.enabled=true
```

#### 5.2 Dockerfile
```dockerfile
FROM eclipse-temurin:21-jre-alpine

RUN apk add --no-cache tzdata

WORKDIR /app

COPY target/FetchTimeMCP-1.0-SNAPSHOT.jar app.jar

ENV JAVA_OPTS="-Xmx512m -XX:+UseG1GC"

EXPOSE 3000

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:3000/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

#### 5.3 docker-compose.yml
```yaml
version: '3.8'

services:
  fetchtime-mcp:
    build: .
    container_name: fetchtime-mcp
    ports:
      - "3000:3000"
    environment:
      - MCP_SERVER_PORT=3000
      - WORLDTIME_API_KEY=${WORLDTIME_API_KEY}
      - TIMEZONEDB_API_KEY=${TIMEZONEDB_API_KEY}
      - HOLIDAY_API_KEY=${HOLIDAY_API_KEY}
      - SPRING_PROFILES_ACTIVE=prod
    volumes:
      - ./logs:/var/log/fetchtime-mcp
    restart: unless-stopped
    networks:
      - mcp-network
      
  prometheus:
    image: prom/prometheus:latest
    container_name: prometheus
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml
      - prometheus-data:/prometheus
    ports:
      - "9090:9090"
    networks:
      - mcp-network
      
  grafana:
    image: grafana/grafana:latest
    container_name: grafana
    ports:
      - "3001:3000"
    volumes:
      - grafana-data:/var/lib/grafana
    networks:
      - mcp-network

networks:
  mcp-network:
    driver: bridge

volumes:
  prometheus-data:
  grafana-data:
```

## 🧪 パフォーマンステスト

### 負荷テスト
```bash
# Apache Benchでの負荷テスト
ab -n 10000 -c 100 -p request.json -T application/json \
  http://localhost:3000/tools/get_current_time

# JMeterテストプラン
jmeter -n -t performance-test.jmx -l results.jtl
```

### ベンチマーク目標
```
- 同時接続数: 100
- レスポンスタイム: p50 < 50ms, p95 < 200ms, p99 < 500ms
- スループット: > 1000 req/s
- エラー率: < 0.1%
- CPU使用率: < 70%
- メモリ使用量: < 512MB
```

## 📊 成功基準

### パフォーマンス
- [ ] レスポンスタイム目標達成
- [ ] スループット目標達成
- [ ] リソース使用量が制限内

### 機能
- [ ] ビジネス時間計算が正確
- [ ] 会議スケジューリングが動作
- [ ] 監視・アラートが機能

### 運用
- [ ] Dockerイメージがビルドできる
- [ ] ヘルスチェックが動作
- [ ] メトリクスが収集される
- [ ] ログが適切に出力される

## 🚀 デプロイ手順

```bash
# ビルド
mvn clean package

# Dockerイメージビルド
docker build -t fetchtime-mcp:latest .

# 環境変数設定
cp .env.example .env
# .envファイルを編集してAPIキーを設定

# 起動
docker-compose up -d

# ログ確認
docker-compose logs -f fetchtime-mcp

# 停止
docker-compose down
```

## 📝 チェックリスト

### 最適化
- [ ] Virtual Threads実装
- [ ] バッチ処理最適化
- [ ] キャッシュ戦略最適化

### 新機能
- [ ] ビジネス時間計算
- [ ] 会議スケジューリング
- [ ] 歴史的日付対応

### 運用準備
- [ ] Docker化
- [ ] 監視設定
- [ ] ロギング設定
- [ ] セキュリティ強化

## 🔗 関連ドキュメント
- [Phase 1: 基本実装](./PHASE1_BASIC.md)
- [Phase 2: 高度な機能](./PHASE2_ADVANCED.md)
- [デプロイガイド](./DEPLOYMENT.md)

---

*Version: 1.0.0*  
*Last Updated: 2024-01-15*