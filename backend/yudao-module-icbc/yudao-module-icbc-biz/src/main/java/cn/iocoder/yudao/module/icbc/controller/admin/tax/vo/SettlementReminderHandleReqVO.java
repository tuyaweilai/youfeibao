package cn.iocoder.yudao.module.icbc.controller.admin.tax.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - 处理汇算清缴提醒 Request VO")
@Data
public class SettlementReminderHandleReqVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "编号不能为空")
    private Long id;

    @Schema(description = "状态：1-已提醒", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "状态不能为空")
    private Integer status;

    @Schema(description = "处理说明", example = "已生成免登录链接发给出售者")
    private String handleRemark;

}
