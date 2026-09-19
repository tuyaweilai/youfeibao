package cn.iocoder.yudao.module.icbc.dal.dataobject.payee;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import cn.iocoder.yudao.module.icbc.enums.IcbcStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.PayeeOnboardingOutcomeEnum;
import cn.iocoder.yudao.module.icbc.enums.PayeeRealNameStatusEnum;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 工行收方信息 DO
 *
 * @author 芋道源码
 */
@TableName("icbc_payee_info")
@KeySequence("icbc_payee_info_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayeeInfoDO extends TenantBaseDO {

    /**
     * 主键
     */
    @TableId
    private Long id;
    
    /**
     * 收方编号（工行返回）
     */
    private String payeeNo;
    
    /**
     * 合作方收方编号（我方生成）
     */
    private String partnerPayeeId;

    /**
     * 自然人主体编号（平台级身份档案，见 ADR 0017）。
     *
     * <p>收方档案是「回收企业 × 自然人」的档案，而身份是跨企业的；同一个自然人在不同租户会有
     * 多条收方档案，但都指向同一个自然人主体。实人认证结果记在主体上，收方入驻状态记在本档案上。
     */
    private Long naturalPersonId;
    
    /**
     * 收方姓名
     */
    private String name;
    
    /**
     * 身份证号码
     */
    private String idCardNo;
    
    /**
     * 手机号码
     */
    private String mobile;
    
    /**
     * 银行卡号
     */
    private String bankCardNo;
    
    /**
     * 开户银行
     */
    private String bankName;
    
    /**
     * 开户支行
     */
    private String bankBranch;
    
    /**
     * 地址
     */
    private String address;
    
    /**
     * 状态
     * 
     * 枚举 {@link IcbcStatusEnum.AuditStatus}
     */
    private Integer status;
    
    /**
     * 审核信息
     */
    private String auditMsg;
    
    /**
     * 业务类型：RECYCLE-再生资源等
     */
    private String businessType;
    
    /**
     * 工行收方状态：0-不可用，1-可用
     */
    private String icbcReceiverStatus;
    
    /**
     * 工行返回的收方账户标识（mediumId，原样透传）
     */
    private String icbcMediumId;
    
    /**
     * 工行侧开户状态（openacctStatus，原样透传）：00-初始，01-开户中，02-开户成功，03-开户失败
     */
    private String icbcOpenacctStatus;
    
    /**
     * 职业
     */
    private String occupation;
    
    /**
     * 关联企业名称
     */
    private String companyName;

    // ==================== 出售者建档（#6） ====================

    /**
     * 实人认证状态：0-未认证，1-认证中，2-认证通过，3-认证未通过
     *
     * 枚举 {@link PayeeRealNameStatusEnum}
     *
     * @deprecated 实人认证是**自然人主体**的事（跨企业复用），已迁到
     * {@link cn.iocoder.yudao.module.icbc.dal.dataobject.naturalperson.IcbcNaturalPersonDO}
     * （ADR 0017）。本字段只保留历史数据，不再写入。
     */
    private Integer realNameStatus;

    /**
     * 实人认证失败原因
     */
    private String realNameMsg;

    /**
     * 实人认证通过时间
     */
    private LocalDateTime realNameTime;

    /**
     * 收方审核结果（result，原样透传）：pass / reject
     */
    private String auditResult;

    /**
     * 审核拒绝原因（可前置展示给收货员）
     */
    private String rejectReason;

    /**
     * 收方入驻结果：四种组合之一，见 {@link PayeeOnboardingOutcomeEnum}
     */
    private String onboardingState;

    /**
     * 证件签发日期 yyyy-MM-dd（收方入驻页面入参，透传）
     */
    private String idSignDate;

    /**
     * 证件截止日期 yyyy-MM-dd，永久有效传 9999-12-30（收方入驻页面入参，透传）
     */
    private String idValidityPeriod;

} 