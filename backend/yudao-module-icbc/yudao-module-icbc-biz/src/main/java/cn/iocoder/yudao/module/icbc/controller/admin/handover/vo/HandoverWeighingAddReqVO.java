package cn.iocoder.yudao.module.icbc.controller.admin.handover.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 管理后台 / 收货员现场端 - 新增一次磅次 Request VO（#50 T12）。
 *
 * <p>每一次过磅都留一条：复磅、拆装、重称都走这里再次提交。**第一次磅次自动成为有效磅次**，
 * 之后要改由 {@link HandoverWeighingEffectiveReqVO} 显式指定。
 */
@Schema(description = "管理后台 - 磅次新增 Request VO")
@Data
public class HandoverWeighingAddReqVO {

    @Schema(description = "交接批次编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    @NotNull(message = "交接批次不能为空")
    private Long batchId;

    @Schema(description = "毛重", requiredMode = Schema.RequiredMode.REQUIRED, example = "18000.00")
    @NotNull(message = "毛重不能为空")
    private BigDecimal grossWeight;

    @Schema(description = "皮重", requiredMode = Schema.RequiredMode.REQUIRED, example = "5500.00")
    @NotNull(message = "皮重不能为空")
    private BigDecimal tareWeight;

    @Schema(description = "过磅时间；不填取登记时刻")
    private LocalDateTime weighTime;

    @Schema(description = "磅单号", example = "WD20261201001")
    private String weightTicketNo;

    @Schema(description = "磅单照片地址")
    private String weightTicketImageUrl;

    @Schema(description = "磅单上的车牌号（与批次车牌不一致时能被看出来）", example = "京A12345")
    private String plateNo;

    @Schema(description = "备注（如复磅原因）")
    private String remark;

}
