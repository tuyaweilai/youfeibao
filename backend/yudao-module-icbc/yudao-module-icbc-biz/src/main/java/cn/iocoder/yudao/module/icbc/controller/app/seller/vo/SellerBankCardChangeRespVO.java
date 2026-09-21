package cn.iocoder.yudao.module.icbc.controller.app.seller.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 自然人端 - 发起变更收款账户的返回（#37 / #89）。
 *
 * <p>换卡走工行的**收方修改数据接口**（后端直接提交，ADR 0035），本人端不再拿一次性令牌去开页面。
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

    @Schema(description = "给自然人看的一句话说明")
    private String message;

    @Schema(description = "口径说明（审核期间新交易的付款会挂起）")
    private String scopeNote;

}
