package cn.iocoder.yudao.module.icbc.dal.dataobject.purchaseorder;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * 采购履约配置 DO（#47 T09）。一个租户一行。
 *
 * <p>两件事：
 * <ol>
 *   <li><b>完成比例采用哪个口径</b>（{@code performance_basis}）：计划 / 验收 / 入库 / 结算 / 未履行
 *       五个口径分列之后，「完成比例」必须标明按哪个口径算（需求原文：「合同明确履约重量口径；订单
 *       完成比例按该口径计算」）。本期落在租户级配置上，因为采购合同（#45）没有承载该字段。</li>
 *   <li><b>三类异常怎么处理</b>（{@code over_quantity_rule} / {@code expired_rule} /
 *       {@code cross_station_rule}）：拦截，还是提交授权审核。见 {@code PurchaseDeliveryRuleEnum}。</li>
 * </ol>
 * 配置缺省时不落库，由 Service 给出默认值（默认按验收口径算完成比例、三类异常都拦截），
 * 避免「没配置过就放行」。
 */
@TableName("icbc_purchase_setting")
@KeySequence("icbc_purchase_setting_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IcbcPurchaseSettingDO extends TenantBaseDO {

    /** 主键 */
    @TableId
    private Long id;

    /** 完成比例采用的履约口径：ACCEPTED-验收口径，SETTLED-结算口径 */
    private String performanceBasis;

    /** 超量交货的处理方式：BLOCK-拦截，APPROVAL-提交授权审核 */
    private String overQuantityRule;

    /** 过期交货的处理方式：BLOCK-拦截，APPROVAL-提交授权审核 */
    private String expiredRule;

    /** 跨场站交货的处理方式：BLOCK-拦截，APPROVAL-提交授权审核 */
    private String crossStationRule;

    /** 备注 */
    private String remark;

}
