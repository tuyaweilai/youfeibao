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
    ErrorCode TRANSPORT_TASK_NOT_BELONG_TO_DRIVER = new ErrorCode(1_030_200_007, "这不是派给你的任务");
    ErrorCode TRANSPORT_TASK_OVERRIDE_REASON_REQUIRED = new ErrorCode(1_030_200_008, "授权放行必须填原因");
    ErrorCode TRANSPORT_TASK_OVERRIDE_NOT_APPLICABLE = new ErrorCode(1_030_200_009, "该情况不能授权放行（车辆维修中、司机离职这类硬门禁不可绕过）");
    ErrorCode TRANSPORT_TASK_REASSIGN_REASON_REQUIRED = new ErrorCode(1_030_200_010, "改派必须填原因");
    ErrorCode TRANSPORT_TASK_REASSIGN_NOT_ALLOWED = new ErrorCode(1_030_200_011, "当前状态不允许改派（只有已分配 / 已接单 / 执行中可以改派）");

    // ========== 车辆 1-030-201-000 ==========
    ErrorCode VEHICLE_NOT_EXISTS = new ErrorCode(1_030_201_000, "车辆不存在");
    ErrorCode VEHICLE_PLATE_NO_DUPLICATE = new ErrorCode(1_030_201_001, "车牌号已存在");
    ErrorCode VEHICLE_STATUS_NOT_ALLOW_UPDATE = new ErrorCode(1_030_201_002, "车辆状态不允许修改");
    /** 软门禁：可授权放行（证件过期） */
    ErrorCode VEHICLE_DOCUMENT_EXPIRED = new ErrorCode(1_030_201_100, "车辆证件（行驶证 / 保险）已过期，不能派车；确需派出请由管理员带原因授权放行");

    // ========== 司机 1-030-202-000 ==========
    ErrorCode DRIVER_NOT_EXISTS = new ErrorCode(1_030_202_000, "司机不存在");
    ErrorCode DRIVER_USER_DUPLICATE = new ErrorCode(1_030_202_001, "该用户已建过司机档案");
    ErrorCode DRIVER_PROFILE_NOT_FOUND = new ErrorCode(1_030_202_002, "当前登录账号还不是司机，请让管理员建司机档案");
    /** 软门禁：可授权放行（证件过期） */
    ErrorCode DRIVER_DOCUMENT_EXPIRED = new ErrorCode(1_030_202_100, "司机证件（驾驶证 / 从业资格证）已过期，不能派车；确需派出请由管理员带原因授权放行");
    /** 硬门禁：不可用授权绕过 */
    ErrorCode DRIVER_CARRIER_REQUIRED = new ErrorCode(1_030_202_101, "来源为承运商的司机必须选择所属承运商");

    // ========== 承运商 1-030-205-000 ==========
    ErrorCode CARRIER_NOT_EXISTS = new ErrorCode(1_030_205_000, "承运商不存在");
    ErrorCode CARRIER_NAME_DUPLICATE = new ErrorCode(1_030_205_001, "承运商名称已存在");
    ErrorCode CARRIER_NOT_ACTIVE = new ErrorCode(1_030_205_002, "承运商已停用");

    // ========== 运输节点 1-030-203-000 ==========
    ErrorCode TRANSPORT_NODE_CLIENT_REQUEST_REQUIRED =
            new ErrorCode(1_030_203_001, "上报节点必须带客户端请求号（幂等键）");
    ErrorCode TRANSPORT_NODE_TIME_REQUIRED =
            new ErrorCode(1_030_203_002, "节点发生时间不能为空");
    ErrorCode TRANSPORT_NODE_TYPE_REQUIRED =
            new ErrorCode(1_030_203_003, "节点类型不能为空");
    ErrorCode TRANSPORT_NODE_TYPE_UNKNOWN =
            new ErrorCode(1_030_203_004, "未知的节点类型");
    ErrorCode TRANSPORT_NODE_TASK_NOT_REPORTABLE =
            new ErrorCode(1_030_203_005, "待分配或已取消的任务不能上报运输节点");

    // ========== 运输节点：照片必填与异常（V4 #71，段位 1_030_203_1xx） ==========
    /** 交接完成与卸货完成是货物流的关键凭证，必须有照片 */
    ErrorCode TRANSPORT_NODE_PHOTO_REQUIRED =
            new ErrorCode(1_030_203_100, "「交接完成」与「卸货完成」必须上传照片（货物流凭证）");
    ErrorCode TRANSPORT_ABNORMAL_TYPE_UNKNOWN =
            new ErrorCode(1_030_203_101, "未知的异常类型");
    ErrorCode TRANSPORT_ABNORMAL_REASON_REQUIRED =
            new ErrorCode(1_030_203_102, "上报异常必须填说明");
    ErrorCode TRANSPORT_ABNORMAL_NOT_EXISTS =
            new ErrorCode(1_030_203_103, "该异常记录不存在");
    ErrorCode TRANSPORT_ABNORMAL_ALREADY_RESOLVED =
            new ErrorCode(1_030_203_104, "该异常已经解决过了");

}
