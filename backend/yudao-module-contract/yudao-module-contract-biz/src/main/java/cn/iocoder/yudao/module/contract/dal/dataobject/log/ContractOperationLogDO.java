package cn.iocoder.yudao.module.contract.dal.dataobject.log;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * 合同操作日志 DO
 *
 * @author 芋道源码
 */
@TableName("contract_operation_logs")
@KeySequence("contract_operation_log_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContractOperationLogDO extends TenantBaseDO {

    /**
     * 日志ID
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
     * 操作类型(1:创建,2:修改,3:提交审核,4:审核,5:签署,6:激活,7:终止,8:归档)
     */
    private Integer operationType;

    /**
     * 操作描述
     */
    private String operationDescription;

    /**
     * 操作前状态
     */
    private Integer oldStatus;

    /**
     * 操作后状态
     */
    private Integer newStatus;

    /**
     * 操作数据(JSON)
     */
    private String operationData;

    /**
     * 操作人ID
     */
    private Long operatorId;

    /**
     * 操作人姓名
     */
    private String operatorName;

    /**
     * 操作IP
     */
    private String operatorIp;

    // 注意：tenantId, createTime, updateTime, creator, updater, deleted 字段已在 TenantBaseDO 中定义，无需重复定义
} 