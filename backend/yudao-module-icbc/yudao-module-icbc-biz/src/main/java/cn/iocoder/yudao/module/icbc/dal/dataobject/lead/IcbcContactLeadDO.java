package cn.iocoder.yudao.module.icbc.dal.dataobject.lead;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * 收方入驻失败后留下的联系方式。落库到解析出的租户下，供后续人工跟进。
 */
@TableName("icbc_contact_lead")
@KeySequence("icbc_contact_lead_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IcbcContactLeadDO extends TenantBaseDO {

    @TableId
    private Long id;

    /** 关联收方 ID */
    private Long payeeId;

    /** 出售者姓名 */
    private String name;

    /** 联系方式（手机号） */
    private String mobile;

    /** 备注 / 失败原因 */
    private String remark;

}
