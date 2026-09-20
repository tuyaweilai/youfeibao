package cn.iocoder.yudao.module.icbc.dal.dataobject.purchasecontract;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * 采购合同的适用品类 DO（#45 / T07）。
 *
 * <p>一个合同可约定多个适用品类。品类名称与计量单位是**签约时的快照**：品类改名或停用后，
 * 历史合同仍能还原当时约定了什么（与收购单 / 结算单同一做法）。
 */
@TableName("icbc_purchase_contract_category")
@KeySequence("icbc_purchase_contract_category_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IcbcPurchaseContractCategoryDO extends TenantBaseDO {

    /** 主键 */
    @TableId
    private Long id;

    /** 采购合同编号 */
    private Long contractId;

    /** 品类编号（icbc_goods_config） */
    private Long goodsConfigId;

    /** 品类名称快照 */
    private String categoryName;

    /** 计量单位快照 */
    private String unit;

}
