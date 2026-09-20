package cn.iocoder.yudao.module.logistics.dal.dataobject.carrier;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 承运合同 DO（V8 #75）。
 *
 * <p>CONTEXT.md「承运合同」：回收企业与承运商之间关于**运价与计费方式**的约定——有效期、适用线路或
 * 品类、按车 / 按吨 / 按公里、附加费与其承担方。它是**运费对账**的依据，与「采购合同」是两份契约
 * （采购合同是买货，承运合同是买运输服务）。
 *
 * <p>{@code route} 与 {@code goodsConfigId} 是「适用线路或品类」：两者**至少填一个**（服务层校验），
 * 因为一份不限定任何范围的运价无法解释「这条运价凭什么适用于这趟货」。品类与 icbc 交接登记同构：
 * 物流只存 icbc 侧的 {@code goodsConfigId} + 名称快照，不引用它的类（ADR 0032）。
 *
 * <p>{@code surcharges} 是附加费列表的 JSON 文本（如路桥 / 燃油附加费），每项带**承担方**
 *（{@code LogisticsFreightBearerEnum}）；运单汇集运费时快照下来，据此决定进不进应付。
 *
 * <p>停用而不是删除（与承运商档案一致）：历史运单上的合同编号与运价快照要留着。
 */
@TableName("logistics_carrier_contract")
@KeySequence("logistics_carrier_contract_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogisticsCarrierContractDO extends TenantBaseDO {

    /** 主键 */
    @TableId
    private Long id;

    /** 合同编号（租户内唯一，对外可见） */
    private String contractNo;

    /** 承运商编号 */
    private Long carrierId;
    /** 承运商名称快照（改名或删档不影响历史运单） */
    private String carrierName;

    /** 生效日期 */
    private LocalDate effectiveFrom;
    /** 失效日期（为空表示长期） */
    private LocalDate effectiveTo;

    /** 适用线路（可空；与适用品类至少填一个） */
    private String route;

    /** 适用品类编号（icbc 侧编号，可空） */
    private Long goodsConfigId;
    /** 适用品类名称快照 */
    private String categoryName;

    /**
     * 计费方式
     *
     * 枚举 {@link cn.iocoder.yudao.module.logistics.enums.LogisticsFreightBillingModeEnum}
     */
    private Integer billingMode;

    /** 运价（按计费方式：每趟 / 每吨 / 每公里） */
    private BigDecimal unitPrice;

    /**
     * 附加费列表（JSON 数组文本）
     *
     * 每项形如 {@code {"name":"路桥附加费","amount":100.00,"bearer":2}}，
     * {@code bearer} 见 {@link cn.iocoder.yudao.module.logistics.enums.LogisticsFreightBearerEnum}。
     */
    private String surcharges;

    /**
     * 状态：0-生效，1-已停用
     *
     * <p>与 {@link cn.iocoder.yudao.framework.common.enums.CommonStatusEnum} 对齐。
     */
    private Integer status;

    /** 备注 */
    private String remark;

}
