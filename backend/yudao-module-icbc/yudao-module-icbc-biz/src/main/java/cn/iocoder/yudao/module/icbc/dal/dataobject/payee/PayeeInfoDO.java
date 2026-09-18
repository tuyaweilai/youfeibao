package cn.iocoder.yudao.module.icbc.dal.dataobject.payee;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import cn.iocoder.yudao.module.icbc.enums.IcbcStatusEnum;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

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

} 