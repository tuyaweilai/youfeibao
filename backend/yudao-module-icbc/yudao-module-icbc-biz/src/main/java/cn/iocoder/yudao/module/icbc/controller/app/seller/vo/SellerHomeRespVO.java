package cn.iocoder.yudao.module.icbc.controller.app.seller.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 自然人端首页摘要（#34）。
 *
 * <p>首屏只给五项：待我确认、我的记录、收款记录、发票与税费、我的资料。本 VO 是首页顶部的
 * 「待我确认」计数与列表；其余四项各自按需拉取。
 */
@Schema(description = "自然人端 - 首页摘要")
@Data
public class SellerHomeRespVO {

    @Schema(description = "待我确认总数", example = "2")
    private Integer pendingCount;

    @Schema(description = "待确认结算单数", example = "2")
    private Integer pendingSettlementCount;

    @Schema(description = "待签协议数", example = "0")
    private Integer pendingAgreementCount;

    @Schema(description = "待办列表（有内容时置顶展示）")
    private List<SellerPendingItemVO> pendingItems;

    @Schema(description = "金额口径说明（ADR 0021）：所有金额为本平台累计，不含其他渠道")
    private String amountScopeNote;

    @Schema(description = "当前场站编号（扫码进入时带上；为空表示不是从场站码进来）", example = "3072")
    private Long stationId;

    @Schema(description = "当前场站名称（只在扫码进入时有值）", example = "城东收货点")
    private String stationName;

}
