package cn.iocoder.yudao.module.icbc.controller.admin.esign.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 确认电子签章已激活 Request VO（#92）。
 *
 * <p>企业在第三方控制台完成企业认证并创建企业印章后，回到后台确认——「已激活 + 印章就位」
 * 两个条件同时成立才落 {@code ACTIVATED}：只激活没印章，发起不了签署。
 */
@Schema(description = "管理后台 - 确认电子签章已激活 Request VO")
@Data
public class EsignActivateReqVO {

    @Schema(description = "经办人编号（第三方侧的企业办事人）", example = "OP1001")
    private String operatorNo;

    @Schema(description = "企业印章编号（印章就位的凭据，必填）", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "SEAL20260921001")
    @NotBlank(message = "企业印章编号不能为空")
    private String sealNo;

    @Schema(description = "备注")
    private String remark;

}
