package cn.iocoder.yudao.module.logistics.enums;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;

/**
 * 物流模块错误码枚举类
 *
 * 物流模块，使用 1-020-000-000 段
 */
public interface ErrorCodeConstants {

    // ========== 车辆管理 1-020-001-000 ==========
    ErrorCode VEHICLE_NOT_EXISTS = new ErrorCode(1_020_001_000, "车辆不存在");
    ErrorCode VEHICLE_PLATE_NUMBER_EXISTS = new ErrorCode(1_020_001_001, "车牌号已存在");
    ErrorCode VEHICLE_IN_USE = new ErrorCode(1_020_001_002, "车辆正在使用中，无法删除");
    ErrorCode VEHICLE_STATUS_INVALID = new ErrorCode(1_020_001_003, "车辆状态无效");

    // ========== 司机管理 1-020-002-000 ==========
    ErrorCode DRIVER_NOT_EXISTS = new ErrorCode(1_020_002_000, "司机不存在");
    ErrorCode DRIVER_CODE_EXISTS = new ErrorCode(1_020_002_001, "司机编号已存在");
    ErrorCode DRIVER_LICENSE_NO_EXISTS = new ErrorCode(1_020_002_002, "驾驶证号码已存在");

    // ========== 运输任务 1-020-003-000 ==========
    ErrorCode TRANSPORT_TASK_NOT_EXISTS = new ErrorCode(1_020_003_000, "运输任务不存在");
    ErrorCode TRANSPORT_TASK_STATUS_INVALID = new ErrorCode(1_020_003_001, "运输任务状态无效");
    ErrorCode TRANSPORT_TASK_NO_DUPLICATE = new ErrorCode(1_020_003_002, "任务编号已存在");
    ErrorCode TRANSPORT_TASK_STATUS_NOT_PENDING = new ErrorCode(1_020_003_003, "任务状态不是待分配，无法分配");
    ErrorCode TRANSPORT_TASK_STATUS_NOT_ASSIGNED = new ErrorCode(1_020_003_004, "任务状态不是已分配，无法接受");
    ErrorCode TRANSPORT_TASK_STATUS_NOT_ACCEPTED = new ErrorCode(1_020_003_005, "任务状态不是已接受，无法开始运输");
    ErrorCode TRANSPORT_TASK_STATUS_NOT_IN_TRANSIT = new ErrorCode(1_020_003_006, "任务状态不是运输中，无法确认取货");
    ErrorCode TRANSPORT_TASK_STATUS_NOT_PICKED_UP = new ErrorCode(1_020_003_007, "任务状态不是已取货，无法确认送达");
    ErrorCode TRANSPORT_TASK_STATUS_NOT_DELIVERED = new ErrorCode(1_020_003_008, "任务状态不是已送达，无法完成任务");
    ErrorCode TRANSPORT_TASK_STATUS_COMPLETED_CANNOT_CANCEL = new ErrorCode(1_020_003_009, "已完成的任务无法取消");
    ErrorCode TRANSPORT_TASK_NO_ABNORMAL = new ErrorCode(1_020_003_010, "任务没有异常，无法解决异常");

    // ========== 临时订单 1-020-004-000 ==========
    ErrorCode TEMPORARY_ORDER_NOT_EXISTS = new ErrorCode(1_020_004_000, "临时订单不存在");
    ErrorCode TEMPORARY_ORDER_ORDER_NO_DUPLICATE = new ErrorCode(1_020_004_001, "订单编号已存在");
    ErrorCode TEMPORARY_ORDER_CANNOT_DELETE_CONVERTED = new ErrorCode(1_020_004_002, "已转为正式订单的临时订单无法删除");
    ErrorCode TEMPORARY_ORDER_CANNOT_DELETE_PAID = new ErrorCode(1_020_004_003, "已支付的临时订单无法删除");
    ErrorCode TEMPORARY_ORDER_PAYMENT_STATUS_NOT_UNPAID = new ErrorCode(1_020_004_004, "订单状态不是未支付，无法支付");
    ErrorCode TEMPORARY_ORDER_PAYMENT_STATUS_NOT_PAID = new ErrorCode(1_020_004_005, "订单状态不是已支付，无法退款");
    ErrorCode TEMPORARY_ORDER_CANNOT_CANCEL_CONVERTED = new ErrorCode(1_020_004_006, "已转为正式订单的临时订单无法取消");
    ErrorCode TEMPORARY_ORDER_ALREADY_CONVERTED = new ErrorCode(1_020_004_007, "临时订单已转为正式订单");

    // ========== 现金代付 1-020-005-000 ==========
    ErrorCode CASH_ADVANCE_NOT_EXISTS = new ErrorCode(1_020_005_000, "现金代付记录不存在");
    ErrorCode CASH_ADVANCE_CANNOT_DELETE_RECONCILED = new ErrorCode(1_020_005_001, "已对账的现金代付记录无法删除");
    ErrorCode CASH_ADVANCE_NOTIFY_STATUS_NOT_NOT_NOTIFIED = new ErrorCode(1_020_005_002, "记录状态不是未通知，无法通知");
    ErrorCode CASH_ADVANCE_RECONCILE_STATUS_NOT_NOT_RECONCILED = new ErrorCode(1_020_005_003, "记录状态不是未对账，无法对账");

}