package cn.iocoder.yudao.framework.common.event;

import org.springframework.context.ApplicationEvent;

/**
 * 基础应用事件
 * 
 * @author ruoyi-vue-pro
 */
public abstract class BaseApplicationEvent extends ApplicationEvent {

    /**
     * 事件时间戳
     */
    private final long eventTimestamp;

    public BaseApplicationEvent(Object source) {
        super(source);
        this.eventTimestamp = System.currentTimeMillis();
    }

    public long getEventTimestamp() {
        return eventTimestamp;
    }
} 