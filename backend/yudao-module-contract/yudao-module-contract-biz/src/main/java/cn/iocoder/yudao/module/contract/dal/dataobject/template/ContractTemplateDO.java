package cn.iocoder.yudao.module.contract.dal.dataobject.template;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * 合同模板 DO
 *
 * @author 芋道源码
 */
@TableName("contract_templates")
@KeySequence("contract_template_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContractTemplateDO extends BaseDO {

    /**
     * 模板ID
     */
    @TableId
    private Long id;

    /**
     * 模板名称
     */
    private String templateName;

    /**
     * 模板编码
     */
    private String templateCode;

    /**
     * 合同类型ID
     */
    private Long contractTypeId;

    /**
     * 模板内容
     */
    private String templateContent;

    /**
     * 模板文件URL
     */
    private String templateFileUrl;

    /**
     * 模板版本
     */
    private String version;

    /**
     * 是否启用
     */
    private Boolean isActive;

    /**
     * 是否默认模板
     */
    private Boolean isDefault;

    /**
     * 备注
     */
    private String remark;

    /**
     * 租户ID
     */
    private Long tenantId;
} 