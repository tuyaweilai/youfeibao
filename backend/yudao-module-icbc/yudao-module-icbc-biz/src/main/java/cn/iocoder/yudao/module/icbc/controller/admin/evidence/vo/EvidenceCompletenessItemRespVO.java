package cn.iocoder.yudao.module.icbc.controller.admin.evidence.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "管理后台 - 单张票齐备率 Response VO")
@Data
public class EvidenceCompletenessItemRespVO {

    @Schema(description = "租户编号（平台运营跨租户查看时用于区分归属）", example = "1")
    private Long tenantId;

    @Schema(description = "合作方订单号", example = "ORDER_20231201_001")
    private String partnerOrderId;

    @Schema(description = "发票号码", example = "12345678901234567890")
    private String invoiceNo;

    @Schema(description = "齐备的流数", example = "3")
    private Integer presentCount;

    @Schema(description = "五流总数", example = "5")
    private Integer totalCount;

    @Schema(description = "齐备率（百分比，0-100）", example = "60.00")
    private BigDecimal completenessRate;

    @Schema(description = "缺失的流名称", example = "[\"合同流\",\"货物流\"]")
    private List<String> missingFlows;

}
