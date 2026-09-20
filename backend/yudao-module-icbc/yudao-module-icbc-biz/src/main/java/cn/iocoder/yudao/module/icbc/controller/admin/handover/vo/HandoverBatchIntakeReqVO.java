package cn.iocoder.yudao.module.icbc.controller.admin.handover.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 管理后台 / 磅房 - 按现场交接登记回场复磅 Request VO（V6 #73）。
 *
 * <p>上门提货的闭环第一段：磅房在**派单场站**按司机现场登记的交接选一条，录入回场过磅读数，
 * 系统据此建交接批次（把现场参考量 / 参考单价、要件状态、司机与车辆引用搬进批次）并落第一次磅次。
 *
 * <p>**场站必填且是派单场站**：收购单与结算单归派单场站，实际提货地址另存交易地址（ADR 0031），
 * 所以这里不允许用「上门地址」顶替场站，也不引入虚拟场站。
 */
@Schema(description = "管理后台 - 按现场交接登记回场复磅 Request VO")
@Data
public class HandoverBatchIntakeReqVO {

    @Schema(description = "物流侧交接登记编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "3072")
    @NotNull(message = "现场交接登记不能为空")
    private Long logisticsHandoverId;

    @Schema(description = "派单场站编号（收购单与结算单归它）", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    @NotNull(message = "派单场站不能为空")
    private Long stationId;

    @Schema(description = "毛重（回场复磅读数）", requiredMode = Schema.RequiredMode.REQUIRED, example = "18000.00")
    @NotNull(message = "毛重不能为空")
    private BigDecimal grossWeight;

    @Schema(description = "皮重（回场复磅读数）", requiredMode = Schema.RequiredMode.REQUIRED, example = "5500.00")
    @NotNull(message = "皮重不能为空")
    private BigDecimal tareWeight;

    @Schema(description = "过磅时间；不填取登记时刻")
    private LocalDateTime weighTime;

    @Schema(description = "磅单号", example = "WD20261201001")
    private String weightTicketNo;

    @Schema(description = "磅单照片地址")
    private String weightTicketImageUrl;

    @Schema(description = "磅单上的车牌号；不填取现场交接登记上的车牌", example = "京A12345")
    private String plateNo;

    @Schema(description = "备注（如复磅原因）")
    private String remark;

}
