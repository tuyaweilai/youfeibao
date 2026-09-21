package cn.iocoder.yudao.module.icbc.service.publicapi.impl;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;

import java.util.function.Supplier;

/**
 * 公开端点里「显式切到令牌解析出的租户下执行」的共用实现（#95）。
 *
 * <p>公开端点没有登录态、没有租户头，令牌解析出 {@code tenantId} 后必须显式把上下文切过去，
 * 用完再还原。{@code PublicAccessServiceImpl} 与 {@code PublicEsignServiceImpl} 以前各自
 * 抄了一份逐字相同的 {@code inTenant}，这里收成一处。
 *
 * <p><b>为什么不用框架的 {@code TenantUtils.execute(tenantId, Callable)}</b>：那个重载把
 * 受检异常包成 {@code RuntimeException} 之前，会先用 {@code catch (Exception e)} 接住
 * {@code ServiceException} 一并包掉，业务错误码就丢了（全局异常处理器只认原始的
 * {@code ServiceException}，包一层只会回 500）。这两个公开端点内部大量抛业务错误
 * （协议不是待签署、文件找不到……），必须让异常原样上抛，所以这里只做上下文切换、不碰异常。
 */
final class TenantCalls {

    private TenantCalls() {
    }

    static void execute(Long tenantId, Runnable runnable) {
        execute(tenantId, () -> {
            runnable.run();
            return null;
        });
    }

    static <T> T execute(Long tenantId, Supplier<T> supplier) {
        Long oldTenantId = TenantContextHolder.getTenantId();
        Boolean oldIgnore = TenantContextHolder.isIgnore();
        TenantContextHolder.setTenantId(tenantId);
        TenantContextHolder.setIgnore(false);
        try {
            return supplier.get();
        } finally {
            TenantContextHolder.setTenantId(oldTenantId);
            TenantContextHolder.setIgnore(oldIgnore);
        }
    }

}
