package cn.iocoder.yudao.module.icbc.controller.admin.cardrecognition.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 卡证识别平台级参数 Response VO（#103，ADR 0037）。
 *
 * <p><b>密钥只写不读</b>：只回「已配置」与否，绝不回明文。
 *
 * <p>页面要能一眼回答「现在到底开了没」：{@link #configured} 是唯一的口径——
 * {@code provider=tencent} 且密钥齐备才算开了；{@code provider=stub} 或密钥缺失时，
 * 现场端返回空、退化为手工录入（ADR 0037 的安静降级，不是缺陷）。
 */
@Schema(description = "管理后台 - 卡证识别平台参数 Response VO")
@Data
public class CardRecognitionConfigRespVO {

    @Schema(description = "生效的供应商：stub-未启用 / tencent-腾讯云 OCR", example = "tencent")
    private String provider;

    @Schema(description = "SecretId 是否已配置（不回明文）", example = "true")
    private Boolean secretIdConfigured;

    @Schema(description = "SecretKey 是否已配置（不回明文）", example = "true")
    private Boolean secretKeyConfigured;

    @Schema(description = "生效的地域", example = "ap-guangzhou")
    private String region;

    @Schema(description = "生效的 OCR 服务域名", example = "ocr.tencentcloudapi.com")
    private String endpoint;

    @Schema(description = "生效的接口超时（毫秒）", example = "10000")
    private Integer timeout;

    @Schema(description = "哪些生效值来自配置文件（可读的中文名，便于运维分辨「我明明配了却不生效」）")
    private List<String> configFileFields;

    @Schema(description = "识别能力是否真的可用（已配置）：provider=tencent 且密钥齐备", example = "true")
    private Boolean configured;

    @Schema(description = "缺少的必填项（可读的中文名）；provider=stub 时为空，页面按「未启用」处理", example = "[]")
    private List<String> missingFields;

    @Schema(description = "最近一次自检分类：OK / AUTH_FAILED / NETWORK / VENDOR_ERROR；从未自检时为空")
    private String lastCheckResult;

    @Schema(description = "最近一次自检分类的中文名", example = "验证通过")
    private String lastCheckResultName;

    @Schema(description = "最近一次自检时间")
    private LocalDateTime lastCheckTime;

    @Schema(description = "备注")
    private String remark;

}
