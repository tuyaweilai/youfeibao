package cn.iocoder.yudao.module.icbc.job;

import cn.iocoder.yudao.framework.quartz.core.handler.JobHandler;
import cn.iocoder.yudao.framework.tenant.core.job.TenantJob;
import cn.iocoder.yudao.module.icbc.service.settlement.SettlementService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 结算确认超时任务（ADR 0018 / 0022）。
 *
 * <p>待确认超过 7 天（可配）时**不自动替自然人确认**，而是升级为「需线下签字确认」并在企业侧生成待办。
 * 异议后企业超时未处理时，自然人侧会显示「企业尚未回复」并给留联系方式的口子。
 */
@Component
@Slf4j
public class SettlementTimeoutJob implements JobHandler {

    @Resource
    private SettlementService settlementService;

    @Override
    @TenantJob
    public String execute(String param) {
        int escalated = settlementService.handleTimeout();
        return String.format("结算确认超时：升级为需线下签字确认 %d 张", escalated);
    }

}
