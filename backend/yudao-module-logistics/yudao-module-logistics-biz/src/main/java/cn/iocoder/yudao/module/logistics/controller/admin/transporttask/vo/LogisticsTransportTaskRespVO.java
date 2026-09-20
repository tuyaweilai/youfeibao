package cn.iocoder.yudao.module.logistics.controller.admin.transporttask.vo;

import cn.iocoder.yudao.module.logistics.controller.admin.transportnode.vo.LogisticsTransportNodeRespVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 运输任务 Response VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class LogisticsTransportTaskRespVO extends LogisticsTransportTaskSaveReqVO {

    @Schema(description = "任务单号", example = "TT202609201200001234")
    private String taskNo;

    @Schema(description = "任务状态：0-待分配，1-已分配，2-已接单，3-执行中，4-已完成，5-已取消")
    private Integer status;

    @Schema(description = "任务状态名")
    private String statusName;

    @Schema(description = "车牌号快照")
    private String plateNo;

    @Schema(description = "司机姓名快照")
    private String driverName;

    @Schema(description = "司机手机号快照")
    private String driverMobile;

    @Schema(description = "派车时间")
    private LocalDateTime assignTime;

    @Schema(description = "授权放行原因（证件过期时管理员带着原因放行，正常派车为空）")
    private String overrideReason;

    @Schema(description = "授权放行人（系统用户编号）")
    private Long overrideBy;

    @Schema(description = "授权放行时间")
    private LocalDateTime overrideTime;

    @Schema(description = "接单时间")
    private LocalDateTime acceptTime;

    @Schema(description = "起运时间")
    private LocalDateTime startTime;

    @Schema(description = "完成时间")
    private LocalDateTime completeTime;

    @Schema(description = "取消时间")
    private LocalDateTime cancelTime;

    @Schema(description = "取消原因")
    private String cancelReason;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "运输节点（按发生时间正序；详情接口才有，列表为空）")
    private List<LogisticsTransportNodeRespVO> nodes;

    @Schema(description = "还没上报的节点类型（断点，按流程顺序；详情接口才有）")
    private List<String> missingNodeNames;

    @Schema(description = "已有节点但缺凭证的说明（断点，如「交接完成缺照片」；详情接口才有）")
    private List<String> missingEvidenceNames;

    @Schema(description = "改派承接记录（按改派时间正序；没有改派过则为空；详情接口才有）")
    private List<LogisticsTransportTaskReassignRespVO> reassigns;

}
