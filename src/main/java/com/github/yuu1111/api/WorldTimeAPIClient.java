package com.github.yuu1111.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * WorldTimeAPI クライアント
 * <a href="http://worldtimeapi.org/">...</a> との連携
 */
public class WorldTimeAPIClient {
    private static final Logger logger = LoggerFactory.getLogger(WorldTimeAPIClient.class);
    
    private static final String BASE_URL = "http://worldtimeapi.org/api";
    private static final int DEFAULT_TIMEOUT = 5000; // 5秒
    private static final int MAX_RETRIES = 3;
    private static final long CACHE_TTL = 60000; // 1分
    
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Map<String, CachedResponse> cache;
    private final boolean cacheEnabled;
    
    /**
     * コンストラクタ
     */
    public WorldTimeAPIClient() {
        this(true);
    }
    
    /**
     * コンストラクタ（キャッシュ設定付き）
     */
    public WorldTimeAPIClient(boolean enableCache) {
        this.httpClient = createHttpClient();
        this.objectMapper = createObjectMapper();
        this.cache = new ConcurrentHashMap<>();
        this.cacheEnabled = enableCache;
        
        // キャッシュクリーンアップタスクを開始
        if (cacheEnabled) {
            startCacheCleanup();
        }
    }
    
    /**
     * HTTPクライアントを作成
     */
    private OkHttpClient createHttpClient() {
        return new OkHttpClient.Builder()
            .connectTimeout(DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS)
            .readTimeout(DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS)
            .writeTimeout(DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS)
            .connectionPool(new ConnectionPool(5, 5, TimeUnit.MINUTES))
            .addInterceptor(new RetryInterceptor(MAX_RETRIES))
            .addInterceptor(new LoggingInterceptor())
            .build();
    }
    
