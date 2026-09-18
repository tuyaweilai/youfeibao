package cn.iocoder.yudao.module.contract.dal.dataobject.contract;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 合同 DO
 *
 * @author 芋道源码
 */
@TableName("contracts")
@KeySequence("contract_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContractDO extends BaseDO {

    /**
     * 合同ID
     */
    @TableId
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
    private String contractName;

    /**
     * 合同类型ID
     */
    private Long contractTypeId;

    /**
     * 合同模板ID
     */
    private Long templateId;

    /**
     * 当前版本ID
     */
    private Long currentVersionId;

    /**
     * 合同状态(0:草稿,1:待审核,2:审核通过,3:待签署,4:签署中,5:已生效,6:已履行,7:即将到期,8:已到期,9:已解除,10:已作废,11:已归档)
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
     * 优先级(0:普通,1:重要,2:紧急)
     */
    private Integer priorityLevel;

    /**
     * 备注
     */
    private String remark;

    /**
     * 租户ID
     */
    private Long tenantId;

    /* --- 以下是为解决字段映射添加的兼容方法 --- */

    /**
     * 合同名称别名（兼容）
     */
    public String getName() {
        return this.contractName;
    }

    /**
     * 合同类型ID别名（兼容）
     */
    public Long getTypeId() {
        return this.contractTypeId;
    }
    
    /**
     * 合同类型ID别名（兼容）
     */
    public void setTypeId(Long typeId) {
        this.contractTypeId = typeId;
    }
} 