package cn.iocoder.yudao.module.icbc.controller.admin.purchasecontract.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

/**
 * 管理后台 - 采购合同新增 / 修改 Request VO（#45 / T07）。
 */
@Schema(description = "管理后台 - 采购合同新增/修改 Request VO")
@Data
public class PurchaseContractSaveReqVO {

    @Schema(description = "主键（修改时必填）", example = "1")
    private Long id;

    @Schema(description = "合同名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "合同名称不能为空")
    private String name;

    @Schema(description = "交易对方主体类型（六态：1-自然人出售者，2-个体工商户，3-个人独资企业，"
            + "4-合伙企业，5-企业法人，6-农民专业合作社）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "交易对方主体类型不能为空")
    private Integer counterpartyType;

    @Schema(description = "自然人出售者档案编号（主体类型为自然人时必填）")
    private Long payeeId;

    @Schema(description = "单位供货方编号（主体类型为非自然人时必填，指向 erp_supplier）")
    private Long supplierId;

    @Schema(description = "单位供货方名称（非自然人合同的对手方名称快照，必填）")
    private String counterpartyName;

    @Schema(description = "有效期起", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "有效期起不能为空")
    private LocalDate startDate;

    @Schema(description = "有效期止", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "有效期止不能为空")
    private LocalDate endDate;

    @Schema(description = "数量约定")
    private String quantityAgreement;

    @Schema(description = "计量标准")
    private String measureStandard;

    @Schema(description = "质量标准")
    private String qualityStandard;

    @Schema(description = "价格规则")
    private String priceRule;

    @Schema(description = "运输责任")
    private String transportResponsibility;

    @Schema(description = "付款条款")
    private String paymentTerms;

    @Schema(description = "附件地址（多个用英文逗号分隔）")
    private String attachmentUrls;

    @Schema(description = "适用品类编号列表（icbc_goods_config）", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Long> categoryIds;

    @Schema(description = "变更原因（修改已生效合同时必填；未审核通过前不得作为采购依据）")
    private String changeReason;

    @Schema(description = "备注")
    private String remark;

}
