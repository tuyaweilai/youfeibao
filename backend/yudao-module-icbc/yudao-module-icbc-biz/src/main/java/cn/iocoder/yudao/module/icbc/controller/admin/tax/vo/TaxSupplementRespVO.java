package cn.iocoder.yudao.module.icbc.controller.admin.tax.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 需补缴税费 Response VO")
@Data
public class TaxSupplementRespVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "补缴单号", example = "TAXSUP-202608-1")
    private String supplementNo;

    @Schema(description = "关联申报单编号")
    private Long declarationId;

    @Schema(description = "所属申报月 yyyy-MM", example = "2026-08")
    private String periodMonth;

    @Schema(description = "出售者档案编号")
    private Long payeeId;

    @Schema(description = "出售者姓名", example = "张三")
    private String sellerName;

    @Schema(description = "补缴原因")
    private String reason;

    @Schema(description = "按 3% 减按 1% 部分的补缴金额（元）")
    private BigDecimal amountAtOnePercent;

    @Schema(description = "放弃减按、按 3% 部分的补缴金额（元）")
    private BigDecimal amountAtThreePercent;

    @Schema(description = "应补缴金额合计（元）")
    private BigDecimal amount;

    @Schema(description = "状态：0-待补缴，1-已补缴")
    private Integer status;

    @Schema(description = "状态名称", example = "待补缴")
    private String statusName;

    @Schema(description = "下一步动作")
    private String nextAction;

    @Schema(description = "实缴金额（元）")
    private BigDecimal paidAmount;

    @Schema(description = "缴款时间")
    private LocalDateTime paidAt;

    @Schema(description = "缴款凭证号")
    private String voucherNo;

    @Schema(description = "缴款凭证文件地址")
    private String voucherFileUrl;

    @Schema(description = "备注")
    private String remark;

}
