package cn.iocoder.yudao.module.icbc.controller.admin.cardrecognition.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Min;

/**
 * 卡证识别连通性自检 Request VO（#103）。
 *
 * <p>允许在请求体里带密钥：**留空 = 用库里已存的那份**。这样「先验证、再保存」能成立——
 * 运维填完密钥先点一次自检，通过了再保存。
 */
@Schema(description = "管理后台 - 卡证识别连通性自检 Request VO")
@Data
public class CardRecognitionCheckReqVO {

    @Schema(description = "待验证的 SecretId（留空表示用已存值）")
    private String secretId;

    @Schema(description = "待验证的 SecretKey（留空表示用已存值）")
    private String secretKey;

    @Schema(description = "待验证的地域（留空表示用已存值）", example = "ap-guangzhou")
    private String region;

    @Schema(description = "待验证的 OCR 服务域名（留空表示用已存值）", example = "ocr.tencentcloudapi.com")
    private String endpoint;

    @Schema(description = "待验证的接口超时（毫秒，留空表示用已存值）", example = "10000")
    @Min(value = 1, message = "接口超时必须是正整数毫秒")
    private Integer timeout;

}
