package cn.iocoder.yudao.module.icbc.dal.dataobject.payer;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
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
public class PayerInfoDO extends BaseDO {

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

    /**
     * 工行介质ID
     */
    private String icbcMediumId;

    /**
     * 工行开户状态：00-初始，01-开户中，02-开户成功，03-开户失败
     */
    private String icbcOpenacctStatus;

    /**
     * 租户ID
     */
    private Long tenantId;

} 