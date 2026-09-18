package cn.iocoder.yudao.module.contract.controller.admin.party.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 合同参与方 Response VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ContractPartyRespVO extends ContractPartyBaseVO {

    @Schema(description = "参与方ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "合同ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long contractId;

    @Schema(description = "参与企业ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    private Long enterpriseId;

    @Schema(description = "企业名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "测试企业")
    private String enterpriseName;

    @Schema(description = "合同中的角色", requiredMode = Schema.RequiredMode.REQUIRED, example = "甲方")
    private String roleInContract;

    @Schema(description = "签署人姓名", example = "张三")
    private String signatoryName;

    @Schema(description = "签署人邮箱", example = "zhangsan@example.com")
    private String signatoryEmail;

    @Schema(description = "签署人电话", example = "13800138000")
    private String signatoryPhone;

    @Schema(description = "签署人用户ID", example = "1")
    private Long signatoryUserId;

    @Schema(description = "签署状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    private Integer signStatus;

    @Schema(description = "签署时间", example = "2025-05-30 12:30:40")
    private LocalDateTime signedAt;

    @Schema(description = "签署IP地址", example = "192.168.1.1")
    private String signIp;

    @Schema(description = "电子签章个体ID", example = "esign-10001")
    private String esignatureIndividualId;

    @Schema(description = "签署顺序", example = "1")
    private Integer orderInSignFlow;

    @Schema(description = "是否必须签署", example = "true")
    private Boolean isRequired;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;
} 