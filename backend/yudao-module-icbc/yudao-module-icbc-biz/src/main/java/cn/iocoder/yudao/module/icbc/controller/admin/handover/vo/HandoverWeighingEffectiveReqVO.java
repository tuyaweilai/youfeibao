package cn.iocoder.yudao.module.icbc.controller.admin.handover.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 管理后台 / 收货员现场端 - 指定有效磅次 Request VO（#50 T12）。
 *
 * <p>「哪一次参与计量」是一个显式动作：同一批次至多一次有效，其余留档但不参与。
 * 该批次一旦产生收购单，计量结果已引用当时那一版，不能再改。
 */
@Schema(description = "管理后台 - 指定有效磅次 Request VO")
@Data
public class HandoverWeighingEffectiveReqVO {

    @Schema(description = "交接批次编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    @NotNull(message = "交接批次不能为空")
    private Long batchId;

    @Schema(description = "要指定为有效的磅次编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "4096")
    @NotNull(message = "磅次不能为空")
    private Long weighingId;

    @Schema(description = "指定原因（选填，如「复磅后以第二次为准」）")
    private String reason;

}
