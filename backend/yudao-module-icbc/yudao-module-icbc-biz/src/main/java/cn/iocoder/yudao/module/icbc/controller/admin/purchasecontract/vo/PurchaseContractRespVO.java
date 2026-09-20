package cn.iocoder.yudao.module.icbc.controller.admin.purchasecontract.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理后台 - 采购合同 Response VO（#45 / T07）。
 *
 * <p>{@link #expired} 与 {@link #statusName} 都是推导值：已生效且有效期止早于今天即显示「过期」，
 * 不落库（与 #13 的「逾期」同一做法）。
 */
@Schema(description = "管理后台 - 采购合同 Response VO")
@Data
public class PurchaseContractRespVO {

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "合同编号")
    private String contractNo;

    @Schema(description = "合同名称")
    private String name;

    @Schema(description = "交易对方主体类型")
    private Integer counterpartyType;

    @Schema(description = "交易对方主体类型名")
    private String counterpartyTypeName;

    @Schema(description = "自然人出售者档案编号")
    private Long payeeId;

    @Schema(description = "单位供货方编号")
    private Long supplierId;

    @Schema(description = "交易对方名称快照")
    private String counterpartyName;

    @Schema(description = "有效期起")
    private LocalDate startDate;

    @Schema(description = "有效期止")
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

    @Schema(description = "状态：0-草稿，1-待审核，2-生效，3-关闭")
    private Integer status;

    @Schema(description = "状态名（已生效且过期时显示「过期」）")
    private String statusName;

    @Schema(description = "是否已过期（已生效 + 有效期止早于今天）")
    private Boolean expired;

    @Schema(description = "是否可作为采购依据（已生效且未过期）")
    private Boolean usableAsPurchaseBasis;

    @Schema(description = "已送审的最新版本号")
    private Integer versionNo;

    @Schema(description = "最近一次送审人")
    private Long submittedBy;

    @Schema(description = "最近一次送审时间")
    private LocalDateTime submittedTime;

    @Schema(description = "审核人")
    private Long auditedBy;

    @Schema(description = "审核时间")
    private LocalDateTime auditedTime;

    @Schema(description = "审核意见")
    private String auditRemark;

    @Schema(description = "关闭人")
    private Long closedBy;

    @Schema(description = "关闭时间")
    private LocalDateTime closedTime;

    @Schema(description = "关闭原因")
    private String closeReason;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "适用品类")
    private List<PurchaseContractCategoryRespVO> categories;

    @Schema(description = "历史版本（只追加，可回查）")
    private List<PurchaseContractVersionRespVO> versions;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

}
