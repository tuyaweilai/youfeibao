package cn.iocoder.yudao.module.icbc.service.station;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static cn.iocoder.yudao.framework.common.exception.enums.GlobalErrorCodeConstants.TOO_MANY_REQUESTS;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;

/**
 * 场站公开信息读取限流（#34）。
 *
 * <p>免登录的场站查询不带令牌，任何人都可能扫同一个码；用**按 IP 的固定窗口**兜住滥用。
 * 一期是单实例部署，进程内计数即可，不引 Redis 依赖，避免为一个只读公开接口引入分布式状态。
 *
 * <p>放在这里而不是 {@code @RateLimiter}：那个注解依赖 protection starter 与 Redis，
 * 本模块没有引入；且公开信息不值得为限流引入额外的运行时依赖。
 */
@Component
public class StationResolveRateLimiter {

    /** 每个窗口允许的次数 */
    @Value("${icbc.station.resolve-per-minute:60}")
    private int permitsPerMinute;

    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    /**
     * 允许则消耗一次；超限抛 {@code TOO_MANY_REQUESTS}。
     */
    public void assertAllowed(String key) {
        if (!tryAcquire(key)) {
            throw exception(TOO_MANY_REQUESTS);
        }
    }

    /**
     * 尝试消耗一次。测试可直接断言布尔值。
     */
    public boolean tryAcquire(String key) {
        long now = System.currentTimeMillis();
        String safeKey = key == null ? "unknown" : key;
        Window window = windows.compute(safeKey, (k, existing) -> {
            if (existing == null || now - existing.startMillis >= 60_000L) {
                return new Window(now);
            }
            return existing;
        });
        // 窗口滚动时清理过期桶，避免 IP 无限增长（只做粗粒度清理，不做严格 LRU）
        if (windows.size() > 10_000) {
            windows.entrySet().removeIf(entry -> now - entry.getValue().startMillis >= 60_000L);
        }
        return window.count.incrementAndGet() <= permitsPerMinute;
    }

    private static final class Window {
        private final long startMillis;
        private final AtomicInteger count = new AtomicInteger(0);

        private Window(long startMillis) {
            this.startMillis = startMillis;
        }
    }

}
