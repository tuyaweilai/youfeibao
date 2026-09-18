package cn.iocoder.yudao.module.icbc.dal.dataobject.authorization;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 出售者首次授权 DO
 *
 * <p>自然人出售者首次卖货时，须明确同意两件事：由回收企业为其反向开票，以及由回收企业
 * 代办税费（增值税及附加、个人所得税）。这两项是法定义务授权的落地，必须留痕，日后
 * 不能被说成「不知情」（issue #6 验收、用户故事 3）。
 */
@TableName("icbc_seller_authorization")
@KeySequence("icbc_seller_authorization_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IcbcSellerAuthorizationDO extends TenantBaseDO {

    @TableId
    private Long id;

    /**
     * 出售者（收方）ID
     */
    private Long payeeId;

    /**
     * 是否授权反向开票
     */
    private Boolean reverseInvoiceAuthorized;

    /**
     * 是否授权代办税费
     */
    private Boolean taxAgencyAuthorized;

    /**
     * 授权时间
     */
    private LocalDateTime authorizedAt;

    /**
     * 授权渠道：ONSITE-收购现场，ICBC_H5-工行页面
     */
    private String channel;

    /**
     * 现场办理人（收货员）
     */
    private String operator;

    /**
     * 留痕附件地址（授权书扫描件 / 电子签名截图）
     */
    private String evidenceUrl;

    /**
     * 备注
     */
    private String remark;

}
