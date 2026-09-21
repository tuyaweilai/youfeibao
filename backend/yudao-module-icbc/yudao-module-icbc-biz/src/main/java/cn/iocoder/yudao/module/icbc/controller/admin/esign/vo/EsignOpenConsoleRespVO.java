package cn.iocoder.yudao.module.icbc.controller.admin.esign.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 电子签章开通链接 Response VO（#92）。
 *
 * <p>管理员点「开通电子签」拿到的一枚**一次性控制台链接**：企业认证与创建企业印章都在第三方侧完成，
 * 平台不代盖、也不代做认证。链接现生成现用，再次开通会换新。
 */
@Schema(description = "管理后台 - 电子签章开通链接 Response VO")
@Data
public class EsignOpenConsoleRespVO {

    @Schema(description = "本租户的子客编号", example = "ES0000000001")
    private String subCustomerNo;

    @Schema(description = "一次性控制台链接（完成企业认证与创建企业印章）")
    private String link;

    @Schema(description = "本次链接的一次性令牌：确认激活时原样带回；重新开通会换新令牌，旧链接随之作废")
    private String consoleToken;

    @Schema(description = "链接有效期止")
    private LocalDateTime expiresTime;

    @Schema(description = "当前开通状态：0-未开通，1-认证中，2-已激活", example = "1")
    private Integer activationStatus;

    @Schema(description = "开通状态名", example = "认证中")
    private String activationStatusName;

    @Schema(description = "下一步该做什么（可直接展示给管理员）")
    private String nextStep;

}
