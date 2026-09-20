package cn.iocoder.yudao.module.icbc.controller.admin.report.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 管理后台 - 异常表 Response VO（#57 T19）。
 *
 * <p>派生清单，不新建表：每一条都能指回来源单据，并给前端一个下钻入口（{@code drillDown}）。
 */
@Schema(description = "管理后台 - 异常表 Response VO")
@Data
public class ReportAnomalyRespVO {

    @Schema(description = "异常类型编码")
    private String type;

    @Schema(description = "异常类型名")
    private String typeName;

    @Schema(description = "判定口径")
    private String definition;

    @Schema(description = "严重程度：DANGER / WARNING")
    private String severity;

    @Schema(description = "建议下钻入口")
    private String drillDown;

    @Schema(description = "来源单据类型")
    private String bizType;

    @Schema(description = "来源单据编号")
    private Long bizId;

    @Schema(description = "来源单号")
    private String bizNo;

    @Schema(description = "主体（交易对方 / 订单对方）")
    private String subject;

    @Schema(description = "品类")
    private String categoryName;

    @Schema(description = "异常说明")
    private String detail;

    @Schema(description = "涉及数量（可为空）")
    private BigDecimal quantity;

    @Schema(description = "涉及金额（可为空）")
    private BigDecimal amount;

    @Schema(description = "发生时间")
    private LocalDateTime time;

}
