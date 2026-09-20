package cn.iocoder.yudao.module.logistics.controller.admin.transportnode.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 运输节点 Response VO")
@Data
public class LogisticsTransportNodeRespVO {

    @Schema(description = "节点编号")
    private Long id;

    @Schema(description = "运输任务编号")
    private Long taskId;

    @Schema(description = "运输任务单号")
    private String taskNo;

    @Schema(description = "停靠点编号（提货相关节点非空；到达场站/卸货完成为整趟收尾，为空）")
    private Long stopId;

    @Schema(description = "节点类型：1-到达提货点，2-交接完成，3-起运，4-到达场站，5-卸货完成")
    private Integer nodeType;

    @Schema(description = "节点类型名")
    private String nodeTypeName;

    @Schema(description = "发生时间")
    private LocalDateTime nodeTime;

    @Schema(description = "上报时间")
    private LocalDateTime reportTime;

    @Schema(description = "位置描述")
    private String location;

    @Schema(description = "纬度")
    private BigDecimal latitude;

    @Schema(description = "经度")
    private BigDecimal longitude;

    @Schema(description = "凭证照片 URL 列表")
    private List<String> photos;

    @Schema(description = "上报人编号")
    private Long operatorId;

    @Schema(description = "上报人姓名")
    private String operatorName;

    @Schema(description = "异常类型（空 = 正常节点）：1-车辆故障，2-交通事故，3-天气延误，4-道路封闭，5-货物损坏，6-对方不在，7-地址错误，8-其他")
    private Integer abnormalType;

    @Schema(description = "异常类型名")
    private String abnormalTypeName;

    @Schema(description = "异常说明")
    private String abnormalReason;

    @Schema(description = "异常是否已解决")
    private Boolean abnormalResolved;

    @Schema(description = "异常解决时间")
    private LocalDateTime abnormalResolvedAt;

    @Schema(description = "异常解决人姓名")
    private String abnormalResolvedName;

    @Schema(description = "异常解决说明")
    private String abnormalResolvedRemark;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
