package cn.iocoder.yudao.module.icbc.controller.admin.notify.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 出售者触达设置 Response VO")
@Data
public class NotifySettingRespVO {

    @Schema(description = "平台级开关（配置 icbc.notify.sms-enabled）")
    private Boolean platformEnabled;

    @Schema(description = "本租户级开关")
    private Boolean tenantEnabled;

    @Schema(description = "实际是否生效（平台或本租户任一打开即生效）")
    private Boolean effectiveEnabled;

    @Schema(description = "是否已配置自然人端入口地址（没配就拼不出链接）")
    private Boolean sellerAppUrlConfigured;

    @Schema(description = "说明")
    private String remark;

}
