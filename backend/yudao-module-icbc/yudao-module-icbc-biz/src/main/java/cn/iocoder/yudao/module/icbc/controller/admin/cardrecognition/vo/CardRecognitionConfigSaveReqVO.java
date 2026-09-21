package cn.iocoder.yudao.module.icbc.controller.admin.cardrecognition.vo;

import cn.iocoder.yudao.framework.common.validation.InEnum;
import cn.iocoder.yudao.module.icbc.enums.CardRecognitionProviderEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Min;

/**
 * 卡证识别平台级参数保存 Request VO（#103，ADR 0037）。
 *
 * <p><b>密钥只写不读</b>：{@code secretId} / {@code secretKey} 留空表示「不改动既有值」，
 * 不会把已配置的密钥清掉。
 *
 * <p>本 VO 的字段名（{@code secretId} / {@code secretKey}）虽然已在平台的默认脱敏名单里，
 * 保存接口仍显式 {@code @ApiAccessLog(requestEnable = false)}：密钥只该走「整段不记」，
 * 不该把安全寄托在「字段名碰巧命中」上（#98 的口径）。
 */
@Schema(description = "管理后台 - 卡证识别平台参数保存 Request VO")
@Data
public class CardRecognitionConfigSaveReqVO {

    @Schema(description = "供应商：stub-未启用 / tencent-腾讯云 OCR；留空表示不改动既有值", example = "tencent")
    @InEnum(value = CardRecognitionProviderEnum.class, message = "供应商取值不合法，必须是 {value} 之一")
    private String provider;

    @Schema(description = "腾讯云 API 密钥 SecretId（留空表示不改动）")
    private String secretId;

    @Schema(description = "腾讯云 API 密钥 SecretKey（留空表示不改动）")
    private String secretKey;

    @Schema(description = "地域（留空表示不改动；未配置过时用配置文件 / 默认 ap-guangzhou）", example = "ap-guangzhou")
    private String region;

    @Schema(description = "OCR 服务域名（留空表示不改动）", example = "ocr.tencentcloudapi.com")
    private String endpoint;

    @Schema(description = "接口超时（毫秒，留空表示不改动）", example = "10000")
    @Min(value = 1, message = "接口超时必须是正整数毫秒")
    private Integer timeout;

    @Schema(description = "备注")
    private String remark;

}
