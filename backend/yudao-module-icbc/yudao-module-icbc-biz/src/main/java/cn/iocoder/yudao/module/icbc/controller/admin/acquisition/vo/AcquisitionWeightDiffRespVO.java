package cn.iocoder.yudao.module.icbc.controller.admin.acquisition.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 管理后台 - 称量差异清单 Response VO（#53 T15，只读）。
 *
 * <p>把同一张收购单上的两个重量口径并排给出：结算重量（计价基准）与实物量（接收量优先，无则净重），
 * 以及两者的差。差额不静默抹平，异常表（#57）直接消费本响应。
 */
@Schema(description = "管理后台 - 称量差异清单 Response VO")
@Data
public class AcquisitionWeightDiffRespVO {

    @Schema(description = "收购单编号", example = "1024")
    private Long id;

    @Schema(description = "收购单号", example = "ACQ17645472000001234")
    private String acquisitionNo;

    @Schema(description = "出售者档案编号", example = "1024")
    private Long payeeId;

    @Schema(description = "出售者姓名", example = "张三")
    private String sellerName;

    @Schema(description = "品类名称", example = "废钢")
    private String categoryName;

    @Schema(description = "计量单位", example = "吨")
    private String unit;

    @Schema(description = "净重（毛重 − 皮重，过磅实物原始量）", example = "12500.00")
    private BigDecimal netWeight;

    @Schema(description = "扣杂（价款扣减，不自动扣库存）", example = "500.00")
    private BigDecimal deduction;

    @Schema(description = "结算重量（唯一计价基准）", example = "12000.00")
    private BigDecimal settlementWeight;

    @Schema(description = "实物量：接收量（有值时）优先，否则取净重", example = "11500.00")
    private BigDecimal physicalWeight;

    @Schema(description = "接收量", example = "11500.00")
    private BigDecimal acceptedWeight;

    @Schema(description = "退回量", example = "500.00")
    private BigDecimal rejectedWeight;

    @Schema(description = "余货出场量", example = "500.00")
    private BigDecimal residualWeight;

    @Schema(description = "拒收原因", example = "含水率超标，杂质过多")
    private String rejectReason;

    @Schema(description = "称量差异 = 实物量 − 结算重量（正数=实物多于计价，负数=计价多于实物）", example = "-500.00")
    private BigDecimal weightDiff;

    @Schema(description = "差异说明：实物量与结算重量的口径对照", example = "实物量 11500.00 − 结算重量 12000.00 = -500.00（计价多于实物）")
    private String differenceNote;

    @Schema(description = "应付金额（已扣退回量与余货出场量）", example = "23000.00")
    private BigDecimal amount;

    @Schema(description = "收购单状态", example = "0")
    private Integer status;

    @Schema(description = "交易时间")
    private LocalDateTime tradeTime;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
