package cn.iocoder.yudao.module.icbc.dal.dataobject.acquisition;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import cn.iocoder.yudao.module.icbc.enums.AcquisitionStatusEnum;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 收购登记单 DO。
 *
 * <p>收货员在收购现场为「谁卖的、卖的什么、多少量、什么价、车牌、货物照片、什么时候在哪儿」
 * 留下的唯一业务凭证。它同时承载五流中的三流骨架：
 * <ul>
 *   <li>合同流：本单即该笔收购的合同/确认书（{@code acquisitionNo}）；</li>
 *   <li>货物流：磅单（{@code weightTicket*}）与车头车尾照片（{@code vehicle*ImageUrl}）；</li>
 *   <li>信息流：时间（{@code tradeTime}）、地点（{@code tradeAddress}）、出售者及联系方式、
 *       报废产品名称、数量、价格。</li>
 * </ul>
 * 资金流与发票流由后续付款 / 开票环节补齐，通过 {@code invoicePartnerOrderId} 挂回本单。
 */
@TableName("icbc_acquisition")
@KeySequence("icbc_acquisition_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IcbcAcquisitionDO extends TenantBaseDO {

    /** 主键 */
    @TableId
    private Long id;

    /** 收购单号（平台生成，唯一；也是合同流里的合同编号） */
    private String acquisitionNo;

    /**
     * 客户端幂等键。离线补传时由现场端生成并保持稳定，服务端据此去重，
     * 保证「断网先存本地、恢复后补传」不产生重复单据。
     */
    private String clientRequestId;

    /** 出售者（收方）档案编号 */
    private Long payeeId;

    /**
     * 卖方主体类型快照，枚举 {@link cn.iocoder.yudao.module.erp.enums.purchase.SellerSubjectTypeEnum}（六态）。
     *
     * <p>反向开票通道只对自然人开放（ADR 0029）：自然人出售者走反向开票，其余五类由对方开票、
     * 我们收票。该字段是采购单据上「走哪条取票链路」的判定依据，开票申请与预下单都会据此做硬校验。
     */
    private Integer sellerSubjectType;

    /** 出售者外部编号（合作方收方编号）快照 */
    private String partnerPayeeId;

    /** 出售者姓名快照（信息流要求，避免档案改名后对不上） */
    private String sellerName;

    /** 出售者联系方式快照（信息流要求） */
    private String sellerMobile;

    // ==================== 品类与价格（按品类自动带出） ====================

    /** 品类配置编号 */
    private Long goodsConfigId;

    /** 品类名称 */
    private String categoryName;

    /** 计量单位（按品类带出） */
    private String unit;

    /** 税率（按品类带出） */
    private BigDecimal taxRate;

    /** 计税方法：SIMPLE-简易计税，GENERAL-一般计税（按品类带出） */
    private String taxMethod;

    /** 商品和服务税收分类合并编码（按品类带出） */
    private String mergedCode;

    /** 规格 */
    private String specification;

    /** 数量 */
    private BigDecimal quantity;

    /** 含税单价（元） */
    private BigDecimal unitPrice;

    /** 金额（元） */
    private BigDecimal amount;

    // ==================== 货物流：过磅与车辆 ====================

    /** 毛重 */
    private BigDecimal grossWeight;

    /** 皮重 */
    private BigDecimal tareWeight;

    /** 净重 */
    private BigDecimal netWeight;

    // ==================== 计价模型（ADR 0019：结算重量是唯一计价基准） ====================

    /**
     * 扣杂**原始值**：{@link #deductionMethod} 为 WEIGHT 时是重量，为 RATIO 时是比例（0~1）。
     * 只存原始录法，换算结果体现在 {@link #settlementWeight}，不用自由文本覆盖结算重量。
     */
    private BigDecimal deduction;

    /** 扣杂录法：WEIGHT-按重量，RATIO-按比例，枚举 {@link cn.iocoder.yudao.module.icbc.enums.DeductionMethodEnum} */
    private String deductionMethod;

    /** 结算重量 = 毛重 − 皮重 − 扣杂；本平台唯一计价基准 */
    private BigDecimal settlementWeight;

    /** 调整项（元，可正可负；运费 / 补贴 / 折让） */
    private BigDecimal adjustmentAmount;

    /** 调整原因；调整项非零时必填 */
    private String adjustmentReason;

    /** 数量口径说明：数量与磅单净重不再相等时的解释（如「结算重量计价，含扣杂」） */
    private String quantityNote;

    // ==================== 接收结论与称量差异（#53 T15，ADR 0028） ====================

    /**
     * 接收量：本次实际接收（留场 / 进库存）的重量；为空表示尚未做接收结论（验收）。
     *
     * <p>实物在库量按此字段取值（有值时优先于 {@link #netWeight}），扣杂只扣价款、不扣库存（ADR 0028）。
     */
    private BigDecimal acceptedWeight;

    /** 退回量：因质量 / 规格不合格退回出售者的重量；拒收部分不进应付、不进库存 */
    private BigDecimal rejectedWeight;

    /** 余货出场量：本次未接收、随车（或随后）带离场站的余货重量 */
    private BigDecimal residualWeight;

    /** 拒收原因；退回量大于 0 时必填（差异要有人解释） */
    private String rejectReason;

    /**
     * 称量差异 = 实物量 − 结算重量（实物量取 {@link #acceptedWeight}，未做接收结论时取 {@link #netWeight}）。
     *
     * <p>正数表示实物多于计价（多收 / 未计价），负数表示计价多于实物（短缺 / 含扣杂）。
     * 只要两侧都算得出来就落库，**不许静默抹平**——ADR 0028 要求差额进异常表可见。
     */
    private BigDecimal weightDiff;

    // ==================== 结算单归属（#33，ADR 0018） ====================

    /** 所属结算单编号；为空表示尚未归入结算单（可被「结束本次收货」归组） */
    private Long settlementId;

    // ==================== 采购安排关联（#51 T13） ====================

    /**
     * 关联的采购订单编号；{@code 0} 表示未关联，即「直接收购」（零散散户不虚造订单，报表照常统计）。
     *
     * <p>与 {@link #purchaseOrderItemId} 成对：关联时必须同时给出，且该订单必须「执行中且未过期」
     * （门禁在 {@code PurchaseOrderService#assertUsableAsPurchaseBasis}）。
     */
    private Long purchaseOrderId;

    /** 关联的采购订单明细编号；{@code 0} 表示未关联。一条明细可分多次收货，多张收购单可挂同一明细 */
    private Long purchaseOrderItemId;

    // ==================== 交接批次与有效磅次（#50 T12） ====================

    /** 交接批次编号；为空表示这笔收购没有经过批次登记（历史数据与直接登记） */
    private Long handoverBatchId;

    // ==================== 现场交接登记来源与要件状态（V6 #73） ====================

    /**
     * 物流侧交接登记编号：本笔收购的**现场交接来源**（上门提货才有）。
     *
     * <p>追溯时 icbc 按它反查物流的读取面，把这一趟的运输节点与现场凭证拉进一票一档
     *（方向恒为 icbc → 物流，ADR 0032）。
     */
    private Long logisticsHandoverId;

    /** 司机编号（物流侧编号；与 {@link #driverName} 快照并存） */
    private Long driverId;

    /** 车辆编号（物流侧编号；与 {@link #vehiclePlateNo} 快照并存） */
    private Long vehicleId;

    /**
     * 要件状态：COMPLETE-已齐，PENDING-待补档（V6 #73，ADR 0030 第 4 条）
     *
     * <p>枚举 {@link cn.iocoder.yudao.module.icbc.enums.AcquisitionDocumentStatusEnum}。
     * **待补档的收购单被付款与开票门禁拦住**：不进开票申请、不进台账口径、不计入额度；
     * 补档后放行（{@link #documentCompletedAt} 留办理人与时间）。
     */
    private String documentStatus;

    /** 缺什么（待补档时说明，如「缺身份证」） */
    private String documentGap;

    /** 现场参考量快照（**不是计量事实**：计量取有效磅次） */
    private BigDecimal referenceQuantity;

    /** 现场参考单价快照（未修正时即成交单价） */
    private BigDecimal referenceUnitPrice;

    /** 修正现场参考价 / 参考量的原因；修正必填（改动要被解释，不能被抹平） */
    private String referenceFixReason;

    /** 补档完成时间 */
    private LocalDateTime documentCompletedAt;

    /** 补档办理人（系统用户编号） */
    private Long documentCompletedBy;

    /** 补档说明 */
    private String documentCompleteRemark;

    /**
     * 有效磅次编号：计量结果引用的就是它。
     * 为空表示这单的重量是人工录入的，不是从有效磅次取的。
     */
    private Long weighingId;

    /** 有效磅次是第几次（版本号快照）；配合 {@link #weighingId} 让「按哪一次磅次计量」可回查 */
    private Integer weighingSeqNo;

    /** 场站编号（ADR 0018：一次到场批次按「出售者 + 场站」聚合；历史数据为空） */
    private Long stationId;

    /** 离线批次键：现场端同一批用同一个值，补传后服务端按它归入同一结算单 */
    private String batchKey;

    /** 作废原因；作废对自然人可见（货已收，作废是敏感动作） */
    private String cancelReason;

    // ==================== 运输信息（司机不参与确认 / 收款，不占自然人主体） ====================

    /** 司机姓名 */
    private String driverName;

    /** 司机手机号 */
    private String driverMobile;

    /** 磅单号 */
    private String weightTicketNo;

    /** 磅单照片地址 */
    private String weightTicketImageUrl;

    /** 磅单识别出的车牌号 */
    private String weightTicketPlateNo;

    /** 车头车尾照片识别出的车牌号 */
    private String vehiclePlateNo;

    /** 车牌比对结果：true-一致，false-不一致，null-缺少任一侧车牌无法比对 */
    private Boolean plateMatched;

    /** 车头照片地址 */
    private String vehicleFrontImageUrl;

    /** 车尾照片地址 */
    private String vehicleRearImageUrl;

    // ==================== 信息流：时间与地点 ====================

    /** 交易地点 */
    private String tradeAddress;

    /** 交易时间 */
    private LocalDateTime tradeTime;

    /** 结算方式 */
    private String settlementMethod;

    // ==================== 状态与关联 ====================

    /** 状态，枚举 {@link AcquisitionStatusEnum} */
    private Integer status;

    /** 关联的开票合作方订单号（开票链路建立后回填） */
    private String invoicePartnerOrderId;

    /** 登记来源：ONLINE-在线登记，OFFLINE_SYNC-离线补传 */
    private String source;

    /** 备注 */
    private String remark;

    /**
     * 可入库实物量（唯一取数点，ADR 0028）：接收量有值优先（#53），否则取净重（过磅实物口径）。
     *
     * <p>结算重量只作计价基准，不直接等于实物量；库存（#52）与差异清单都用这个口径，
     * 不要在调用方各自判断「用哪个重量」。
     */
    public BigDecimal resolvePhysicalWeight() {
        return acceptedWeight != null ? acceptedWeight : netWeight;
    }

    /**
     * 是否**待补档**（V6 #73）：缺身份证或银行卡的交接照记事实，但付款与开票要被门禁拦住。
     *
     * <p>历史数据（{@code documentStatus} 为空）视为「已齐」：不因新增字段把老单据变成走不通的。
     */
    public boolean isDocumentPending() {
        return cn.iocoder.yudao.module.icbc.enums.AcquisitionDocumentStatusEnum.PENDING.getStatus()
                .equals(documentStatus);
    }

}
