package cn.iocoder.yudao.module.logistics.enums;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;

/**
 * 物流模块错误码。
 *
 * <p>物流模块使用 1-030-200-xxx 段：icbc 占 1-030-001~050，erp 占 1-030-100~1xx，
 * 已停编的 vendored 模块（`backend/legacy/`）不再占用号段。段位划分见 `docs/agents/handoff.md` 的并行约定。
 *
 * <p>域内再分段：运输任务 1-030-200、车辆 1-030-201、司机 1-030-202，每域 1000 个号。
 */
public interface ErrorCodeConstants {

    // ========== 运输任务 1-030-200-000 ==========
    ErrorCode TRANSPORT_TASK_NOT_EXISTS = new ErrorCode(1_030_200_000, "运输任务不存在");

    // ========== 车辆 1-030-201-000 ==========
    ErrorCode VEHICLE_NOT_EXISTS = new ErrorCode(1_030_201_000, "车辆不存在");
    ErrorCode VEHICLE_PLATE_NO_DUPLICATE = new ErrorCode(1_030_201_001, "车牌号已存在");
    ErrorCode VEHICLE_STATUS_NOT_ALLOW_UPDATE = new ErrorCode(1_030_201_002, "车辆状态不允许修改");

    // ========== 司机 1-030-202-000 ==========
    ErrorCode DRIVER_NOT_EXISTS = new ErrorCode(1_030_202_000, "司机不存在");
    ErrorCode DRIVER_USER_DUPLICATE = new ErrorCode(1_030_202_001, "该用户已建过司机档案");

}
