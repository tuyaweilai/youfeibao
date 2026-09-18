package cn.iocoder.yudao.module.contract.api;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 合同创建请求 DTO
 *
 * @author 芋道源码
 */
@Data
public class ContractCreateReqDTO {

    /**
     * 合同名称
     */
    @NotBlank(message = "合同名称不能为空")
    private String contractName;

    /**
     * 合同类型ID
     */
    @NotNull(message = "合同类型ID不能为空")
    private Long contractTypeId;

    /**
     * 合同模板ID（可选）
     */
    private Long templateId;

    /**
     * 合同主要负责企业ID
     */
    @NotNull(message = "合同主要负责企业ID不能为空")
    private Long primaryOwnerEnterpriseId;

    /**
     * 合同总金额
     */
    private BigDecimal totalAmount;

    /**
     * 币种
     */
    private String currency = "CNY";

    /**
     * 优先级
     */
    private Integer priorityLevel = 0;

    /**
     * 是否为电子合同
     */
    private Boolean isElectronic = true;

    /**
     * 生效日期
     */
    private LocalDate effectiveDate;

    /**
     * 失效日期
     */
    private LocalDate expiryDate;

    /**
     * 合同内容
     */
    private String contractContent;

    /**
     * 备注
     */
    private String remark;

} 