package cn.iocoder.yudao.module.contract.dal.dataobject.version;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDate;

/**
 * 合同版本 DO
 *
 * @author 芋道源码
 */
@TableName("contract_versions")
@KeySequence("contract_version_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContractVersionDO extends BaseDO {

    /**
     * 版本ID
     */
    @TableId
    private Long id;

    /**
     * 合同ID
     */
    private Long contractId;

    /**
     * 版本号
     */
    private String versionNumber;

    /**
     * 生效日期
     */
    private LocalDate effectiveDate;

    /**
     * 失效日期
     */
    private LocalDate expiryDate;

    /**
     * 签署完成日期
     */
    private LocalDate signingDate;

    /**
     * 终止日期
     */
    private LocalDate terminationDate;

    /**
     * 终止原因
     */
    private String reasonForTermination;

    /**
     * 版本变更说明
     */
    private String descriptionOfChanges;

    /**
     * 合同内容
     */
    private String contractContent;

    /**
     * 合同文件URL
     */
    private String contractFileUrl;

    /**
     * 电子签章服务商
     */
    private String esignatureProvider;

    /**
     * 第三方签署流程ID
     */
    private String esignatureProcessId;

    /**
     * 电子签章状态(0:未发起,1:进行中,2:已完成,3:已失败)
     */
    private Integer esignatureStatus;

    /**
     * 自动提醒天数(逗号分隔)
     */
    private String autoRemindDays;

    /**
     * 租户ID
     */
    private Long tenantId;
} 