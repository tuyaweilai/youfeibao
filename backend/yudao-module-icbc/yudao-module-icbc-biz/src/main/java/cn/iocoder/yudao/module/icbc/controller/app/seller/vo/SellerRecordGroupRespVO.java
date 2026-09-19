package cn.iocoder.yudao.module.icbc.controller.app.seller.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 自然人端 - 卖货记录按回收企业分组。
 *
 * <p>跨企业仅本人可见，企业侧不可见（CONTEXT.md「交易可见性边界」）。
 */
@Schema(description = "自然人端 - 卖货记录分组")
@Data
public class SellerRecordGroupRespVO {

    @Schema(description = "租户编号", example = "1")
    private Long tenantId;

    @Schema(description = "回收企业名称", example = "某某再生资源有限公司")
    private String enterpriseName;

    @Schema(description = "记录条数", example = "3")
    private Integer count;

    @Schema(description = "本企业合计金额（本平台累计）", example = "3000.00")
    private BigDecimal totalAmount;

    @Schema(description = "本企业合计结算重量", example = "30.00")
    private BigDecimal totalSettlementWeight;

    @Schema(description = "记录明细")
    private List<SellerRecordRespVO> records;

}
