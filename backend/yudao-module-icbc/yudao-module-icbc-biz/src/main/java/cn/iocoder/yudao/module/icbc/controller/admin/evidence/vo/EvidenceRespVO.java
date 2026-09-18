package cn.iocoder.yudao.module.icbc.controller.admin.evidence.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 一票一档证据 Response VO")
@Data
public class EvidenceRespVO {

    @Schema(description = "证据ID", example = "1024")
    private Long id;

    @Schema(description = "所属流", example = "GOODS")
    private String flow;

    @Schema(description = "所属流名称", example = "货物流")
    private String flowName;

    @Schema(description = "证据类型", example = "WEIGHBRIDGE_TICKET")
    private String evidenceType;

    @Schema(description = "证据类型名称", example = "过磅单")
    private String evidenceTypeName;

    @Schema(description = "证据标题", example = "2024-12-01 过磅单")
    private String title;

    @Schema(description = "证据文件地址")
    private String fileUrl;

    @Schema(description = "证据文件名称")
    private String fileName;

    @Schema(description = "证据对应业务发生时间")
    private LocalDateTime occurredTime;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
