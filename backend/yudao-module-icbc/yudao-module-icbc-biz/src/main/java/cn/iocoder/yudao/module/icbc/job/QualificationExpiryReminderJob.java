package cn.iocoder.yudao.module.icbc.job;

import cn.iocoder.yudao.framework.quartz.core.handler.JobHandler;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.tenant.core.job.TenantJob;
import cn.iocoder.yudao.module.icbc.service.warning.IcbcExpiryWarningService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 资质到期提醒定时任务。
 *
 * <p>标注 {@link TenantJob}，框架会按租户逐个执行；本方法只需处理「当前租户」。
 * 任务扫描临近到期的三层资质，落一条待处理预警，供开票就绪页提醒管理员。
 * 重复执行幂等：同一资质已有未处理预警时不会重复生成。
 */
@Component
@Slf4j
public class QualificationExpiryReminderJob implements JobHandler {

    /** 默认提前 30 天预警 */
    private static final int DEFAULT_DAYS = 30;

    @Resource
    private IcbcExpiryWarningService expiryWarningService;

    @Override
    @TenantJob
    public String execute(String param) {
        int days = parseDays(param);
        int created = expiryWarningService.scan(days);
        if (created > 0) {
            log.warn("[execute][租户({}) 新增 {} 条资质到期预警（{} 天内）]",
                    TenantContextHolder.getTenantId(), created, days);
        }
        return String.format("扫描完成，新增 %d 条到期预警", created);
    }

    private int parseDays(String param) {
        if (param == null || param.trim().isEmpty()) {
            return DEFAULT_DAYS;
        }
        try {
            return Integer.parseInt(param.trim());
        } catch (NumberFormatException e) {
            log.warn("[parseDays][参数格式错误，使用默认 {} 天: {}]", DEFAULT_DAYS, param);
            return DEFAULT_DAYS;
        }
    }

}
