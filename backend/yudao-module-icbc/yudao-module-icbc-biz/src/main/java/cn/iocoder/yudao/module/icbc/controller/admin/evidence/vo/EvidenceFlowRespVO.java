package cn.iocoder.yudao.module.icbc.controller.admin.evidence.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - 五流中某一流 Response VO")
@Data
public class EvidenceFlowRespVO {

    @Schema(description = "流标识", requiredMode = Schema.RequiredMode.REQUIRED, example = "CONTRACT")
    private String flow;

    @Schema(description = "流名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "合同流")
    private String flowName;

    @Schema(description = "该流是否齐备", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
    private Boolean present;

    @Schema(description = "该流的证据来源")
    private List<EvidenceSourceRespVO> sources;

}
