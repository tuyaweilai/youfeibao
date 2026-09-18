package cn.iocoder.yudao.module.icbc.controller.admin.evidence.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 一张票的五流证据链。{@code completenessRate} 按「齐备的流数 / 5」计算，
 * 便于运营在列表里一眼看出哪张票还缺口。
 */
@Schema(description = "管理后台 - 一票一档证据链 Response VO")
@Data
public class EvidenceChainRespVO {

    @Schema(description = "合作方订单号", requiredMode = Schema.RequiredMode.REQUIRED, example = "ORDER_20231201_001")
    private String partnerOrderId;

    @Schema(description = "订单号", example = "INV17330112000001234")
    private String orderNo;

    @Schema(description = "发票号码", example = "12345678901234567890")
    private String invoiceNo;

    @Schema(description = "出售者姓名", example = "张三")
    private String sellerName;

    @Schema(description = "订单金额", example = "1000.00")
    private BigDecimal totalAmount;

    @Schema(description = "齐备的流数", requiredMode = Schema.RequiredMode.REQUIRED, example = "3")
    private Integer presentCount;

    @Schema(description = "五流总数", requiredMode = Schema.RequiredMode.REQUIRED, example = "5")
    private Integer totalCount;

    @Schema(description = "齐备率（百分比，0-100）", requiredMode = Schema.RequiredMode.REQUIRED, example = "60.00")
    private BigDecimal completenessRate;

    @Schema(description = "是否五流齐备", requiredMode = Schema.RequiredMode.REQUIRED, example = "false")
    private Boolean complete;

    @Schema(description = "五流明细")
    private List<EvidenceFlowRespVO> flows;

    @Schema(description = "人工补录的证据")
    private List<EvidenceRespVO> attachments;

    @Schema(description = "最近更新时间")
    private LocalDateTime updateTime;

}
