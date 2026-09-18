package cn.iocoder.yudao.module.contract.controller.admin.contract.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 合同 Response VO")
@Data
public class ContractRespVO {

    @Schema(description = "合同ID", example = "1")
    private Long id;

    @Schema(description = "合同编号", example = "HT202401001")
    private String contractNo;

    @Schema(description = "合同UUID", example = "a1b2c3d4-e5f6-g7h8-i9j0")
    private String contractUuid;

    @Schema(description = "合同名称", example = "危废处置服务合同")
    private String name;

    @Schema(description = "合同类型ID", example = "1")
    private Long typeId;

    @Schema(description = "合同类型名称", example = "危废处置合同")
    private String typeName;

    @Schema(description = "合同模板ID", example = "1")
    private Long templateId;

    @Schema(description = "当前版本ID", example = "1")
    private Long currentVersionId;

    @Schema(description = "合同状态", example = "1")
    private Integer status;

    @Schema(description = "是否为电子合同", example = "true")
    private Boolean isElectronic;

    @Schema(description = "合同主要负责企业ID", example = "100")
    private Long primaryOwnerEnterpriseId;

    @Schema(description = "合同总金额", example = "100000.00")
    private BigDecimal totalAmount;

    @Schema(description = "币种", example = "CNY")
    private String currency;

    @Schema(description = "优先级", example = "1")
    private Integer priorityLevel;

    @Schema(description = "备注", example = "这是一份重要合同")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
} 