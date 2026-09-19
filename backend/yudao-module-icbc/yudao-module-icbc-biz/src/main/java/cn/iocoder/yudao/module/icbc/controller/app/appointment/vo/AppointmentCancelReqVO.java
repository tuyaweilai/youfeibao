package cn.iocoder.yudao.module.icbc.controller.app.appointment.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Schema(description = "自然人出售者端 - 取消到站预约 Request VO")
@Data
public class AppointmentCancelReqVO {

    @Schema(description = "自然人主体编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "自然人主体编号不能为空")
    private Long naturalPersonId;

    @Schema(description = "预约编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "预约编号不能为空")
    private Long id;

    @Schema(description = "取消原因（选填）")
    private String reason;

}
