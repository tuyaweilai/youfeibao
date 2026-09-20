package cn.iocoder.yudao.module.logistics.controller.admin.freight.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 承运商运费单 Response VO（V8 #75）。
 *
 * <p>同时用于导出：以 `@ExcelIgnoreUnannotated` 只导出带 `@ExcelProperty` 的列，
 * 避免把内部 id / 备注等一起写进对账表。
 */
@Schema(description = "管理后台 - 承运商运费单 Response VO")
@Data
@ExcelIgnoreUnannotated
public class LogisticsFreightRespVO {

    @ExcelProperty("编号")
    @Schema(description = "编号")
    private Long id;

    @ExcelProperty("运费单号")
    @Schema(description = "运费单号")
    private String freightNo;

    @ExcelProperty("任务单号")
    @Schema(description = "运输任务单号")
    private String taskNo;

    @Schema(description = "运输任务编号")
    private Long taskId;

    @Schema(description = "承运商编号")
    private Long carrierId;

    @ExcelProperty("承运商")
    @Schema(description = "承运商名称")
    private String carrierName;

    @Schema(description = "承运合同编号")
    private Long contractId;

    @ExcelProperty("承运合同")
    @Schema(description = "承运合同号")
    private String contractNo;

    @Schema(description = "计费方式：1-按车，2-按吨，3-按公里")
    private Integer billingMode;

    @ExcelProperty("计费方式")
    @Schema(description = "计费方式名")
    private String billingModeName;

    @ExcelProperty("计费量")
    @Schema(description = "计费量")
    private BigDecimal billQuantity;

    @ExcelProperty("运价")
    @Schema(description = "运价（合同快照）")
    private BigDecimal billUnitPrice;

    @ExcelProperty("基础运费")
    @Schema(description = "基础运费（计费量 × 运价）")
    private BigDecimal baseAmount;

    @ExcelProperty("附加费净额")
    @Schema(description = "附加费净额（本企业承担 − 承运商承担）")
    private BigDecimal surchargeAmount;

    @ExcelProperty("应有应付")
    @Schema(description = "应有应付（基础运费 + 附加费净额）")
    private BigDecimal expectedAmount;

    @ExcelProperty("实际应付")
    @Schema(description = "实际应付（对账确认）")
    private BigDecimal actualAmount;

    @ExcelProperty("差异")
    @Schema(description = "差异（实际 − 应有）")
    private BigDecimal varianceAmount;

    @ExcelProperty("差异原因")
    @Schema(description = "差异原因")
    private String varianceReason;

    @ExcelProperty("状态")
    @Schema(description = "状态名")
    private String statusName;

    @Schema(description = "状态：0-待确认应付，1-已确认应付，2-已登记付款凭证")
    private Integer status;

    @ExcelProperty("确认人")
    @Schema(description = "确认应付人姓名")
    private String confirmByName;

    @ExcelProperty("确认时间")
    @Schema(description = "确认应付时间")
    private LocalDateTime confirmTime;

    @Schema(description = "确认备注")
    private String confirmRemark;

    @ExcelProperty("付款凭证号")
    @Schema(description = "外部付款凭证号")
    private String paymentVoucherNo;

    @Schema(description = "外部付款凭证附件 URL")
    private String paymentVoucherUrl;

    @ExcelProperty("实付金额")
    @Schema(description = "实付金额")
    private BigDecimal paymentAmount;

    @ExcelProperty("付款时间")
    @Schema(description = "付款时间")
    private LocalDateTime paidAt;

    @Schema(description = "付款备注")
    private String paymentRemark;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
