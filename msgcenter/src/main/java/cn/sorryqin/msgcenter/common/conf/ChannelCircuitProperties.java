package cn.sorryqin.msgcenter.common.conf;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 渠道熔断：连续失败阈值、熔断时长、降级目标渠道（模板渠道 id -> 备用渠道 id）。
 */
@Component
@ConfigurationProperties(prefix = "send-msg-conf.circuit")
public class ChannelCircuitProperties {

    /** 同一渠道连续失败多少次后熔断 */
    private int failureThreshold = 5;

    /** 熔断持续时间（秒），之后重新尝试原渠道 */
    private int openSeconds = 300;

    /**
     * 原渠道 -> 降级渠道（与 {@link cn.sorryqin.msgcenter.enums.ChannelEnum} 中 channel 值一致）
     * 例：1=邮件 -> 3=飞书
     */
    private Map<String, Integer> fallback = new HashMap<>();

    public int getFailureThreshold() {
        return failureThreshold;
    }

    public void setFailureThreshold(int failureThreshold) {
        this.failureThreshold = failureThreshold;
    }

    public int getOpenSeconds() {
        return openSeconds;
    }

    public void setOpenSeconds(int openSeconds) {
        this.openSeconds = openSeconds;
    }

    public Map<String, Integer> getFallback() {
        return fallback;
    }

    public void setFallback(Map<String, Integer> fallback) {
        this.fallback = fallback != null ? fallback : new HashMap<>();
    }

    public Integer getFallbackChannel(int channel) {
        if (fallback == null) {
            return null;
        }
        Integer v = fallback.get(String.valueOf(channel));
        if (v != null) {
            return v;
        }
        return fallback.get(Integer.toString(channel));
    }
}
