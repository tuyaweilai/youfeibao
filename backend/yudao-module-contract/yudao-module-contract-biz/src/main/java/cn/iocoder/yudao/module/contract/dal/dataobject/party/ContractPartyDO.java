package cn.iocoder.yudao.module.contract.dal.dataobject.party;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 合同参与方 DO
 *
 * @author 芋道源码
 */
@TableName("contract_parties")
@KeySequence("contract_party_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContractPartyDO extends BaseDO {

    /**
     * 参与方ID
     */
    @TableId
    private Long id;

    /**
     * 合同ID
     */
    private Long contractId;

    /**
     * 参与企业ID
     */
    private Long enterpriseId;

    /**
     * 企业名称
     */
    private String enterpriseName;

    /**
     * 合同中的角色
     */
    private String roleInContract;

    /**
     * 签署人姓名
     */
    private String signatoryName;

    /**
     * 签署人邮箱
     */
    private String signatoryEmail;

    /**
     * 签署人电话
     */
    private String signatoryPhone;

    /**
     * 签署人用户ID
     */
    private Long signatoryUserId;

    /**
     * 签署状态(0:待签署,1:已签署,2:已拒绝,3:已过期)
     */
    private Integer signStatus;

    /**
     * 签署时间
     */
    private LocalDateTime signedAt;

    /**
     * 签署IP地址
     */
    private String signIp;

    /**
     * 电子签章个体ID
     */
    private String esignatureIndividualId;

    /**
     * 签署顺序
     */
    private Integer orderInSignFlow;

    /**
     * 是否必须签署
     */
    private Boolean isRequired;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 获取签署顺序（兼容方法）
     */
    public Integer getSignOrder() {
        return this.orderInSignFlow;
    }
} 