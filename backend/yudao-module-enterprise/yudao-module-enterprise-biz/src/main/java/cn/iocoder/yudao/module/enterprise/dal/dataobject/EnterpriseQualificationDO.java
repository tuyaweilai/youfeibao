package cn.iocoder.yudao.module.enterprise.dal.dataobject;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDate;

/**
 * 企业资质 DO
 *
 * @author 芋道源码
 */
@TableName("enterprise_qualification")
@KeySequence("enterprise_qualification_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class EnterpriseQualificationDO extends BaseDO {

    /**
     * 主键ID
     */
    @TableId
    private Long id;

    /**
     * 企业ID
     */
    private Long enterpriseId;

    /**
     * 资质类型
     */
    private Integer qualificationType;

    /**
     * 资质名称
     */
    private String qualificationName;

    /**
     * 资质编号
     */
    private String qualificationCode;

    /**
     * 发证日期
     */
    private LocalDate issueDate;

    /**
     * 到期日期
     */
    private LocalDate expiryDate;

    /**
     * 发证机关
     */
    private String issuingAuthority;

    /**
     * 资质文件附件ID
     */
    private Long qualificationFileId;

    /**
     * 资质状态
     *
     * 枚举 {@link cn.iocoder.yudao.module.enterprise.enums.EnterpriseQualificationStatusEnum}
     */
    private Integer status;

    /**
     * 审核备注
     */
    private String auditRemarks;

} 