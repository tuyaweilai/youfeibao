package cn.iocoder.yudao.module.contract.controller.admin.party.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Schema(description = "管理后台 - 合同参与方创建 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ContractPartyCreateReqVO extends ContractPartyBaseVO {

    @Schema(description = "合同ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "合同ID不能为空")
    private Long contractId;

    @Schema(description = "参与企业ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @NotNull(message = "参与企业ID不能为空")
    private Long enterpriseId;

    @Schema(description = "企业名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "测试企业")
    @NotEmpty(message = "企业名称不能为空")
    @Size(max = 255, message = "企业名称长度不能超过 255 个字符")
    private String enterpriseName;

    @Schema(description = "合同中的角色", requiredMode = Schema.RequiredMode.REQUIRED, example = "甲方")
    @NotEmpty(message = "合同中的角色不能为空")
    @Size(max = 100, message = "角色长度不能超过 100 个字符")
    private String roleInContract;

    @Schema(description = "签署人姓名", example = "张三")
    private String signatoryName;

    @Schema(description = "签署人邮箱", example = "zhangsan@example.com")
    private String signatoryEmail;

    @Schema(description = "签署人电话", example = "13800138000")
    private String signatoryPhone;

    @Schema(description = "签署人用户ID", example = "1")
    private Long signatoryUserId;

    @Schema(description = "签署顺序", example = "1")
    private Integer orderInSignFlow;

    @Schema(description = "是否必须签署", example = "true")
    private Boolean isRequired;
} 