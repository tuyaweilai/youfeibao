package cn.iocoder.yudao.module.icbc.controller.admin.platform.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 平台运营 - 全平台异常票 Response VO。
 *
 * <p>一张票被列进来，要么是某条状态线异常（开票 / 缴税 / 上传 / 付款 / 红冲），
 * 要么是票已开出但五流证据不齐。{@code reasons} 说明是哪一种。
 */
@Schema(description = "管理后台 - 平台运营异常票 Response VO")
@Data
public class PlatformExceptionInvoiceRespVO {

    @Schema(description = "租户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long tenantId;

    @Schema(description = "合作方订单号", requiredMode = Schema.RequiredMode.REQUIRED, example = "ORDER_20231201_001")
    private String partnerOrderId;

    @Schema(description = "订单号", example = "INV202409180001")
    private String orderNo;

    @Schema(description = "发票号码", example = "24312000000012345678")
    private String invoiceNo;

    @Schema(description = "开票状态名", example = "开票失败")
    private String invoiceStatusName;

    @Schema(description = "缴税状态名", example = "缴税失败")
    private String taxStatusName;

    @Schema(description = "上传状态名", example = "上传失败")
    private String uploadStatusName;

    @Schema(description = "支付状态名", example = "支付失败")
    private String paymentStatusName;

    @Schema(description = "五流齐备率（百分比，0-100）", example = "60.00")
    private BigDecimal completenessRate;

    @Schema(description = "缺失的流名称", example = "[\"合同流\",\"货物流\"]")
    private List<String> missingFlows;

    @Schema(description = "异常原因清单", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "[\"开票失败\",\"五流不齐（缺 合同流、货物流）\"]")
    private List<String> reasons;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
