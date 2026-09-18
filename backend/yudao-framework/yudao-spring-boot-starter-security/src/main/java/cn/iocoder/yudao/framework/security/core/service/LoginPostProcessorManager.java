package cn.iocoder.yudao.framework.security.core.service;

import cn.iocoder.yudao.framework.common.event.auth.LoginEvent;
import cn.iocoder.yudao.framework.common.extension.auth.LoginPostProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

/**
 * 登录后处理器管理器
 * 
 * @author ruoyi-vue-pro
 */
@Component
@Slf4j
public class LoginPostProcessorManager {

    private final List<LoginPostProcessor> processors = new ArrayList<>();

    @Autowired(required = false)
    private List<LoginPostProcessor> springProcessors;

    @PostConstruct
    public void init() {
        // 1. 加载 Spring 容器中的处理器
        if (springProcessors != null) {
            processors.addAll(springProcessors);
        }

        // 2. 使用 SPI 机制加载处理器
        ServiceLoader<LoginPostProcessor> serviceLoader = ServiceLoader.load(LoginPostProcessor.class);
        for (LoginPostProcessor processor : serviceLoader) {
            if (processor.isEnabled()) {
                processors.add(processor);
                log.info("加载登录后处理器: {}", processor.getName());
            }
        }

        // 3. 按优先级排序
        processors.sort((p1, p2) -> Integer.compare(p2.getOrder(), p1.getOrder()));
        
        log.info("共加载 {} 个登录后处理器", processors.size());
    }

    /**
     * 处理登录事件
     */
    public void processLogin(LoginEvent event) {
        for (LoginPostProcessor processor : processors) {
            try {
                if (processor.supports(event.getUserType())) {
                    boolean continueProcess = processor.process(event);
                    log.debug("处理器 {} 处理登录事件, 继续执行: {}", processor.getName(), continueProcess);
                    
                    if (!continueProcess) {
                        log.info("处理器 {} 中断了后续处理器的执行", processor.getName());
                        break;
                    }
                }
            } catch (Exception e) {
                log.error("登录后处理器 {} 执行失败", processor.getName(), e);
                // 继续执行后续处理器，不因为单个处理器失败而中断
            }
        }
    }

    /**
     * 增强登录响应
     * @param respObj 登录响应对象
     * @param event 登录事件
     */
    public void enhanceResponse(Object respObj, LoginEvent event) {
        for (LoginPostProcessor processor : processors) {
            try {
                if (processor.supports(event.getUserType())) {
                    processor.enhanceResponse(respObj, event);
                }
            } catch (Exception e) {
                log.error("登录后处理器 {} 增强响应失败", processor.getName(), e);
            }
        }
    }
} 