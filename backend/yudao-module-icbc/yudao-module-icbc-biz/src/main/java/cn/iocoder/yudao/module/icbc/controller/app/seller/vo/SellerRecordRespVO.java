package cn.iocoder.yudao.module.icbc.controller.app.seller.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 自然人端 - 卖货记录（一条收购单）。
 *
 * <p>字段口径：结算重量（毛重 − 皮重 − 扣杂）是唯一计价基准（ADR 0019）。
 */
@Schema(description = "自然人端 - 卖货记录")
@Data
public class SellerRecordRespVO {

    @Schema(description = "收购单编号", example = "1024")
    private Long acquisitionId;

    @Schema(description = "收购单号", example = "AC202612010001")
    private String acquisitionNo;

    @Schema(description = "品类", example = "废钢")
    private String categoryName;

    @Schema(description = "计量单位", example = "吨")
    private String unit;

    @Schema(description = "结算重量", example = "9.80")
    private BigDecimal settlementWeight;

    @Schema(description = "扣杂", example = "0.20")
    private BigDecimal deduction;

    @Schema(description = "扣杂录法：WEIGHT-按重量，RATIO-按比例")
    private String deductionMethod;

    @Schema(description = "单价", example = "100.00")
    private BigDecimal unitPrice;

    @Schema(description = "金额", example = "980.00")
    private BigDecimal amount;

    @Schema(description = "收购方（回收企业名称）", example = "某某再生资源有限公司")
    private String acquirerName;

    @Schema(description = "状态编码", example = "3")
    private Integer status;

    @Schema(description = "状态名", example = "已开票")
    private String statusName;

    @Schema(description = "作废原因（作废对自然人可见）")
    private String cancelReason;

    @Schema(description = "交易时间")
    private LocalDateTime tradeTime;

    @Schema(description = "交易地点")
    private String tradeAddress;

    @Schema(description = "是否可打印单笔收购确认书", example = "true")
    private Boolean printable;

}
