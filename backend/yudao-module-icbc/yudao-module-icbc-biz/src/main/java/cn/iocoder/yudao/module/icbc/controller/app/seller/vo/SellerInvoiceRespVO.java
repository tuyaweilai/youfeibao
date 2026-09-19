package cn.iocoder.yudao.module.icbc.controller.app.seller.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 自然人端 - 发票与税费（一张反向开票订单）。
 *
 * <p>付款、开票、税费三条状态线**分别显示**，任何一条失败都不得被「交易成功」掩盖（ADR 0021）。
 */
@Schema(description = "自然人端 - 发票与税费")
@Data
public class SellerInvoiceRespVO {

    @Schema(description = "开票订单编号", example = "4096")
    private Long invoiceOrderId;

    @Schema(description = "订单号", example = "INV202612010001")
    private String orderNo;

    @Schema(description = "对应的收购单号", example = "AC202612010001")
    private String acquisitionNo;

    @Schema(description = "收购方（回收企业名称）", example = "某某再生资源有限公司")
    private String acquirerName;

    @Schema(description = "发票号码", example = "24332000000012345678")
    private String invoiceNo;

    @Schema(description = "开票日期")
    private LocalDateTime invoiceDate;

    @Schema(description = "创建时间（未开票时按它归年）")
    private LocalDateTime createTime;

    @Schema(description = "开票金额（价税合计）", example = "980.00")
    private BigDecimal invoiceAmount;

    @Schema(description = "税额", example = "9.80")
    private BigDecimal taxAmount;

    // ==================== 三条状态线（分别显示） ====================

    @Schema(description = "开票状态编码", example = "2")
    private Integer invoiceStatus;

    @Schema(description = "开票状态名", example = "已开票")
    private String invoiceStatusName;

    @Schema(description = "税费状态编码", example = "1")
    private Integer taxStatus;

    @Schema(description = "税费状态名", example = "已缴税")
    private String taxStatusName;

    @Schema(description = "上传（税局）状态编码", example = "1")
    private Integer uploadStatus;

    @Schema(description = "上传状态名", example = "已上传")
    private String uploadStatusName;

    @Schema(description = "是否有可下载的发票 PDF", example = "true")
    private Boolean pdfAvailable;

}
