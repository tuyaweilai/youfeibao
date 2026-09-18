package cn.iocoder.yudao.module.icbc.dal.dataobject.qualification;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDate;

/**
 * 租户三层资质 DO
 */
@TableName("icbc_qualification")
@KeySequence("icbc_qualification_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class IcbcQualificationDO extends TenantBaseDO {

    @TableId
    private Long id;

    /** 资质层：TAX / INDUSTRY / PUBLIC_SECURITY */
    private String type;
    /** 资质名称 */
    private String name;
    /** 发证机关 */
    private String issuingAuthority;
    /** 证书编号 */
    private String certNo;
    /** 有效期起 */
    private LocalDate validFrom;
    /** 有效期止 */
    private LocalDate validTo;
    /** 证照扫描件 */
    private String fileUrl;
    /** 状态：0-待核实，1-有效，2-失效，3-吊销 */
    private Integer status;
    /** 核实意见 */
    private String auditRemark;

}
