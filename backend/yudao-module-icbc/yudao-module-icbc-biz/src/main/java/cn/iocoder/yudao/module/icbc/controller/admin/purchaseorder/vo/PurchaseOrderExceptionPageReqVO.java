package cn.iocoder.yudao.module.icbc.controller.admin.purchaseorder.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 管理后台 - 履约异常授权单分页 Request VO（#47 T09）。
 */
@Schema(description = "管理后台 - 履约异常授权单分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class PurchaseOrderExceptionPageReqVO extends PageParam {

    @Schema(description = "采购订单编号")
    private Long orderId;

    @Schema(description = "采购订单号")
    private String orderNo;

    @Schema(description = "异常类型：OVER_QUANTITY / EXPIRED / CROSS_STATION")
    private String exceptionType;

    @Schema(description = "状态：0-待审核，1-已通过，2-已拒绝")
    private Integer status;

}
