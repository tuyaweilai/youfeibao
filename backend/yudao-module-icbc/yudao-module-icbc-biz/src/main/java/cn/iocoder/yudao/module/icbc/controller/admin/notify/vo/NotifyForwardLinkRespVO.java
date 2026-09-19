package cn.iocoder.yudao.module.icbc.controller.admin.notify.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 转达结算确认链接 Response VO")
@Data
public class NotifyForwardLinkRespVO {

    @Schema(description = "结算单编号")
    private Long settlementId;

    @Schema(description = "结算单号")
    private String settlementNo;

    @Schema(description = "一次性令牌")
    private String token;

    @Schema(description = "一次性令牌链接（打开即可查看，无需注册）")
    private String link;

    @Schema(description = "是否已配置自然人端入口地址（未配置时 link 为空，只能复制令牌）")
    private Boolean linkConfigured;

    @Schema(description = "令牌有效期")
    private LocalDateTime expiresTime;

    @Schema(description = "出售者手机号（脱敏；为空表示没留号）")
    private String mobileMasked;

    @Schema(description = "短信文案（可直接复制，用微信 / 电话转达）")
    private String notificationText;

    @Schema(description = "是否已发短信")
    private Boolean smsSent;

    @Schema(description = "短信结果说明")
    private String message;

}
