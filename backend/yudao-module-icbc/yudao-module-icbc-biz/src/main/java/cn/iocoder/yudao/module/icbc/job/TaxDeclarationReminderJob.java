package cn.iocoder.yudao.module.icbc.job;

import cn.iocoder.yudao.framework.quartz.core.handler.JobHandler;
import cn.iocoder.yudao.framework.tenant.core.job.TenantJob;
import cn.iocoder.yudao.module.icbc.controller.admin.tax.vo.TaxDeclarationPrecheckRespVO;
import cn.iocoder.yudao.module.icbc.service.tax.TaxDeclarationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 代办税费申报提醒定时任务。
 *
 * <p>每月申报期是次月 15 日。任务每天把<b>上月</b>的申报清单生成 / 刷新一次（数据补充后
 * 重新计算），并统计当前待处理的预警——申报期临近、已经逾期可能被暂停开票资格、数据不齐。
 * 标注 {@link TenantJob}，框架按租户逐个执行。
 *
 * <p>只在当月确有开票时生成：没有业务的月份不造出一张金额为零的申报单，否则预警清单会
 * 被空申报单刷屏。
 */
@Component
@Slf4j
public class TaxDeclarationReminderJob implements JobHandler {

    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    @Resource
    private TaxDeclarationService taxDeclarationService;

    @Override
    @TenantJob
    public String execute(String param) {
        String lastMonth = LocalDate.now().minusMonths(1).format(MONTH_FORMATTER);
        TaxDeclarationPrecheckRespVO precheck = taxDeclarationService.precheck(lastMonth);
        boolean hasBusiness = precheck.getSellerCount() != null && precheck.getSellerCount() > 0;
        if (hasBusiness) {
            taxDeclarationService.generate(lastMonth);
        }
        int warnings = taxDeclarationService.getWarnings().size();
        if (hasBusiness && warnings > 0) {
            log.warn("[execute][{} 申报清单已刷新，当前待处理申报预警 {} 条]", lastMonth, warnings);
        }
        return String.format("上月（%s）%s，当前待处理申报预警 %d 条",
                lastMonth, hasBusiness ? "申报清单已生成" : "无开票业务", warnings);
    }

}
