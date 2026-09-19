package cn.iocoder.yudao.module.icbc.controller.admin.appointment.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - 标记到场 Request VO")
@Data
public class AppointmentArriveReqVO {

    @Schema(description = "预约编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "预约编号不能为空")
    private Long id;

    @Schema(description = "到场后建的收购单编号（选填；只是关联，预约本身不产生收购事实）")
    private Long acquisitionId;

}
