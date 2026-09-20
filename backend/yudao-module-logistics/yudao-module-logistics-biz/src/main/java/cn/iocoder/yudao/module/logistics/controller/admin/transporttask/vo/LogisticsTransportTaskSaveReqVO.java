package cn.iocoder.yudao.module.logistics.controller.admin.transporttask.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 运输任务新增/修改 Request VO")
@Data
public class LogisticsTransportTaskSaveReqVO {

    @Schema(description = "主键（修改时必填）", example = "1")
    private Long id;

    @Schema(description = "车辆编号（留空即待分配；填了就必须同时填司机）", example = "1")
    private Long vehicleId;

    @Schema(description = "司机编号（留空即待分配；填了就必须同时填车辆）", example = "1")
    private Long driverId;

    @Schema(description = "出发地", example = "城东场站")
    private String departureAddress;

    @Schema(description = "提货点地址", requiredMode = Schema.RequiredMode.REQUIRED, example = "某某路 1 号")
    @NotEmpty(message = "提货点地址不能为空")
    private String pickupAddress;

    @Schema(description = "提货点联系人", example = "张三")
    private String pickupContactName;

    @Schema(description = "提货点联系电话", example = "13800138000")
    private String pickupContactPhone;

    @Schema(description = "时间窗开始")
    private LocalDateTime expectedStartTime;

    @Schema(description = "时间窗结束")
    private LocalDateTime expectedEndTime;

    @Schema(description = "关联采购订单编号（可空：什么都不挂也能派车）", example = "1")
    private Long purchaseOrderId;

    @Schema(description = "采购订单号快照", example = "PO202609201200001234")
    private String purchaseOrderNo;

    @Schema(description = "备注")
    private String remark;

}
