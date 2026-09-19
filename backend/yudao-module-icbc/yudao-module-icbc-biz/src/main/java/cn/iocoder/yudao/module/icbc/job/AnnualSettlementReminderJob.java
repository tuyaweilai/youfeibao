package cn.iocoder.yudao.module.icbc.job;

import cn.iocoder.yudao.framework.quartz.core.handler.JobHandler;
import cn.iocoder.yudao.framework.tenant.core.job.TenantJob;
import cn.iocoder.yudao.module.icbc.service.tax.AnnualSettlementService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 出售者汇算清缴提醒定时任务。
 *
 * <p>出售者须在次年 3 月 31 日前自行汇算清缴。任务每月为「当年有开票记录」的出售者生成
 * 提醒与对账单，标注 {@link TenantJob} 按租户逐个执行。重复执行幂等：同一出售者同一年度
 * 只生成一条。
 */
@Component
@Slf4j
public class AnnualSettlementReminderJob implements JobHandler {

    @Resource
    private AnnualSettlementService annualSettlementService;

    @Override
    @TenantJob
    public String execute(String param) {
        int taxYear = parseTaxYear(param);
        int created = annualSettlementService.remind(taxYear);
        return String.format("纳税年度 %d：新增 %d 条汇算清缴提醒", taxYear, created);
    }

    private int parseTaxYear(String param) {
        if (param == null || param.trim().isEmpty()) {
            return annualSettlementService.currentTaxYear();
        }
        try {
            return Integer.parseInt(param.trim());
        } catch (NumberFormatException e) {
            log.warn("[parseTaxYear][参数格式错误，使用当前应提醒年度: {}]", param);
            return annualSettlementService.currentTaxYear();
        }
    }

}
