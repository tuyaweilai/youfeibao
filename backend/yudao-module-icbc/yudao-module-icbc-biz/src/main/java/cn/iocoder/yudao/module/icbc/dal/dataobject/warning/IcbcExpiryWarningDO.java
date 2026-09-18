package cn.iocoder.yudao.module.icbc.dal.dataobject.warning;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 资质到期预警 DO。
 *
 * <p>由定时任务扫描即将到期的资质后落库，是「主动预警」的持久化载体：
 * 租户在开票就绪页能直接看到待处理的预警，不必自己去翻资质列表。
 */
@TableName("icbc_expiry_warning")
@KeySequence("icbc_expiry_warning_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class IcbcExpiryWarningDO extends TenantBaseDO {

    @TableId
    private Long id;

    /** 资质编号 */
    private Long qualificationId;
    /** 资质层 */
    private String type;
    /** 资质名称 */
    private String name;
    /** 有效期止 */
    private LocalDate validTo;
    /** 状态：0-待处理，1-已处理 */
    private Integer status;
    /** 预警时间 */
    private LocalDateTime warnedAt;
    /** 备注 */
    private String remark;

}
