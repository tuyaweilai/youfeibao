package cn.iocoder.yudao.module.icbc.controller.admin.wizard.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 建档向导 - 识别银行卡请求。
 *
 * <p>回传当前表单里的卡号 / 开户行 / 是否我行卡，服务端只在空缺处回填识别结果。
 */
@Schema(description = "管理后台 - 建档向导：识别银行卡请求")
@Data
public class BankCardRecognizeReqVO {

    @Schema(description = "压缩后的银行卡照片（base64）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "银行卡影像不能为空")
    private String imageBase64;

    @Schema(description = "当前表单里的银行卡号（可为空，非空则不覆盖）")
    private String bankCardNo;

    @Schema(description = "当前表单里的开户银行（可为空，非空则不覆盖）")
    private String bankName;

    @Schema(description = "当前表单里的是否我行用户：0-非我行用户，1-我行用户（可为空，非空则不覆盖）")
    private String accountCode;

}
