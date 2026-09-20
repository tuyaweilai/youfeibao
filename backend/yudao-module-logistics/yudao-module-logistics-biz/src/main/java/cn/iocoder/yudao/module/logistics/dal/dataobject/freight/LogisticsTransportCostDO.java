package cn.iocoder.yudao.module.logistics.dal.dataobject.freight;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 运输费用（内部成本）DO（V8 #75）。
 *
 * <p>记的是**本企业自己的运输成本**（路桥、燃油、其他），不是承运商运费：
 * 后者是「付给承运商的应付」，走 {@code logistics_freight_order}。两本账分开的理由见
 * CONTEXT.md「运费」——成本是内部口径，运费是对承运商的应付。
 *
 * <p>它的存在是为了让「自有车不虚造承运商运费」落到实处：自有车没有承运商，就**不该也不能**
 * 建运费单；但它真实发生的路桥与燃油要按**实际承担方**（{@code bearer}）记下来，
 * 于是成本口径不混。
 *
 * <p>本表的金额**不进入**运费单，也**不进入**收购单 / 发票（CONTEXT.md「运费」）。
 */
@TableName("logistics_transport_cost")
@KeySequence("logistics_transport_cost_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogisticsTransportCostDO extends TenantBaseDO {

    /** 主键 */
    @TableId
    private Long id;

    /** 运输任务编号 */
    private Long taskId;
    /** 运输任务单号（冗余） */
    private String taskNo;

    /**
     * 费用类型
     *
     * 枚举 {@link cn.iocoder.yudao.module.logistics.enums.LogisticsTransportCostTypeEnum}
     */
    private Integer costType;

    /** 费用名称（自由文本，如「绕行过路费」） */
    private String name;

    /** 金额 */
    private BigDecimal amount;

    /**
     * 承担方（实际是谁出的）
     *
     * 枚举 {@link cn.iocoder.yudao.module.logistics.enums.LogisticsFreightBearerEnum}
     */
    private Integer bearer;

    /** 发生日期 */
    private LocalDate occurDate;

    /** 备注 */
    private String remark;

}
