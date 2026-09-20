package cn.iocoder.yudao.module.icbc.controller.admin.purchaseorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

/**
 * 管理后台 - 采购订单新增 / 修改 Request VO（#46 T08）。
 */
@Schema(description = "管理后台 - 采购订单新增/修改 Request VO")
@Data
public class PurchaseOrderSaveReqVO {

    @Schema(description = "主键（修改时必填）", example = "1")
    private Long id;

    @Schema(description = "关联采购合同编号（可空；填了就必须是已审核生效且未过期的合同）")
    private Long contractId;

    @Schema(description = "交易对方主体类型（六态：1-自然人出售者，2-个体工商户，3-个人独资企业，"
            + "4-合伙企业，5-企业法人，6-农民专业合作社）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "交易对方主体类型不能为空")
    private Integer counterpartyType;

    @Schema(description = "自然人出售者档案编号（主体类型为自然人时必填）")
    private Long payeeId;

    @Schema(description = "单位供货方编号（主体类型为非自然人时必填，指向 erp_supplier）")
    private Long supplierId;

    @Schema(description = "单位供货方名称（非自然人对对方的名称快照，必填）")
    private String counterpartyName;

    @Schema(description = "执行场站编号（可空）")
    private Long stationId;

    @Schema(description = "执行开始日期", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "执行开始日期不能为空")
    private LocalDate startDate;

    @Schema(description = "执行结束日期（早于今天即视为过期）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "执行结束日期不能为空")
    private LocalDate endDate;

    @Schema(description = "品类明细（至少一条）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "采购订单至少需要一条品类明细")
    private List<PurchaseOrderItemReqVO> items;

    @Schema(description = "备注")
    private String remark;

}
