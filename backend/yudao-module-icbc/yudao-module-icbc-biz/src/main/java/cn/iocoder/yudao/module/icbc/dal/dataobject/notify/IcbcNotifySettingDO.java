package cn.iocoder.yudao.module.icbc.dal.dataobject.notify;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * 租户级触达设置 DO（#36）。
 *
 * <p>短信开关「租户或平台可开」：平台级开关是配置 {@code icbc.notify.sms-enabled}（默认关闭），
 * 租户可以在这里为自己的租户单独打开。**两者取或**：任一打开即生效。费用与到达率是运营成本，
 * 所以默认一律关闭，谁开谁清楚。
 */
@TableName("icbc_notify_setting")
@KeySequence("icbc_notify_setting_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IcbcNotifySettingDO extends TenantBaseDO {

    /** 主键 */
    @TableId
    private Long id;

    /** 本租户是否开启短信触达 */
    private Boolean smsEnabled;

    /** 备注（谁开的、为什么开） */
    private String remark;

}
