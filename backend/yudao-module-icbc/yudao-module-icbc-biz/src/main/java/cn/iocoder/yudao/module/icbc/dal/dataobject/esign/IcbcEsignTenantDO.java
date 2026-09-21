package cn.iocoder.yudao.module.icbc.dal.dataobject.esign;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * 租户级电子签章配置 DO（#92，ADR 0036）。
 *
 * <p>章是**租户级**的：回收企业才是发起方（第三方接口约束，也与 ADR 0001 一致），平台不代盖。
 * 每个租户一行，`sub_customer_no`（子客编号）由**我们生成、持久化、不可变、不可重复**——
 * 它是第三方回执里唯一的租户锚点，回调据此反查到具体是哪家回收企业。
 *
 * <p>它是租户表（继承 {@link TenantBaseDO}）：一个回收企业只能看见 / 维护自己这一行；
 * 平台运营要看全平台时用 {@code TenantUtils.executeIgnore} 显式跨租户读取。
 */
@TableName("icbc_esign_tenant")
@KeySequence("icbc_esign_tenant_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IcbcEsignTenantDO extends TenantBaseDO {

    @TableId
    private Long id;

    /** 子客编号：我们生成、持久化、不可变、不可重复（回调靠它反查租户） */
    private String subCustomerNo;

    /** 开通状态，见 {@code EsignActivationStatusEnum}：0-未开通，1-认证中，2-已激活 */
    private Integer activationStatus;

    /** 经办人编号（第三方侧的企业办事人） */
    private String operatorNo;

    /** 企业印章编号：为空表示印章未就位 */
    private String sealNo;

    /** 合同额度（份数）：本次开通内可发起的合同组数量上限 */
    private Integer contractQuota;

    /** 已用合同额度（份数） */
    private Integer contractUsed;

    /** 最近一次签发的控制台链接令牌（一次性：再次开通会换新，激活后清空） */
    private String consoleToken;

    /** 控制台链接有效期止 */
    private LocalDateTime consoleTokenExpireTime;

    /** 激活时间（企业认证通过且印章就位的那一刻） */
    private LocalDateTime activatedTime;

    /** 备注 */
    private String remark;

}
