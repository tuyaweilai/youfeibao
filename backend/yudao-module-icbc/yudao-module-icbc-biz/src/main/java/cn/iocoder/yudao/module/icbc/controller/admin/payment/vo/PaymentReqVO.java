package cn.iocoder.yudao.module.icbc.controller.admin.payment.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 工行付方支付请求 VO
 *
 * @author 芋道源码
 */
@Schema(description = "管理后台 - 工行付方支付请求")
@Data
public class PaymentReqVO {

    @Schema(description = "合作方编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "10000000000000261019")
    @NotBlank(message = "合作方编号不能为空")
    @Size(max = 20, message = "合作方编号长度不能超过20个字符")
    private String appId;

    @Schema(description = "合作方订单ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "2018040908")
    @NotBlank(message = "合作方订单ID不能为空")
    @Size(max = 35, message = "合作方订单ID长度不能超过35个字符")
    private String outOrderId;

    @Schema(description = "付方编号（付方平台外部编号 / 子商户编号，即回收企业）", example = "010020200513111111")
    @Size(max = 40, message = "付方编号长度不能超过40个字符")
    private String outVendorId;

    @Schema(description = "收方编号（外部用户编号，即自然人出售者）", example = "10000000000000003")
    @Size(max = 20, message = "收方编号长度不能超过20个字符")
    private String outUserId;

    @Schema(description = "机构编码，场景支付时必输", example = "20201128531215026")
    @Size(max = 30, message = "机构编码长度不能超过30个字符")
    private String verifiedCode;

    @Schema(description = "U盾ID，场景支付时必输", example = "20201128531215026")
    @Size(max = 24, message = "U盾ID长度不能超过24个字符")
    private String ukeyId;

} 