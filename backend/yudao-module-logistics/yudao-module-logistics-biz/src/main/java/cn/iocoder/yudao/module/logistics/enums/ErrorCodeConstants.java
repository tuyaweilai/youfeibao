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
    ErrorCode TRANSPORT_TASK_STOP_REQUIRED = new ErrorCode(1_030_200_012, "任务至少需要一个停靠点，或一个提货点地址");

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

    // ========== 运输停靠点 1-030-204-000（V5 #72） ==========
    ErrorCode TRANSPORT_STOP_NOT_EXISTS = new ErrorCode(1_030_204_000, "运输停靠点不存在");
    ErrorCode TRANSPORT_STOP_NOT_BELONG_TO_TASK = new ErrorCode(1_030_204_001, "该停靠点不属于这趟运输任务");
    ErrorCode TRANSPORT_STOP_CANCEL_REASON_REQUIRED = new ErrorCode(1_030_204_002, "取消停靠点必须填原因");
    ErrorCode TRANSPORT_STOP_STATUS_NOT_ALLOW_CANCEL =
            new ErrorCode(1_030_204_003, "已完成或已取消的停靠点不能再取消");
    ErrorCode TRANSPORT_STOP_CANCELLED_NOT_REPORTABLE =
            new ErrorCode(1_030_204_004, "该停靠点已取消，不能再上报节点");
    ErrorCode TRANSPORT_STOP_REQUIRED_FOR_NODE =
            new ErrorCode(1_030_204_005, "到达提货点 / 交接完成 / 起运必须指定停靠点（集货时不能混报）");
    ErrorCode TRANSPORT_STOP_NOT_ALLOWED_FOR_NODE =
            new ErrorCode(1_030_204_006, "到达场站 / 卸货完成是整趟活的收尾，不属于单个停靠点");

    // ========== 交接登记 1-030-207-000（V6 #73） ==========
    //
    // 现场只登记事实（品类 / 参考量 / 参考单价 / 凭证照片），不产生金额、不产生收购单（ADR 0031）。
    ErrorCode TRANSPORT_HANDOVER_NOT_EXISTS = new ErrorCode(1_030_207_000, "交接登记不存在");
    ErrorCode TRANSPORT_HANDOVER_TASK_NOT_REGISTRABLE =
            new ErrorCode(1_030_207_001, "待分配或已取消的任务不能登记交接（没有车与人这一趟就是没跑）");
    ErrorCode TRANSPORT_HANDOVER_STOP_REQUIRED =
            new ErrorCode(1_030_207_002, "任务有停靠点时必须指定在哪一家登记的交接（集货时不能混成一次）");
    ErrorCode TRANSPORT_HANDOVER_STOP_NOT_BELONG_TO_TASK =
            new ErrorCode(1_030_207_003, "该停靠点不属于这趟运输任务");
    ErrorCode TRANSPORT_HANDOVER_PAYEE_REQUIRED =
            new ErrorCode(1_030_207_004, "交接登记必须指定出售者（现场新出售者先建档再回来登记）");
    ErrorCode TRANSPORT_HANDOVER_QUANTITY_INVALID =
            new ErrorCode(1_030_207_005, "参考量必须大于 0（参考单价不能为负）");
    ErrorCode TRANSPORT_HANDOVER_PHOTO_REQUIRED =
            new ErrorCode(1_030_207_006, "交接登记必须上传凭证照片（现场凭证）");
    ErrorCode TRANSPORT_HANDOVER_DOCUMENT_GAP_REQUIRED =
            new ErrorCode(1_030_207_007, "记为「待补档」时必须说明缺什么（如缺身份证 / 缺银行卡）");
    ErrorCode TRANSPORT_HANDOVER_DOCUMENT_STATUS_UNKNOWN =
            new ErrorCode(1_030_207_008, "未知的要件状态");
    ErrorCode TRANSPORT_HANDOVER_STOP_ALREADY_REGISTERED =
            new ErrorCode(1_030_207_009, "该停靠点已经登记过交接，不要重复登记（补录请改这一条）");

    // ========== 承运合同 1-030-206-000（V8 #75） ==========
    ErrorCode CARRIER_CONTRACT_NOT_EXISTS = new ErrorCode(1_030_206_000, "承运合同不存在");
    ErrorCode CARRIER_CONTRACT_NO_DUPLICATE = new ErrorCode(1_030_206_001, "承运合同编号已存在");
    ErrorCode CARRIER_CONTRACT_EFFECTIVE_RANGE_INVALID =
            new ErrorCode(1_030_206_002, "承运合同生效日期不能晚于失效日期");
    ErrorCode CARRIER_CONTRACT_SCOPE_REQUIRED =
            new ErrorCode(1_030_206_003, "承运合同必须至少限定适用线路或适用品类之一（否则运价无从适用）");
    ErrorCode CARRIER_CONTRACT_PRICE_INVALID =
            new ErrorCode(1_030_206_004, "承运合同运价不能为负");
    ErrorCode CARRIER_CONTRACT_SURCHARGE_INVALID =
            new ErrorCode(1_030_206_005, "附加费必须写清名称、金额与承担方");
    ErrorCode CARRIER_CONTRACT_NOT_EFFECTIVE =
            new ErrorCode(1_030_206_006, "承运合同已停用或不在有效期内，不能据它汇集运费");
    ErrorCode CARRIER_CONTRACT_CARRIER_MISMATCH =
            new ErrorCode(1_030_206_007, "该承运合同不属于这一趟的承运商");

    // ========== 承运商运费与对账 1-030-208-000（V8 #75） ==========
    ErrorCode FREIGHT_NOT_EXISTS = new ErrorCode(1_030_208_000, "运费单不存在");
    ErrorCode FREIGHT_CARRIER_REQUIRED =
            new ErrorCode(1_030_208_003, "自有车不产生承运商运费：这一趟不是承运商的车，不能建承运商运费单");
    ErrorCode FREIGHT_CONTRACT_REQUIRED =
            new ErrorCode(1_030_208_004, "承运商运费必须指明按哪份承运合同汇集（运价来自合同）");
    ErrorCode FREIGHT_BILL_QUANTITY_INVALID =
            new ErrorCode(1_030_208_005, "计费量必须大于 0（按车填趟数、按吨填吨数、按公里填公里数）");
    ErrorCode FREIGHT_STATUS_NOT_ALLOW_UPDATE =
            new ErrorCode(1_030_208_006, "当前状态不允许修改运费单（已确认应付后只能登记付款凭证）");
    ErrorCode FREIGHT_ACTUAL_AMOUNT_REQUIRED =
            new ErrorCode(1_030_208_007, "确认应付前必须填实际应付金额");
    ErrorCode FREIGHT_VARIANCE_REASON_REQUIRED =
            new ErrorCode(1_030_208_008, "实际应付与应有应付有差异时必须填差异原因（差异不抹平）");
    ErrorCode FREIGHT_STATUS_NOT_ALLOW_CONFIRM =
            new ErrorCode(1_030_208_009, "当前状态不允许确认应付（只有待确认应付能确认）");
    ErrorCode FREIGHT_VOUCHER_REQUIRED =
            new ErrorCode(1_030_208_010, "登记付款凭证必须填凭证号或凭证附件");
    ErrorCode FREIGHT_STATUS_NOT_ALLOW_PAY =
            new ErrorCode(1_030_208_011, "当前状态不允许登记付款凭证（先确认应付）");
    ErrorCode FREIGHT_CARRIER_NOT_ACTIVE = new ErrorCode(1_030_208_012, "承运商已停用，不能据它汇集运费");
    ErrorCode FREIGHT_TASK_ALREADY_GATHERED =
            new ErrorCode(1_030_208_013, "该趟次已经汇集过运费，要改请改那一条（一趟一张）");

    // ========== 运输费用：自有车的路桥 / 燃油等内部成本 1-030-209-000（V8 #75） ==========
    ErrorCode TRANSPORT_COST_NOT_EXISTS = new ErrorCode(1_030_209_000, "运输费用记录不存在");
    ErrorCode TRANSPORT_COST_AMOUNT_INVALID = new ErrorCode(1_030_209_002, "运输费用金额不能为负");
    ErrorCode TRANSPORT_COST_TYPE_UNKNOWN = new ErrorCode(1_030_209_003, "未知的运输费用类型");
    ErrorCode TRANSPORT_COST_BEARER_UNKNOWN = new ErrorCode(1_030_209_004, "未知的费用承担方");

}
