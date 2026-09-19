package cn.iocoder.yudao.module.icbc.controller.app.sellerauth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 自然人出售者 - 绑定本次交易涉及的收方档案对应的自然人主体 Request VO。
 *
 * <p>这就是「注册挂在确认结算这一步」的落地：他已经在看某一笔货的结算单，登录后把
 * 「这笔货的出售者」绑到当前登录凭证上。同一身份证已绑别的手机号时**拒绝、不合并**（ADR 0017）。
 */
@Schema(description = "自然人出售者 - 绑定自然人主体 Request VO")
@Data
public class SellerSubjectBindReqVO {

    @Schema(description = "收方档案编号（本租户内）", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "收方档案编号不能为空")
    private Long payeeId;

}
