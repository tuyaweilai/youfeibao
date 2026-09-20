package cn.iocoder.yudao.module.logistics.controller.admin.transportstop.vo;

import cn.iocoder.yudao.module.logistics.controller.admin.transportnode.vo.LogisticsTransportNodeRespVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 运输停靠点 Response VO（V5 #72）。
 *
 * <p>带回该停靠点**自己的**节点、进度与断点——每个停靠点独立推进、互不相串。
 * 注意：这里只描述运输事实，**不构成把几个出售者合并结算的依据**（ADR 0031）。
 */
@Schema(description = "管理后台 - 运输停靠点 Response VO")
@Data
public class LogisticsTransportStopRespVO {

    @Schema(description = "停靠点编号")
    private Long id;

    @Schema(description = "运输任务编号")
    private Long taskId;

    @Schema(description = "运输任务单号")
    private String taskNo;

    @Schema(description = "停靠顺序（从 1 开始）")
    private Integer stopNo;

    @Schema(description = "停靠点类型：1-提货，2-送货")
    private Integer stopType;

    @Schema(description = "停靠点类型名")
    private String stopTypeName;

    @Schema(description = "出售者编号（icbc 侧编号）")
    private Long payeeId;

    @Schema(description = "出售者姓名")
    private String payeeName;

    @Schema(description = "出售者手机号")
    private String payeeMobile;

    @Schema(description = "地址")
    private String address;

    @Schema(description = "联系人")
    private String contactName;

    @Schema(description = "联系电话")
    private String contactPhone;

    @Schema(description = "货物名称（计划提示）")
    private String cargoName;

    @Schema(description = "约量（计划提示）")
    private BigDecimal estimatedQuantity;

    @Schema(description = "约量单位")
    private String quantityUnit;

    @Schema(description = "预计到站时间")
    private LocalDateTime expectedArrivalTime;

    @Schema(description = "状态：0-待处理，1-进行中，2-已完成，3-已取消")
    private Integer status;

    @Schema(description = "状态名")
    private String statusName;

    @Schema(description = "取消原因")
    private String cancelReason;

    @Schema(description = "取消时间")
    private LocalDateTime cancelTime;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "该停靠点的运输节点（按发生时间正序；列表接口为空）")
    private List<LogisticsTransportNodeRespVO> nodes;

    @Schema(description = "该停靠点还没上报的节点类型（断点，含「到达提货点/交接完成/起运」）")
    private List<String> missingNodeNames;

    @Schema(description = "该停靠点已有节点但缺凭证的说明")
    private List<String> missingEvidenceNames;

}
