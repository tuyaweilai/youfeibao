package cn.iocoder.yudao.module.icbc.controller.admin.onboarding.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 出售者首次授权返回
 */
@Schema(description = "管理后台 - 出售者首次授权返回")
@Data
public class SellerAuthorizationRespVO {

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "出售者（收方）编号")
    private Long payeeId;

    @Schema(description = "是否授权反向开票")
    private Boolean reverseInvoiceAuthorized;

    @Schema(description = "是否授权代办税费")
    private Boolean taxAgencyAuthorized;

    @Schema(description = "授权时间")
    private LocalDateTime authorizedAt;

    @Schema(description = "授权渠道")
    private String channel;

    @Schema(description = "现场办理人")
    private String operator;

    @Schema(description = "留痕附件地址")
    private String evidenceUrl;

    @Schema(description = "备注")
    private String remark;

}
