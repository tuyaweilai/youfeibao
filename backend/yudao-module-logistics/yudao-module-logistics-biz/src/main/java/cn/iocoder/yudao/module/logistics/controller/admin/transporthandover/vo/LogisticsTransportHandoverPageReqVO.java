package cn.iocoder.yudao.module.logistics.controller.admin.transporthandover.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 交接登记分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class LogisticsTransportHandoverPageReqVO extends PageParam {

    @Schema(description = "运输任务编号", example = "1")
    private Long taskId;

    @Schema(description = "停靠点编号", example = "1")
    private Long stopId;

    @Schema(description = "出售者编号（icbc 侧收方档案编号）", example = "1024")
    private Long payeeId;

    @Schema(description = "要件状态：COMPLETE-已齐，PENDING-待补档", example = "PENDING")
    private String documentStatus;

    @Schema(description = "车牌号（模糊匹配）", example = "京A12345")
    private String plateNo;

    @Schema(description = "交接发生时间（起）")
    private LocalDateTime occurTimeStart;

    @Schema(description = "交接发生时间（止）")
    private LocalDateTime occurTimeEnd;

}
