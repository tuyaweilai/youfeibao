package cn.iocoder.yudao.module.icbc.controller.admin.appointment.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - 标记未到场 Request VO")
@Data
public class AppointmentNoShowReqVO {

    @Schema(description = "预约编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "预约编号不能为空")
    private Long id;

    @Schema(description = "未到场说明（选填）")
    private String reason;

}
