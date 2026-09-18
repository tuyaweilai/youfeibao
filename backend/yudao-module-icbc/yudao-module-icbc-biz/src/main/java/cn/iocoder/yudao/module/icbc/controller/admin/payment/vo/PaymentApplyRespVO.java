package cn.iocoder.yudao.module.icbc.controller.admin.payment.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 发起付款响应 VO。
 *
 * <p>{@link #payPageHtml} 是工行返回的企业支付页面自动提交表单，由前端用
 * {@code openIcbcForm()} 打开，回收企业在页面上授权后货款直付到出售者本人银行卡。
 * 平台不接触资金，也不持有资金。
 */
@Schema(description = "管理后台 - 发起付款响应")
@Data
public class PaymentApplyRespVO {

    @Schema(description = "是否成功生成 / 复用支付页面", example = "true")
    private Boolean success;

    @Schema(description = "是否为重复发起（未重复提交工行）", example = "false")
    private Boolean duplicate;

    @Schema(description = "结果说明", example = "已生成企业支付页面")
    private String message;

    @Schema(description = "合作方订单号", example = "ACQ202601011200001234")
    private String partnerOrderId;

    @Schema(description = "支付订单号", example = "PAY202601011200001234")
    private String orderNo;

    @Schema(description = "收购单编号", example = "1024")
    private Long acquisitionId;

    @Schema(description = "应付金额（元）", example = "1000.00")
    private BigDecimal payAmount;

    @Schema(description = "企业支付页面表单 HTML")
    private String payPageHtml;

    @Schema(description = "支付状态值", example = "0")
    private Integer paymentStatus;

    @Schema(description = "支付状态名", example = "待支付")
    private String paymentStatusName;

    @Schema(description = "是否可由用户重新发起", example = "false")
    private Boolean reInitiable;

    @Schema(description = "错误码", example = "1030004010")
    private String errorCode;

    @Schema(description = "错误信息")
    private String errorMsg;

}
