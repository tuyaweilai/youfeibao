package cn.iocoder.yudao.module.icbc.controller.admin.appointment.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 到站预约分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AppointmentPageReqVO extends PageParam {

    @Schema(description = "状态：0-待到站，1-已到场，2-未到场，9-已取消")
    private Integer status;

    @Schema(description = "场站编号")
    private Long stationId;

    @Schema(description = "收方（出售者）档案编号")
    private Long payeeId;

    @Schema(description = "自然人主体编号")
    private Long naturalPersonId;

    @Schema(description = "车牌号")
    private String plateNo;

    @Schema(description = "预计到站时间起")
    private LocalDateTime arrivalTimeStart;

    @Schema(description = "预计到站时间止")
    private LocalDateTime arrivalTimeEnd;

}
