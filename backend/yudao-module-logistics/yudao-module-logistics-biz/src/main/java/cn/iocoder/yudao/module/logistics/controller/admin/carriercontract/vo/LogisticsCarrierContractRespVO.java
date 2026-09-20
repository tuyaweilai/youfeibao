package cn.iocoder.yudao.module.logistics.controller.admin.carriercontract.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 承运合同 Response VO（同时用作导出 Excel 的行）")
@Data
@ExcelIgnoreUnannotated
public class LogisticsCarrierContractRespVO {

    @ExcelProperty("编号")
    @Schema(description = "编号")
    private Long id;

    @ExcelProperty("合同编号")
    @Schema(description = "合同编号")
    private String contractNo;

    @Schema(description = "承运商编号")
    private Long carrierId;

    @ExcelProperty("承运商")
    @Schema(description = "承运商名称")
    private String carrierName;

    @ExcelProperty("生效日期")
    @Schema(description = "生效日期")
    private LocalDate effectiveFrom;

    @ExcelProperty("失效日期")
    @Schema(description = "失效日期（空为长期）")
    private LocalDate effectiveTo;

    @ExcelProperty("适用线路")
    @Schema(description = "适用线路")
    private String route;

    @ExcelProperty("适用品类")
    @Schema(description = "适用品类名称")
    private String categoryName;

    @Schema(description = "适用品类编号")
    private Long goodsConfigId;

    @ExcelProperty("计费方式")
    @Schema(description = "计费方式名（导出用）")
    private String billingModeName;

    @Schema(description = "计费方式：1-按车，2-按吨，3-按公里")
    private Integer billingMode;

    @ExcelProperty("运价")
    @Schema(description = "运价")
    private BigDecimal unitPrice;

    @Schema(description = "附加费列表（名称 / 金额 / 承担方）")
    private List<LogisticsCarrierContractSurchargeVO> surcharges;

    @ExcelProperty("状态")
    @Schema(description = "状态名")
    private String statusName;

    @Schema(description = "状态：0-生效，1-已停用")
    private Integer status;

    @ExcelProperty("备注")
    @Schema(description = "备注")
    private String remark;

    @ExcelProperty("创建时间")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
