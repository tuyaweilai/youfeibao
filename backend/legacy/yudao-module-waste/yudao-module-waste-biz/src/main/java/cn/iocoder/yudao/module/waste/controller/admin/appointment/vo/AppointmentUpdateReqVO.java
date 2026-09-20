package cn.iocoder.yudao.module.waste.controller.admin.appointment.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - 危废转移预约更新 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AppointmentUpdateReqVO extends AppointmentCreateReqVO {

    @Schema(description = "预约ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "预约ID不能为空")
    private Long id;

} 