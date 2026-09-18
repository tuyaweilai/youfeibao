package cn.iocoder.yudao.module.enterprise.dal.dataobject;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 用户企业关系 DO
 *
 * @author 芋道源码
 */
@TableName("enterprise_user_relation")
@KeySequence("enterprise_user_relation_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class EnterpriseUserRelationDO extends BaseDO {

    /**
     * 主键ID
     */
    @TableId
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 企业ID
     */
    private Long enterpriseId;

    /**
     * 门店ID
     */
    private Long storeId;

    /**
     * 关系类型
     * 
     * 枚举 {@link cn.iocoder.yudao.module.enterprise.enums.EnterpriseUserRelationTypeEnum}
     */
    private Integer relationType;

    /**
     * 是否企业主联系人
     */
    private Boolean isPrimaryContact;

    /**
     * 是否用户默认操作企业
     */
    private Boolean isDefaultEnterprise;
} 