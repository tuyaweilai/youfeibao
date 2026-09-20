package cn.iocoder.yudao.module.logistics.controller.admin.carriercontract.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "管理后台 - 承运合同新增/修改 Request VO")
@Data
public class LogisticsCarrierContractSaveReqVO {

    @Schema(description = "主键", example = "1")
    private Long id;

    @Schema(description = "合同编号（留空则由平台生成）", example = "CC202609200001")
    private String contractNo;

    @Schema(description = "承运商编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "承运商不能为空")
    private Long carrierId;

    @Schema(description = "生效日期", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "生效日期不能为空")
    private LocalDate effectiveFrom;

    @Schema(description = "失效日期（留空表示长期）")
    private LocalDate effectiveTo;

    @Schema(description = "适用线路（与适用品类至少填一个）", example = "城东场站—临平")
    private String route;

    @Schema(description = "适用品类编号（与适用线路至少填一个）", example = "2048")
    private Long goodsConfigId;

    @Schema(description = "适用品类名称快照", example = "废钢铁")
    private String categoryName;

    @Schema(description = "计费方式：1-按车，2-按吨，3-按公里", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "计费方式不能为空")
    private Integer billingMode;

    @Schema(description = "运价（按计费方式的单价）", requiredMode = Schema.RequiredMode.REQUIRED, example = "300.00")
    @NotNull(message = "运价不能为空")
    private BigDecimal unitPrice;

    @Schema(description = "附加费列表（名称 / 金额 / 承担方）")
    @Valid
    private List<LogisticsCarrierContractSurchargeVO> surcharges;

    @Schema(description = "状态：0-生效，1-已停用", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @NotNull(message = "状态不能为空")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

}
