package cn.sorryqin.msgcenter.tools;

import cn.sorryqin.common.redis.RedisBase;
import cn.sorryqin.msgcenter.common.conf.ChannelCircuitProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

/**
 * 多云容灾：按「实际使用的渠道」维护 Redis 连续失败计数；达阈值则熔断并在 TTL 内降级到备用渠道。
 */
@Service
public class ChannelCircuitBreakerService {

    private static final Logger log = LoggerFactory.getLogger(ChannelCircuitBreakerService.class);

    private static final String KEY_FAIL = "msgcenter:circuit:channel:fail:";
    private static final String KEY_OPEN = "msgcenter:circuit:channel:open:";

    @Autowired
    private RedisBase redisBase;

    @Autowired
    private ChannelCircuitProperties circuitProperties;

    public boolean isCircuitOpen(int channel) {
        return redisBase.hasKey(KEY_OPEN + channel);
    }

    /**
     * 从模板渠道出发，若熔断则沿 fallback 链解析，直到未熔断或无法继续降级。
     */
    public int resolveEffectiveChannel(int templateChannel) {
        int c = templateChannel;
        Set<Integer> visited = new HashSet<>();
        final int maxHops = 8;
        int hops = 0;
        while (isCircuitOpen(c) && hops < maxHops) {
            if (!visited.add(c)) {
                log.warn("channel circuit fallback cycle detected, channel={}", c);
                break;
            }
            Integer next = circuitProperties.getFallbackChannel(c);
            if (next == null || next == c) {
                log.warn("channel {} is open but no fallback configured, still use it", c);
                break;
            }
            log.warn("channel {} circuit open, degrading send to channel {}", c, next);
            c = next;
            hops++;
        }
        return c;
    }

    /** 该渠道一次发送成功：连续失败计数清零 */
    public void recordSuccess(int channel) {
        redisBase.del(KEY_FAIL + channel);
    }

    /**
     * 该渠道一次发送失败：连续失败 +1；达到阈值则设置熔断键并带 TTL，并清零失败计数键。
     */
    public void recordFailure(int channel) {
        int threshold = circuitProperties.getFailureThreshold();
        int openSec = circuitProperties.getOpenSeconds();
        if (threshold <= 0 || openSec <= 0) {
            return;
        }
        String failKey = KEY_FAIL + channel;
        long n = redisBase.incr(failKey, 1);
        log.warn("channel {} send failed, consecutive failures={}/{}", channel, n, threshold);
        if (n >= threshold) {
            String openKey = KEY_OPEN + channel;
            redisBase.set(openKey, "1", openSec);
            redisBase.del(failKey);
            log.error("channel {} circuit OPEN for {}s after {} consecutive failures", channel, openSec, n);
        }
    }
}
