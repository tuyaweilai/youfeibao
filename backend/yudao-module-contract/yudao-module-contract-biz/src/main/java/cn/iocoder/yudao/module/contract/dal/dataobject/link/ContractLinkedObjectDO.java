package cn.iocoder.yudao.module.contract.dal.dataobject.link;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * 合同关联对象 DO
 *
 * @author 芋道源码
 */
@TableName("contract_linked_objects")
@KeySequence("contract_linked_object_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContractLinkedObjectDO extends BaseDO {

    /**
     * 关联ID
     */
    @TableId
    private Long id;

    /**
     * 合同ID
     */
    private Long contractId;

    /**
     * 合同版本ID
     */
    private Long versionId;

    /**
     * 关联对象ID
     */
    private Long objectId;

    /**
     * 关联对象类型
     */
    private String objectType;

    /**
     * 关联对象编号
     */
    private String objectNo;

    /**
     * 关联类型(0:业务关联,1:依赖关联,2:参考关联)
     */
    private Integer linkType;

    /**
     * 关联状态(0:有效,1:失效,2:暂停)
     */
    private Integer linkStatus;

    /**
     * 关联说明
     */
    private String linkDescription;

    /**
     * 租户ID
     */
    private Long tenantId;
} 