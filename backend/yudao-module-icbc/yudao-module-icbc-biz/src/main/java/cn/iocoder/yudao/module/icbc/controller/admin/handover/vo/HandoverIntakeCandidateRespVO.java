package cn.iocoder.yudao.module.icbc.controller.admin.handover.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理后台 / 磅房 - 待回场复磅的现场交接登记（V6 #73）。
 *
 * <p>磅房在这一份里挑一条建批次。**现场参考量与照片凭证都在这里**（AC：磅房侧可见现场参考量与
 * 照片凭证）：参考值是现场约定的事实，不是计量事实——计量取回场复磅的有效磅次。
 */
@Schema(description = "管理后台 - 待回场复磅的现场交接登记")
@Data
public class HandoverIntakeCandidateRespVO {

    @Schema(description = "物流侧交接登记编号（建批次时回传）")
    private Long logisticsHandoverId;

    @Schema(description = "交接登记单号")
    private String handoverNo;

    @Schema(description = "运输任务编号")
    private Long taskId;

    @Schema(description = "运输任务单号")
    private String taskNo;

    @Schema(description = "停靠点编号")
    private Long stopId;

    @Schema(description = "出售者（收方）档案编号")
    private Long payeeId;

    @Schema(description = "出售者姓名快照")
    private String payeeName;

    @Schema(description = "出售者手机号快照")
    private String payeeMobile;

    @Schema(description = "品类配置编号")
    private Long goodsConfigId;

    @Schema(description = "品类名称快照")
    private String categoryName;

    @Schema(description = "计量单位快照")
    private String unit;

    @Schema(description = "现场参考量（**不是计量事实**：计量取回场复磅的有效磅次）")
    private BigDecimal referenceQuantity;

    @Schema(description = "现场参考单价（生成收购单时的单价默认值，修正要留原因）")
    private BigDecimal referenceUnitPrice;

    @Schema(description = "现场凭证照片 URL 列表")
    private List<String> photos;

    @Schema(description = "提货地址（上门提货的实际提货地址，落到收购单的交易地址）")
    private String address;

    @Schema(description = "车牌号快照")
    private String plateNo;

    @Schema(description = "司机姓名快照")
    private String driverName;

    @Schema(description = "交接发生时间")
    private LocalDateTime occurTime;

    @Schema(description = "要件状态：COMPLETE-已齐，PENDING-待补档")
    private String documentStatus;

    @Schema(description = "要件状态名")
    private String documentStatusName;

    @Schema(description = "缺什么（待补档时说明）")
    private String documentGap;

}