    /**
     * ObjectMapperを作成
     */
    private ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }
    
    /**
     * 現在時刻を取得
     */
    public CompletableFuture<TimeInfo> getCurrentTime(String timezone) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return getTimeSync(timezone);
            } catch (IOException e) {
                throw new RuntimeException(
                    "Failed to get time for timezone: %s".formatted(timezone), e);
            }
        });
    }
    
    /**
     * 同期的に時刻を取得
     */
    public TimeInfo getTimeSync(String timezone) throws IOException {
        // キャッシュチェック
        if (cacheEnabled) {
            CachedResponse cached = cache.get(timezone);
            if (cached != null && !cached.isExpired()) {
                logger.debug("Cache hit for timezone: {}", timezone);
                return cached.data;
            }
        }
        
        // APIコール
        String url = String.format("%s/timezone/%s", BASE_URL, timezone);
        Request request = new Request.Builder()
            .url(url)
            .get()
            .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                handleErrorResponse(response);
            }
            
            String body = response.body().string();
            Map<String, Object> data = objectMapper.readValue(body,
                new TypeReference<>() {
                });
            
            TimeInfo timeInfo = parseTimeInfo(data);
            
            // キャッシュに保存
            if (cacheEnabled) {
                cache.put(timezone, new CachedResponse(timeInfo));
            }
            
            return timeInfo;
        }
    }
    
    /**
     * 利用可能なタイムゾーンのリストを取得
     */
    public CompletableFuture<List<String>> getAvailableTimezones() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return getTimezonesSync();
            } catch (IOException e) {
                throw new RuntimeException("Failed to get available timezones", e);
            }
        });
    }
    
    /**
     * 同期的にタイムゾーンリストを取得
     */
    public List<String> getTimezonesSync() throws IOException {
        String url = "%s/timezone".formatted(BASE_URL);
        Request request = new Request.Builder()
            .url(url)
            .get()
            .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                handleErrorResponse(response);
            }
            
            String body = response.body().string();

          return objectMapper.readValue(body,
              new TypeReference<>() {
              });
        }
    }
    
    /**
     * IPアドレスから時刻を取得
     */
    public CompletableFuture<TimeInfo> getTimeByIP(String ipAddress) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String url = String.format("%s/ip/%s", BASE_URL, ipAddress);
                return fetchTimeInfo(url);
            } catch (IOException e) {
                throw new RuntimeException("Failed to get time for IP: %s".formatted(ipAddress), e);
            }
        });
    }
    
    /**
     * 時刻情報を取得
     */
    private TimeInfo fetchTimeInfo(String url) throws IOException {
        Request request = new Request.Builder()
            .url(url)
            .get()
            .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                handleErrorResponse(response);
            }
            
            String body = response.body().string();
            Map<String, Object> data = objectMapper.readValue(body,
                new TypeReference<>() {
                });
            
            return parseTimeInfo(data);
        }
    }
    
    /**
     * TimeInfo をパース
     */
    private TimeInfo parseTimeInfo(Map<String, Object> data) {
        return new TimeInfo(
            (String) data.get("timezone"),
            (String) data.get("datetime"),
            (String) data.get("utc_datetime"),
            (String) data.get("utc_offset"),
            ((Number) data.get("unixtime")).longValue(),
            (Boolean) data.get("dst"),
            (Integer) data.get("dst_offset"),
            (String) data.get("dst_from"),
            (String) data.get("dst_until"),
            (Integer) data.get("raw_offset"),
            (String) data.get("abbreviation"),
            (Integer) data.get("week_number"),
            (Integer) data.get("day_of_week"),
            (Integer) data.get("day_of_year"),
            (String) data.get("client_ip")
        );
    }
    
    /**
     * エラーレスポンスを処理
     */
    private void handleErrorResponse(Response response) throws IOException {
        String errorBody = response.body() != null ? response.body().string() : "No error body";
        
        switch (response.code()) {
            case 404:
                throw new IOException("Timezone not found: %s".formatted(errorBody));
            case 429:
                throw new IOException("Rate limit exceeded");
            case 500:
            case 502:
            case 503:
                throw new IOException("Server error: %d".formatted(response.code()));
            default:
                throw new IOException(
                    "Unexpected response: %d - %s".formatted(response.code(), errorBody));
        }
    }
    
    /**
     * キャッシュクリーンアップタスクを開始
     */
    private void startCacheCleanup() {
        Thread cleanupThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(60000); // 1分ごと
                    cleanupCache();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "WorldTimeAPI-Cache-Cleanup");
        cleanupThread.setDaemon(true);
        cleanupThread.start();
    }
    
    /**
     * 期限切れキャッシュをクリーンアップ
     */
    private void cleanupCache() {
        cache.entrySet().removeIf(entry -> entry.getValue().isExpired());
        logger.debug("Cache cleanup completed. Current size: {}", cache.size());
    }
    
    /**
     * キャッシュをクリア
     */
    public void clearCache() {
        cache.clear();
    }
    
    /**
     * クライアントをシャットダウン
     */
    public void shutdown() {
        httpClient.dispatcher().executorService().shutdown();
        httpClient.connectionPool().evictAll();
    }
    
    /**
     * 時刻情報クラス
     */
    public static class TimeInfo {
        public final String timezone;
        public final String datetime;
        public final String utcDatetime;
        public final String utcOffset;
        public final long unixtime;
        public final boolean dst;
        public final Integer dstOffset;
        public final String dstFrom;
        public final String dstUntil;
        public final Integer rawOffset;
        public final String abbreviation;
        public final Integer weekNumber;
        public final Integer dayOfWeek;
        public final Integer dayOfYear;
        public final String clientIp;
        
        public TimeInfo(String timezone, String datetime, String utcDatetime, 
                       String utcOffset, long unixtime, Boolean dst, Integer dstOffset,
                       String dstFrom, String dstUntil, Integer rawOffset, 
                       String abbreviation, Integer weekNumber, Integer dayOfWeek,
                       Integer dayOfYear, String clientIp) {
            this.timezone = timezone;
            this.datetime = datetime;
            this.utcDatetime = utcDatetime;
            this.utcOffset = utcOffset;
            this.unixtime = unixtime;
            this.dst = dst != null ? dst : false;
            this.dstOffset = dstOffset;
            this.dstFrom = dstFrom;
            this.dstUntil = dstUntil;
            this.rawOffset = rawOffset;
            this.abbreviation = abbreviation;
            this.weekNumber = weekNumber;
            this.dayOfWeek = dayOfWeek;
            this.dayOfYear = dayOfYear;
            this.clientIp = clientIp;
        }
        
        public ZonedDateTime toZonedDateTime() {
            return ZonedDateTime.parse(datetime);
        }
        
        @Override
        public String toString() {
            return String.format("TimeInfo{timezone='%s', datetime='%s', dst=%s}", 
                timezone, datetime, dst);
        }
    }
    
    /**
     * キャッシュされたレスポンス
     */
    private static class CachedResponse {
        final TimeInfo data;
        final long timestamp;
        
        CachedResponse(TimeInfo data) {
            this.data = data;
            this.timestamp = System.currentTimeMillis();
        }
        
        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_TTL;
        }
    }
    
    /**
     * リトライインターセプター
     */
    private static class RetryInterceptor implements Interceptor {
        private final int maxRetries;
        
        RetryInterceptor(int maxRetries) {
            this.maxRetries = maxRetries;
        }
        
        @Override
        public Response intercept(Chain chain) throws IOException {
            Request request = chain.request();
            Response response = null;
            IOException lastException = null;
            
            for (int i = 0; i <= maxRetries; i++) {
                try {
                    if (response != null) {
                        response.close();
                    }
                    response = chain.proceed(request);
                    
                    if (response.isSuccessful()) {
                        return response;
                    }
                    
                    // 4xx エラーはリトライしない
                    if (response.code() >= 400 && response.code() < 500) {
                        return response;
                    }
                    
                } catch (IOException e) {
                    lastException = e;
                    if (i < maxRetries) {
                        try {
                            Thread.sleep((long) Math.pow(2, i) * 1000); // Exponential backoff
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw new IOException("Retry interrupted", ie);
                        }
                    }
                }
            }
            
            if (lastException != null) {
                throw lastException;
            }
            
            return response;
        }
    }
    
    /**
     * ロギングインターセプター
     */
    private static class LoggingInterceptor implements Interceptor {
        private static final Logger logger = LoggerFactory.getLogger(LoggingInterceptor.class);
        
        @Override
        public Response intercept(Chain chain) throws IOException {
            Request request = chain.request();
            long startTime = System.nanoTime();
            
            logger.debug("Sending request: {} {}", request.method(), request.url());
            
            Response response = chain.proceed(request);
            
            long endTime = System.nanoTime();
            long duration = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);
            
            logger.debug("Received response: {} {} in {}ms", 
                response.code(), request.url(), duration);
            
            return response;
        }
    }
}