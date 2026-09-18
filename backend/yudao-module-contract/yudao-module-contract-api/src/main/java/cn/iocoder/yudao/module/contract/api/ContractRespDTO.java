package cn.iocoder.yudao.module.contract.api;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 合同响应 DTO
 *
 * @author 芋道源码
 */
@Data
public class ContractRespDTO {

    /**
     * 合同ID
     */
    private Long id;

    /**
     * 合同编号
     */
    private String contractNo;

    /**
     * 合同UUID
     */
    private String contractUuid;

    /**
     * 合同名称
     */
    private String name;

    /**
     * 合同类型ID
     */
    private Long typeId;

    /**
     * 合同模板ID
     */
    private Long templateId;

    /**
     * 当前版本ID
     */
    private Long currentVersionId;

    /**
     * 合同状态
     */
    private Integer status;

    /**
     * 是否为电子合同
     */
    private Boolean isElectronic;

    /**
     * 合同主要负责企业ID
     */
    private Long primaryOwnerEnterpriseId;

    /**
     * 合同总金额
     */
    private BigDecimal totalAmount;

    /**
     * 币种
     */
    private String currency;

    /**
     * 优先级
     */
    private Integer priorityLevel;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
} 