package cn.iocoder.yudao.module.waste.dal.dataobject.payment;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 产废企业付款配置 DO
 *
 * @author 芋道源码
 */
@TableName("waste_producer_payment_config")
@KeySequence("waste_producer_payment_config_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProducerPaymentConfigDO extends BaseDO {

    /**
     * 配置ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 产废企业ID
     */
    private Long producingEnterpriseId;

    /**
     * 产废门店ID (可细化到门店级别)
     */
    private Long producingStoreId;

    /**
     * 结算方式 (1:对公结算, 2:个人结算)
     *
     * 枚举 {@link cn.iocoder.yudao.module.waste.enums.PaymentMethodEnum}
     */
    private Integer paymentMethod;

    /**
     * 是否为企业默认配置
     */
    private Boolean isDefaultConfig;

    // ========== 对公结算配置 ==========
    /**
     * 对公开户银行
     */
    private String companyBankName;

    /**
     * 对公银行账号
     */
    private String companyBankAccount;

    /**
     * 对公账户名称
     */
    private String companyAccountName;

    /**
     * 税号
     */
    private String taxNumber;

    /**
     * 公司地址
     */
    private String companyAddress;

    /**
     * 公司电话
     */
    private String companyPhone;

    /**
     * 是否需要开票
     */
    private Boolean invoiceRequired;

    /**
     * 发票抬头
     */
    private String invoiceTitle;

    // ========== 个人结算配置 ==========
    /**
     * 个人收款人姓名
     */
    private String personalPayeeName;

    /**
     * 个人收款人电话
     */
    private String personalPayeePhone;

    /**
     * 个人收款人身份证号
     */
    private String personalPayeeIdCard;

    /**
     * 个人开户银行
     */
    private String personalBankName;

    /**
     * 个人银行账号
     */
    private String personalBankAccount;

    /**
     * 个人账户名称
     */
    private String personalAccountName;

    /**
     * 与企业关系 (法人/财务/授权人等)
     */
    private String relationshipToEnterprise;

    // ========== 通用配置 ==========
    /**
     * 是否启用自动付款
     */
    private Boolean autoPaymentEnabled;

    /**
     * 付款延迟时间(小时) 0=立即
     */
    private Integer paymentDelayHours;

    /**
     * 最小付款金额阈值
     */
    private BigDecimal minPaymentAmount;

    /**
     * 最大付款金额阈值
     */
    private BigDecimal maxPaymentAmount;

    /**
     * 配置状态 (1:有效, 2:无效, 3:待审核)
     *
     * 枚举 {@link cn.iocoder.yudao.module.waste.enums.ConfigStatusEnum}
     */
    private Integer configStatus;

    /**
     * 审核人
     */
    private String approvedBy;

    /**
     * 审核时间
     */
    private LocalDateTime approvedTime;

    /**
     * 配置备注
     */
    private String remark;

} 