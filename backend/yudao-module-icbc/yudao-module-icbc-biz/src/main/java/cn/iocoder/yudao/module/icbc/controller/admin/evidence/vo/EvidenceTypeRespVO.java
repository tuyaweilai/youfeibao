package cn.iocoder.yudao.module.icbc.controller.admin.evidence.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 证据类型 Response VO")
@Data
public class EvidenceTypeRespVO {

    @Schema(description = "证据类型", example = "WEIGHBRIDGE_TICKET")
    private String code;

    @Schema(description = "证据类型名称", example = "过磅单")
    private String name;

    @Schema(description = "所属流", example = "GOODS")
    private String flow;

    @Schema(description = "所属流名称", example = "货物流")
    private String flowName;

}
