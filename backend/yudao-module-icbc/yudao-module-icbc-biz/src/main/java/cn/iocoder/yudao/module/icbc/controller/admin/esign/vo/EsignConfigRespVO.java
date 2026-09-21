package cn.iocoder.yudao.module.icbc.controller.admin.esign.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 电子签章平台级参数 Response VO（#92，ADR 0036）。
 *
 * <p><b>只写不读</b>：三个密钥字段只回「已配置」与否，绝不回明文——密钥只落后端，
 * 不回显到界面，也就不会随一次前端泄露把整条通道赔进去。
 */
@Schema(description = "管理后台 - 电子签章平台参数 Response VO")
@Data
public class EsignConfigRespVO {

    @Schema(description = "环境：TEST-测试，PROD-生产", example = "TEST")
    private String environment;

    @Schema(description = "服务端接口地址", example = "https://ess.tencentcloudapi.com")
    private String apiEndpoint;

    @Schema(description = "控制台地址（一次性开通链接拼在它上面）", example = "https://ess.tencent.cn")
    private String consoleEndpoint;

    @Schema(description = "应用标识", example = "1234567890")
    private String appId;

    @Schema(description = "应用密钥 ID 是否已配置（不回明文）", example = "true")
    private Boolean secretIdConfigured;

    @Schema(description = "应用密钥是否已配置（不回明文）", example = "true")
    private Boolean secretKeyConfigured;

    @Schema(description = "签署状态回调地址")
    private String callbackUrl;

    @Schema(description = "回调验签密钥是否已配置（不回明文）", example = "true")
    private Boolean callbackSignKeyConfigured;

    @Schema(description = "签署链接渠道：H5 / MINI_PROGRAM / PC", example = "H5")
    private String signLinkChannel;

    @Schema(description = "平台模板：框架收购协议")
    private String agreementTemplateId;

    @Schema(description = "平台模板：反向发票合规告知函")
    private String noticeTemplateId;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "平台参数是否已齐备（齐备才允许租户侧发起电子签署）", example = "true")
    private Boolean configured;

    @Schema(description = "尚未配置的必填项（可读的中文名，便于运维一次补齐）")
    private List<String> missingFields;

}
