package cn.iocoder.yudao.module.icbc.controller.app.appointment.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "自然人出售者端 - 可预约品类 Response VO")
@Data
public class AppointmentGoodsRespVO {

    @Schema(description = "品类配置编号")
    private Long id;

    @Schema(description = "品类名称")
    private String name;

    @Schema(description = "计量单位")
    private String unit;

}
