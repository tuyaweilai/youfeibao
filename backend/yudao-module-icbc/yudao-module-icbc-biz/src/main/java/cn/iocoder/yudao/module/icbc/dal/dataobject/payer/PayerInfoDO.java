package cn.iocoder.yudao.module.icbc.dal.dataobject.payer;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 工行付方信息 DO
 *
 * @author 芋道源码
 */
@TableName("icbc_payer_info")
@KeySequence("icbc_payer_info_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class PayerInfoDO extends TenantBaseDO {

    /**
     * 主键
     */
    @TableId
    private Long id;

    /**
     * 工行付方编号
     */
    private String payerNo;

    /**
     * 合作方付方编号
     */
    private String partnerPayerId;

    /**
     * 企业名称
     */
    private String name;

    /**
     * 统一社会信用代码
     */
    private String creditCode;

    /**
     * 纳税人识别号
     */
    private String taxNo;

    /**
     * 银行账户
     */
    private String bankAccount;

    /**
     * 开户行名称
     */
    private String bankName;

    /**
     * 企业地址
     */
    private String address;

    /**
     * 企业电话
     */
    private String telephone;

    /**
     * 联系人姓名
     */
    private String contactName;

    /**
     * 联系人手机号
     */
    private String contactMobile;

    /**
     * 纳税人类型：01-一般纳税人，02-小规模纳税人
     */
    private String taxpayerType;

    /**
     * 开票人姓名（须与工行税务登记的开票员为同一实名主体）。
     *
     * <p>与 {@link #drawerCardNumber}、{@link #areaCode} 一并构成本企业的**开票参数**：
     * 这三项是工行预下单的必输项。结算确认后自动预下单发生在自然人的手机上（#106 / ADR 0039），
     * 那一刻没有开票员在场，所以必须提前落成租户配置；三项不全时不自动发起，退回人工路径。
     */
    private String drawerName;

    /**
     * 开票人证件号码
     */
    private String drawerCardNumber;

    /**
     * 应税行为发生地（省级税务机关代码，如 110000）
     */
    private String areaCode;

    /**
     * 业务类型
     */
    private String businessType;

    /**
     * 状态：0-待审核，1-审核通过，2-审核拒绝
     */
    private Integer status;

    /**
     * 审核消息
     */
    private String auditMsg;

    /**
     * 工行付方状态：0-不可用，1-可用
     */
    private String icbcPayerStatus;

} 