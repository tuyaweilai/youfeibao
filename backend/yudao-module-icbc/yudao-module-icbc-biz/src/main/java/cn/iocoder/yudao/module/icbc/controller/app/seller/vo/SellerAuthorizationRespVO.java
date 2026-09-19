package cn.iocoder.yudao.module.icbc.controller.app.seller.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 自然人端 - 企业授权（对某一家回收企业的开票与代办税费授权）。
 *
 * <p>按企业分别管理、可自助撤销；**撤销只拦未来，已开出的票不追溯**（ADR 0017 / CONTEXT.md）。
 */
@Schema(description = "自然人端 - 企业授权")
@Data
public class SellerAuthorizationRespVO {

    @Schema(description = "租户编号", example = "1")
    private Long tenantId;

    @Schema(description = "回收企业名称", example = "某某再生资源有限公司")
    private String enterpriseName;

    @Schema(description = "本租户内的收方档案编号", example = "2048")
    private Long payeeId;

    @Schema(description = "是否授权反向开票", example = "true")
    private Boolean reverseInvoiceAuthorized;

    @Schema(description = "是否授权代办税费", example = "true")
    private Boolean taxAgencyAuthorized;

    @Schema(description = "是否已撤销", example = "false")
    private Boolean revoked;

    @Schema(description = "授权时间")
    private LocalDateTime authorizedAt;

    @Schema(description = "撤销时间")
    private LocalDateTime revokedAt;

    @Schema(description = "撤销原因")
    private String revokeReason;

}
