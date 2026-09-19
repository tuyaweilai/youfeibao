package cn.iocoder.yudao.module.icbc.controller.admin.settlement.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 「结束本次收货」：显式生成结算单。系统的时间窗只用来建议把哪几张并在一个结算单里，
 * 不自动合并——现场动作是唯一可信的批次边界（ADR 0018）。
 */
@Schema(description = "管理后台 - 生成结算单 Request VO")
@Data
public class SettlementGenerateReqVO {

    @Schema(description = "出售者（收方）档案编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "出售者不能为空")
    private Long payeeId;

    @Schema(description = "场站编号；一次到场批次按「出售者 + 场站」聚合，不传则不分场站（兼容旧客户端）", example = "3072")
    private Long stationId;

    @Schema(description = "离线批次键；现场端同一批用同一个值，补传后据此归入同一结算单", example = "BATCH_20261201_01")
    private String batchKey;

    @Schema(description = "备注", example = "一车混合废料")
    private String remark;

}
