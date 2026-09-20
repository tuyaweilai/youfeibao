package cn.iocoder.yudao.module.logistics.controller.admin.transportnode.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 上报运输异常（V4 #71）。
 *
 * <p>异常是**独立标记**，不是任务状态：它走自己的上报入口，不推进也不回退状态机。
 * 一次异常仍然是一条「事实」——有发生时间 / 上报时间 / 位置 / 照片 / 上报人，只是没有节点类型
 * （车辆故障不发生在五个步骤中的某一步，它发生在路上）。
 */
@Schema(description = "管理后台 - 运输异常上报 Request VO")
@Data
public class LogisticsTransportAbnormalReportReqVO {

    @Schema(description = "运输任务编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "运输任务编号不能为空")
    private Long taskId;

    @Schema(description = "停靠点编号（异常发生在某一停靠点时填；路上发生可为空）", example = "1")
    private Long stopId;

    @Schema(description = "异常类型：1-车辆故障，2-交通事故，3-天气延误，4-道路封闭，5-货物损坏，6-对方不在，7-地址错误，8-其他",
            requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "异常类型不能为空")
    private Integer abnormalType;

    @Schema(description = "异常说明", requiredMode = Schema.RequiredMode.REQUIRED, example = "发动机水温过高，已靠边停车等拖车")
    @NotEmpty(message = "异常说明不能为空")
    private String abnormalReason;

    @Schema(description = "发生时间：事情实际发生的时刻（弱网补录时可能早于上报时间）",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "发生时间不能为空")
    private LocalDateTime nodeTime;

    @Schema(description = "位置描述", example = "绕城高速北段")
    private String location;

    @Schema(description = "纬度")
    private BigDecimal latitude;

    @Schema(description = "经度")
    private BigDecimal longitude;

    @Schema(description = "现场照片 URL 列表")
    private List<String> photos;

    @Schema(description = "客户端请求号（幂等键，重复提交只落一条）",
            requiredMode = Schema.RequiredMode.REQUIRED, example = "5f1e2d3c-...")
    @NotEmpty(message = "客户端请求号不能为空")
    private String clientRequestId;

    @Schema(description = "备注")
    private String remark;

}
