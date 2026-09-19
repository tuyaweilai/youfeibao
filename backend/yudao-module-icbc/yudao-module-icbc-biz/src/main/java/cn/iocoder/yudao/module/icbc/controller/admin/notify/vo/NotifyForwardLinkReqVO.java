package cn.iocoder.yudao.module.icbc.controller.admin.notify.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 收货员「一键把确认链接转达给出售者」（#36，ADR 0023）。
 *
 * <p>首次交易、从未留手机号的场景，只有收货员当场把链接给他这一条通路，所以这是必需功能。
 * {@code sendSms=true} 时顺带用短信发一条（**由人显式触发**，不受自动开关影响）；否则只把链接返回给收货员复制转达。
 */
@Schema(description = "管理后台 - 转达结算确认链接 Request VO")
@Data
public class NotifyForwardLinkReqVO {

    @Schema(description = "结算单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "结算单编号不能为空")
    private Long settlementId;

    @Schema(description = "是否同时发短信（默认为否；由收货员显式触发）", example = "false")
    private Boolean sendSms;

}
