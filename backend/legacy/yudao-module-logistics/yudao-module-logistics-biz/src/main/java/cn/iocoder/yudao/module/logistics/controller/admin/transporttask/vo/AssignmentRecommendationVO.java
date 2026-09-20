package cn.iocoder.yudao.module.logistics.controller.admin.transporttask.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - 智能推荐任务分配 Response VO")
@Data
public class AssignmentRecommendationVO {

    @Schema(description = "车辆ID", example = "101")
    private Long vehicleId;

    @Schema(description = "车牌号", example = "京A12345")
    private String plateNumber;

    @Schema(description = "车辆类型", example = "危废运输车")
    private String vehicleType;

    @Schema(description = "载重能力(吨)", example = "15.5")
    private BigDecimal capacityKg;

    @Schema(description = "司机ID", example = "201")
    private Long driverId;

    @Schema(description = "司机姓名", example = "张三")
    private String driverName;

    @Schema(description = "司机电话", example = "13800138000")
    private String driverPhone;

    @Schema(description = "推荐评分", example = "8.5")
    private BigDecimal recommendScore;

    @Schema(description = "距离(公里)", example = "12.5")
    private BigDecimal distance;

    @Schema(description = "预计到达时间(分钟)", example = "30")
    private Integer estimatedArrivalTime;

    @Schema(description = "当前位置", example = "北京市朝阳区")
    private String currentLocation;

    @Schema(description = "车辆状态", example = "0")
    private Integer vehicleStatus; // 0:可用, 1:运输中, 2:维护中

    @Schema(description = "司机状态", example = "0")
    private Integer driverStatus; // 0:在职, 1:离职, 2:请假

    @Schema(description = "推荐理由", example = "距离最近，载重匹配")
    private String recommendReason;
} 