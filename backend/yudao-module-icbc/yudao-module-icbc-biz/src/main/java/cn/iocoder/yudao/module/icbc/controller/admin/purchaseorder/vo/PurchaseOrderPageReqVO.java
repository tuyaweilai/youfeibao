package cn.iocoder.yudao.module.icbc.controller.admin.purchaseorder.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 管理后台 - 采购订单分页 Request VO（#46 T08）。
 */
@Schema(description = "管理后台 - 采购订单分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class PurchaseOrderPageReqVO extends PageParam {

    @Schema(description = "采购订单号")
    private String orderNo;

    @Schema(description = "关联采购合同编号")
    private Long contractId;

    @Schema(description = "交易对方名称")
    private String counterpartyName;

    @Schema(description = "交易对方主体类型（六态）")
    private Integer counterpartyType;

    @Schema(description = "执行场站编号")
    private Long stationId;

    @Schema(description = "状态：0-草稿，1-执行中，2-暂停，3-完成，4-关闭")
    private Integer status;

}
