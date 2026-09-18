package cn.iocoder.yudao.module.icbc.dal.dataobject.entauth;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * 工行企业授权记录 DO
 */
@TableName("icbc_enterprise_auth")
@KeySequence("icbc_enterprise_auth_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class IcbcEnterpriseAuthDO extends TenantBaseDO {

    @TableId
    private Long id;

    /** 付方编号（回收企业 / 子商户） */
    private String outVendorId;
    /** 工行入参 siteType */
    private String siteType;
    /** 工行入参 userType */
    private String userType;
    /** 授权状态：0-未授权，1-已授权，2-已失效 */
    private Integer authStatus;
    /** 授权时间 */
    private LocalDateTime authTime;
    /** 授权有效期止 */
    private LocalDateTime expireTime;
    /** 备注 */
    private String remark;

}
