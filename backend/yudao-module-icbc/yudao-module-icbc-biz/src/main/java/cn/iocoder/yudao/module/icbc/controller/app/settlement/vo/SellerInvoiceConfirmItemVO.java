package cn.iocoder.yudao.module.icbc.controller.app.settlement.vo;

import cn.iocoder.yudao.module.icbc.controller.admin.invoice.vo.InvoicePreCheckItemVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 自然人端 - 一张收购单的开票信息确认进度（#106，ADR 0039）。
 *
 * <p>结算确认之后，同一张结算单下的每张收购单各要一次工行页面的确认。这里逐张给出「走到哪了」
 * 与「还能不能打开确认页」，让自然人在自己手机上按步骤走完，而不是被开票员塞一张只能他打开的页面。
 */
@Schema(description = "自然人端 - 开票信息确认进度")
@Data
public class SellerInvoiceConfirmItemVO {

    @Schema(description = "收购单编号")
    private Long acquisitionId;

    @Schema(description = "收购单号", example = "ACQ17645472000001234")
    private String acquisitionNo;

    @Schema(description = "品类", example = "废钢")
    private String categoryName;

    @Schema(description = "金额", example = "32500.00")
    private BigDecimal amount;

    @Schema(description = "档位：BLOCKED-还不能发起，WAITING_CONFIRM-待本人确认，CONFIRMED-已确认等付款")
    private String stage;

    @Schema(description = "档位名称", example = "待你在工行页面确认")
    private String stageName;

    @Schema(description = "这一张的说明：为什么还不能发起，或为什么打不开确认页")
    private String message;

    @Schema(description = "不能发起时的逐项原因与补齐方式（与企业侧开票申请用同一套校验结果）")
    private List<InvoicePreCheckItemVO> failures;

    @Schema(description = "工行确认页面的地址（带一次性令牌，可直接在浏览器 / web-view 里打开）；为空表示现在打不开")
    private String confirmPageUrl;

    @Schema(description = "确认页地址是否可用")
    private Boolean confirmPageAvailable;

}
