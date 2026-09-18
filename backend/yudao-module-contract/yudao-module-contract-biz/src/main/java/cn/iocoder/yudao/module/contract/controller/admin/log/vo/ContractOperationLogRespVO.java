package cn.iocoder.yudao.module.contract.controller.admin.log.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 合同操作日志 Response VO")
@Data
public class ContractOperationLogRespVO {

    @Schema(description = "日志ID", example = "1")
    private Long id;

    @Schema(description = "合同ID", example = "1")
    private Long contractId;

    @Schema(description = "合同版本ID", example = "1")
    private Long versionId;

    @Schema(description = "操作类型", example = "1")
    private Integer operationType;

    @Schema(description = "操作类型描述", example = "创建合同")
    private String operationTypeDesc;

    @Schema(description = "操作描述", example = "创建了合同")
    private String operationDescription;

    @Schema(description = "操作前状态", example = "0")
    private Integer oldStatus;

    @Schema(description = "操作前状态描述", example = "草稿")
    private String oldStatusDesc;

    @Schema(description = "操作后状态", example = "1")
    private Integer newStatus;

    @Schema(description = "操作后状态描述", example = "待审核")
    private String newStatusDesc;

    @Schema(description = "操作数据", example = "{}")
    private String operationData;

    @Schema(description = "操作人ID", example = "1")
    private Long operatorId;

    @Schema(description = "操作人姓名", example = "张三")
    private String operatorName;

    @Schema(description = "操作IP", example = "127.0.0.1")
    private String operatorIp;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

} 