package cn.iocoder.yudao.module.logistics.controller.admin.transporthandover.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 / 司机端 - 交接登记 Response VO")
@Data
public class LogisticsTransportHandoverRespVO {

    @Schema(description = "交接登记编号")
    private Long id;

    @Schema(description = "交接登记单号")
    private String handoverNo;

    @Schema(description = "运输任务编号")
    private Long taskId;

    @Schema(description = "运输任务单号")
    private String taskNo;

    @Schema(description = "停靠点编号")
    private Long stopId;

    @Schema(description = "出售者编号（icbc 侧收方档案编号）")
    private Long payeeId;

    @Schema(description = "出售者姓名快照")
    private String payeeName;

    @Schema(description = "出售者手机号快照")
    private String payeeMobile;

    @Schema(description = "品类配置编号（icbc 侧编号）")
    private Long goodsConfigId;

    @Schema(description = "品类名称快照")
    private String categoryName;

    @Schema(description = "计量单位快照")
    private String unit;

    @Schema(description = "参考量（现场约定值，不是计量事实）")
    private BigDecimal referenceQuantity;

    @Schema(description = "参考单价（现场约定值）")
    private BigDecimal referenceUnitPrice;

    @Schema(description = "凭证照片 URL 列表")
    private List<String> photos;

    @Schema(description = "司机编号")
    private Long driverId;

    @Schema(description = "司机姓名快照")
    private String driverName;

    @Schema(description = "司机手机号快照")
    private String driverMobile;

    @Schema(description = "车辆编号")
    private Long vehicleId;

    @Schema(description = "车牌号快照")
    private String plateNo;

    @Schema(description = "交接发生时间")
    private LocalDateTime occurTime;

    @Schema(description = "要件状态：COMPLETE-已齐，PENDING-待补档")
    private String documentStatus;

    @Schema(description = "要件状态名")
    private String documentStatusName;

    @Schema(description = "缺什么（待补档时说明）")
    private String documentGap;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
