package cn.iocoder.yudao.module.icbc.controller.admin.payment.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * 发起付款请求 VO。
 *
 * <p>付款的对象是一笔「已预开票成功」的收购：要么给收购单编号，要么给这笔收购的开票合作方订单号
 * （即收购单号）。金额由收购单决定，调用方可以显式带上 {@link #amount}，
 * 一旦与收购单金额不一致即被拦下（见 issue #9 验收）。
 */
@Schema(description = "管理后台 - 发起付款请求")
@Data
public class PaymentApplyReqVO {

    @Schema(description = "收购单编号", example = "1024")
    private Long acquisitionId;

    @Schema(description = "开票合作方订单号（等于收购单号）", example = "ACQ202601011200001234")
    @Size(max = 64, message = "合作方订单号长度不能超过 64 个字符")
    private String partnerOrderId;

    @Schema(description = "本次付款金额（元），必须等于收购单金额；不填则按收购单金额付款", example = "1000.00")
    private BigDecimal amount;

    @Schema(description = "机构编码，场景支付时必输", example = "20201128531215026")
    @Size(max = 30, message = "机构编码长度不能超过 30 个字符")
    private String verifiedCode;

    @Schema(description = "U盾ID，场景支付时必输", example = "20201128531215026")
    @Size(max = 24, message = "U盾ID长度不能超过 24 个字符")
    private String ukeyId;

}
