package cn.iocoder.yudao.module.icbc.dal.dataobject.purchaseorder;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 采购订单履约异常授权单 DO（#47 T09）。
 *
 * <p>超量 / 过期 / 跨场站交货被企业配置成「提交授权审核」时，先落一条本记录；审核通过后，它成为
 * 这次交货继续办理的依据（授权范围：超量的**追加量**、过期 / 跨场站的**有效期与场站**）。
 *
 * <p>只追加不放宽：一条授权单只放宽它自己那件事，不会把订单变回「执行中」，也不改任何已发生的业务。
 * 已通过的授权单被后续交货引用（{@code icbc_acquisition} 侧经门禁校验即可，不反写本表）。
 */
@TableName("icbc_purchase_exception")
@KeySequence("icbc_purchase_exception_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IcbcPurchaseExceptionDO extends TenantBaseDO {

    /** 主键 */
    @TableId
    private Long id;

    /** 授权单号（平台生成，租户内唯一） */
    private String exceptionNo;

    /** 采购订单编号 */
    private Long orderId;

    /** 采购订单号快照 */
    private String orderNo;

    /** 采购订单明细编号（超量交货必填，其余可空表示订单级） */
    private Long itemId;

    /** 品类名称快照 */
    private String categoryName;

    /** 异常类型：OVER_QUANTITY / EXPIRED / CROSS_STATION */
    private String exceptionType;

    /** 本次交货场站编号（跨场站交货必填） */
    private Long stationId;

    /** 本次交货场站名称快照 */
    private String stationName;

    /** 本次申请的交货量（正数） */
    private BigDecimal requestedQuantity;

    /** 审核通过的追加量（超量交货的授权上限；其余类型为空） */
    private BigDecimal approvedQuantity;

    /** 授权有效期止（含当日；空表示不设有效期） */
    private LocalDate validUntil;

    /** 提交原因（必填） */
    private String reason;

    /** 状态：0-待审核，1-已通过，2-已拒绝 */
    private Integer status;

    /** 提交人 */
    private Long requestedBy;

    /** 提交时间 */
    private LocalDateTime requestedTime;

    /** 审核人 */
    private Long reviewedBy;

    /** 审核时间 */
    private LocalDateTime reviewedTime;

    /** 审核意见 */
    private String reviewRemark;

}
