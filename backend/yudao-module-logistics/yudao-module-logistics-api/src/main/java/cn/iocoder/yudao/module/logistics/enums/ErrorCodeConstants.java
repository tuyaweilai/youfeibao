package cn.iocoder.yudao.module.logistics.enums;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;

/**
 * 物流模块错误码。
 *
 * <p>物流模块使用 1-030-200-xxx 段：icbc 占 1-030-001~050，erp 占 1-030-100~1xx，
 * 已停编的 vendored 模块（`backend/legacy/`）不再占用号段。段位划分见 `docs/agents/handoff.md` 的并行约定。
 */
public interface ErrorCodeConstants {

    // ========== 运输任务 1-030-200-000 ==========
    ErrorCode TRANSPORT_TASK_NOT_EXISTS = new ErrorCode(1_030_200_000, "运输任务不存在");

}
