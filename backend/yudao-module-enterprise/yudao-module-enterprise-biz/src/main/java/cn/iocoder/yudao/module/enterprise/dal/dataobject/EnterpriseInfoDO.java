package cn.iocoder.yudao.module.enterprise.dal.dataobject;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDate;
import java.math.BigDecimal;

/**
 * 企业信息 DO
 *
 * @author 芋道源码
 */
@TableName("enterprise_info")
@KeySequence("enterprise_info_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class EnterpriseInfoDO extends BaseDO {

    /**
     * 主键ID
     */
    @TableId
    private Long id;

    /**
     * 企业名称
     */
    private String name;

    /**
     * 统一社会信用代码
     */
    private String creditCode;

    /**
     * 企业类型
     *
     * 枚举 {@link cn.iocoder.yudao.module.enterprise.enums.EnterpriseTypeEnum}
     */
    private Integer enterpriseType;

    /**
     * 法定代表人姓名
     */
    private String legalPersonName;

    /**
     * 法定代表人身份证号 (脱敏存储或仅用于认证过程)
     */
    private String legalPersonIdCardNo;

    /**
     * 注册资本 (万元)
     */
    private BigDecimal registeredCapital;

    /**
     * 成立日期
     */
    private LocalDate establishmentDate;

    /**
     * 经营范围
     */
    private String businessScope;

    /**
     * 注册地址-省编码
     */
    private String registeredAddressProvinceCode;

    /**
     * 注册地址-市编码
     */
    private String registeredAddressCityCode;

    /**
     * 注册地址-区编码
     */
    private String registeredAddressDistrictCode;

    /**
     * 注册地址-详细地址
     */
    private String registeredAddressDetail;

    /**
     * 企业联系人姓名
     */
    private String contactName;

    /**
     * 企业联系人电话
     */
    private String contactPhone;

    /**
     * 营业执照附件
     */
    private String businessLicenseFile;

    /**
     * 企业状态
     *
     * 枚举 {@link cn.iocoder.yudao.module.enterprise.enums.EnterpriseStatusEnum}
     */
    private Integer status;

    /**
     * 最新审核备注 (冗余字段，主要记录在audit_log和auth_record)
     */
    private String auditRemarks;

    /**
     * 获取统一社会信用代码
     */
    public String getUnifiedSocialCreditCode() {
        return creditCode;
    }

    /**
     * 获取法定代表人姓名
     */
    public String getLegalRepresentativeName() {
        return legalPersonName;
    }

    /**
     * 获取法定代表人身份证号
     */
    public String getLegalRepresentativeIdCard() {
        return legalPersonIdCardNo;
    }

} 