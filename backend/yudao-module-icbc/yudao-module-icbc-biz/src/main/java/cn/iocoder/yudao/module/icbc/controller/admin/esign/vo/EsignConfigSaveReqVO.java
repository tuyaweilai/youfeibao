package cn.iocoder.yudao.module.icbc.controller.admin.esign.vo;

import cn.iocoder.yudao.framework.common.validation.InEnum;
import cn.iocoder.yudao.module.icbc.enums.EsignEnvironmentEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 电子签章平台级参数保存 Request VO（#92，ADR 0036）。
 *
 * <p><b>密钥只写不读</b>：{@code secretId} / {@code secretKey} / {@code callbackSignKey}
 * 留空表示「不改动既有值」，不会把已配置的密钥清掉。
 */
@Schema(description = "管理后台 - 电子签章平台参数保存 Request VO")
@Data
public class EsignConfigSaveReqVO {

    @Schema(description = "环境：TEST-测试，PROD-生产", example = "TEST")
    @InEnum(value = EsignEnvironmentEnum.class, message = "环境取值不合法，必须是 {value} 之一")
    private String environment;

    @Schema(description = "服务端接口地址", example = "https://ess.tencentcloudapi.com")
    private String apiEndpoint;

    @Schema(description = "控制台地址", example = "https://ess.tencent.cn")
    private String consoleEndpoint;

    @Schema(description = "应用标识", example = "1234567890")
    private String appId;

    @Schema(description = "应用密钥 ID（留空表示不改动）")
    private String secretId;

    @Schema(description = "应用密钥（留空表示不改动）")
    private String secretKey;

    @Schema(description = "签署状态回调地址")
    private String callbackUrl;

    @Schema(description = "回调验签密钥（留空表示不改动）")
    private String callbackSignKey;

    @Schema(description = "签署链接渠道：H5 / MINI_PROGRAM / PC", example = "H5")
    private String signLinkChannel;

    @Schema(description = "平台模板：框架收购协议")
    private String agreementTemplateId;

    @Schema(description = "平台模板：反向发票合规告知函")
    private String noticeTemplateId;

    @Schema(description = "备注")
    private String remark;

}
