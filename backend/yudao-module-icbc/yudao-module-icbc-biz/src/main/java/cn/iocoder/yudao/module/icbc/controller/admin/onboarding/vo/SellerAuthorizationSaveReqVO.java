package cn.iocoder.yudao.module.icbc.controller.admin.onboarding.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * 出售者首次授权请求
 *
 * <p>反向开票与代办税费是两个独立授权，都必须显式勾选才视为完成。
 */
@Schema(description = "管理后台 - 出售者首次授权请求")
@Data
public class SellerAuthorizationSaveReqVO {

    @Schema(description = "出售者（收方）编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "出售者编号不能为空")
    private Long payeeId;

    @Schema(description = "是否授权反向开票", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
    @NotNull(message = "反向开票授权不能为空")
    private Boolean reverseInvoiceAuthorized;

    @Schema(description = "是否授权代办税费", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
    @NotNull(message = "代办税费授权不能为空")
    private Boolean taxAgencyAuthorized;

    @Schema(description = "授权渠道：ONSITE-收购现场，ICBC_H5-工行页面", example = "ICBC_H5")
    @Size(max = 20, message = "授权渠道长度不能超过20个字符")
    private String channel;

    @Schema(description = "现场办理人（收货员）", example = "李四")
    @Size(max = 64, message = "办理人长度不能超过64个字符")
    private String operator;

    @Schema(description = "留痕附件地址")
    @Size(max = 500, message = "留痕附件地址长度不能超过500个字符")
    private String evidenceUrl;

    @Schema(description = "授权时间（留空取当前时间）")
    private LocalDateTime authorizedAt;

    @Schema(description = "备注")
    @Size(max = 500, message = "备注长度不能超过500个字符")
    private String remark;

}
