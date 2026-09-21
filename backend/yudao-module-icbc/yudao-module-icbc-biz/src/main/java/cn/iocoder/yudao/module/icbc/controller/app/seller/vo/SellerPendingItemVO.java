package cn.iocoder.yudao.module.icbc.controller.app.seller.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 自然人端首页「待我确认」的一条待办。
 *
 * <p>只放**需要他动作**的：待签协议、待确认结算单、被企业改过需重新确认的异议单（#34）。
 * 不做消息中心、不做通知流。
 */
@Schema(description = "自然人端 - 待我确认项")
@Data
public class SellerPendingItemVO {

    @Schema(description = "类型：AGREEMENT-待签框架协议，SETTLEMENT-待确认结算单", example = "SETTLEMENT")
    private String type;

    @Schema(description = "点击动作：SIGN_AGREEMENT-去签署（待签电子协议，点一下现取签署链接并跳转）；无动作时为空",
            example = "SIGN_AGREEMENT")
    private String action;

    @Schema(description = "类型名", example = "待确认结算单")
    private String typeName;

    @Schema(description = "租户编号", example = "1")
    private Long tenantId;

    @Schema(description = "回收企业名称", example = "某某再生资源有限公司")
    private String enterpriseName;

    @Schema(description = "结算单编号（类型为结算单时）")
    private Long settlementId;

    @Schema(description = "收方档案编号")
    private Long payeeId;

    @Schema(description = "标题", example = "ST202612010001")
    private String title;

    @Schema(description = "状态名", example = "待确认")
    private String statusName;

    @Schema(description = "下一步动作截止时间")
    private LocalDateTime deadlineTime;

    @Schema(description = "是否已逾期/需线下签字")
    private Boolean urgent;

}
