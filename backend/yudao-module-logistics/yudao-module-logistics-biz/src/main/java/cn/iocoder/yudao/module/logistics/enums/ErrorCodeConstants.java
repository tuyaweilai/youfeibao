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
    ErrorCode TRANSPORT_TASK_NO_DUPLICATE = new ErrorCode(1_030_200_001, "运输任务单号已存在");
    ErrorCode TRANSPORT_TASK_STATUS_NOT_ALLOW_UPDATE = new ErrorCode(1_030_200_002, "运输任务当前状态不允许该操作");
    ErrorCode TRANSPORT_TASK_CANCEL_REASON_REQUIRED = new ErrorCode(1_030_200_003, "取消运输任务必须填原因");
    ErrorCode TRANSPORT_TASK_ASSIGN_REQUIRED = new ErrorCode(1_030_200_004, "派车必须同时指定车辆与司机");
    ErrorCode TRANSPORT_TASK_VEHICLE_NOT_AVAILABLE = new ErrorCode(1_030_200_005, "车辆当前不可派（维护中或已在其它任务上）");
    ErrorCode TRANSPORT_TASK_DRIVER_NOT_ACTIVE = new ErrorCode(1_030_200_006, "司机不在职，不能派车");

    // ========== 车辆 1-030-201-000 ==========
    ErrorCode VEHICLE_NOT_EXISTS = new ErrorCode(1_030_201_000, "车辆不存在");
    ErrorCode VEHICLE_PLATE_NO_DUPLICATE = new ErrorCode(1_030_201_001, "车牌号已存在");
    ErrorCode VEHICLE_STATUS_NOT_ALLOW_UPDATE = new ErrorCode(1_030_201_002, "车辆状态不允许修改");

    // ========== 司机 1-030-202-000 ==========
    ErrorCode DRIVER_NOT_EXISTS = new ErrorCode(1_030_202_000, "司机不存在");
    ErrorCode DRIVER_USER_DUPLICATE = new ErrorCode(1_030_202_001, "该用户已建过司机档案");

    // ========== 运输节点 1-030-203-000 ==========
    ErrorCode TRANSPORT_NODE_TYPE_NOT_SUPPORTED_YET =
            new ErrorCode(1_030_203_000, "该节点类型本期还不支持上报（五类节点与异常见 #71）");
    ErrorCode TRANSPORT_NODE_CLIENT_REQUEST_REQUIRED =
            new ErrorCode(1_030_203_001, "上报节点必须带客户端请求号（幂等键）");
    ErrorCode TRANSPORT_NODE_TIME_REQUIRED =
            new ErrorCode(1_030_203_002, "节点发生时间不能为空");

}
