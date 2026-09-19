package cn.iocoder.yudao.module.icbc.controller.app.seller.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 自然人端 - 发起变更收款账户的返回。
 *
 * <p>返回一枚 {@code ONBOARDING} 一次性令牌：自然人端用它打开**后端输出的工行收方入驻自动提交表单**
 * （{@code /icbc/public/onboarding/form?token=...}），与首次建档走的是同一套机制（ADR 0010）。
 * 状态文案只说可核验的事：工行的审核结果拿到之前是「银行审核中」，不是「已换卡」。
 */
@Schema(description = "自然人端 - 变更收款账户返回")
@Data
public class SellerBankCardChangeRespVO {

    @Schema(description = "变更单号", example = "BC20260101120000ABCD")
    private String changeNo;

    @Schema(description = "状态：0-银行审核中，1-已生效，2-已拒绝，9-已取消", example = "0")
    private Integer status;

    @Schema(description = "状态名", example = "银行审核中")
    private String statusName;

    @Schema(description = "原卡尾号（钱原本要打到的卡）", example = "1234")
    private String oldCardTail;

    @Schema(description = "待变更的新卡尾号", example = "5678")
    private String newCardTail;

    @Schema(description = "工行收方入驻一次性令牌（用途 ONBOARDING）")
    private String token;

    @Schema(description = "令牌过期时间")
    private LocalDateTime expiresTime;

    @Schema(description = "给自然人看的一句话说明")
    private String message;

    @Schema(description = "口径说明（审核期间新交易的付款会挂起）")
    private String scopeNote;

}
