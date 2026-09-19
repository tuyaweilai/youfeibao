package cn.iocoder.yudao.module.icbc.controller.admin.notify.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - 保存出售者触达设置 Request VO")
@Data
public class NotifySettingSaveReqVO {

    @Schema(description = "本租户是否开启短信触达", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
    @NotNull(message = "是否开启不能为空")
    private Boolean smsEnabled;

    @Schema(description = "备注", example = "运营同意承担短信费用")
    private String remark;

}
