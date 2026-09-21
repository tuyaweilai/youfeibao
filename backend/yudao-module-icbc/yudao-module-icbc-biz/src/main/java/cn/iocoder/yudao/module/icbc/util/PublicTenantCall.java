package cn.iocoder.yudao.module.icbc.util;

import cn.iocoder.yudao.framework.tenant.core.util.TenantUtils;

import java.util.concurrent.Callable;
import java.util.function.Supplier;

/**
 * 公开端点「验令牌 → 切到令牌里的租户执行」的唯一入口（#94 修票 ST-2）。
 *
 * <p>以前 {@code PublicAccessServiceImpl} 与 {@code PublicOnboardingWizardServiceImpl} 各抄了一份一模一样的
 * {@code inTenant}（连注释都逐字相同），第三份随时会出现。这里收成一处，两个 Service 都调它。
 *
 * <p><b>为什么还要包一层，不直接用框架的</b>：{@link TenantUtils#execute(Long, Callable)} 会把异常包成
 * {@code RuntimeException}，业务错误码（{@code ServiceException}）会被吃掉变成 500。这里仍复用框架的
 * 上下文切换（含「强制不忽略租户」与还原），只把被包起来的业务异常原样抛回去。
 */
public final class PublicTenantCall {

    private PublicTenantCall() {
    }

    /** 有返回值：{@code ServiceException} 等 RuntimeException 原样抛出，不被包成 500。 */
    public static <T> T execute(Long tenantId, Supplier<T> supplier) {
        Callable<T> callable = supplier::get; // 显式用 Callable 形参，避免与 Runnable 重载歧义
        try {
            return TenantUtils.execute(tenantId, callable);
        } catch (RuntimeException e) {
            if (e.getCause() instanceof RuntimeException) {
                throw (RuntimeException) e.getCause(); // 还原 ServiceException 的业务错误码
            }
            throw e;
        }
    }

    /** 无返回值：框架的 Runnable 版本本身不包装异常，直接复用。 */
    public static void run(Long tenantId, Runnable runnable) {
        TenantUtils.execute(tenantId, runnable);
    }

}
