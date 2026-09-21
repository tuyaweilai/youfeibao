package cn.iocoder.yudao.module.icbc.controller.admin.wizard.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 建档向导 - 识别身份证国徽面请求：带出证件签发日期与证件有效期。
 *
 * <p>这两个字段是工行收方入驻的必输项，也是「抄错一个就开不出票」的高危字段（见 #81）。
 * 图片仍是 base64 直传、不留存。
 */
@Schema(description = "管理后台 - 建档向导：识别身份证国徽面请求")
@Data
public class IdCardBackRecognizeReqVO {

    @Schema(description = "压缩后的身份证国徽面照片（base64）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "证件影像不能为空")
    private String imageBase64;

    @Schema(description = "当前表单里的证件签发日期 yyyy-MM-dd（可为空，非空则不覆盖）")
    private String idSignDate;

    @Schema(description = "当前表单里的证件有效期截止 yyyy-MM-dd（可为空，非空则不覆盖）")
    private String idValidityPeriod;

}
