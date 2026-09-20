package cn.iocoder.yudao.module.icbc.dal.dataobject.handover;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 磅次 DO（#50 T12）：一次过磅留下的**原始读数**。
 *
 * <p>同一车同一品类可能过磅多次（复磅、拆装、重称）。每一次都是一条只读的事实，
 * **只有被选定的那一次（{@code effective = true}）参与计量**，其余留档但不参与（见 CONTEXT
 * 「有效磅次」）。计量结果（收购单）引用的是有效磅次的值与版本（{@code seqNo}），
 * 所以批次一旦产生了收购单，就不允许再改有效磅次。
 */
@TableName("icbc_weighing")
@KeySequence("icbc_weighing_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IcbcWeighingDO extends TenantBaseDO {

    /** 主键 */
    @TableId
    private Long id;

    /** 交接批次编号 */
    private Long batchId;

    /** 第几次磅次（同一批次内从 1 递增；计量结果引用它的值与此版本号） */
    private Integer seqNo;

    /** 毛重 */
    private BigDecimal grossWeight;

    /** 皮重 */
    private BigDecimal tareWeight;

    /** 净重 = 毛重 − 皮重 */
    private BigDecimal netWeight;

    /** 过磅时间 */
    private LocalDateTime weighTime;

    /** 磅单号 */
    private String weightTicketNo;

    /** 磅单照片地址 */
    private String weightTicketImageUrl;

    /** 磅单上的车牌号（与批次车牌不一致时能被看出来，磅单不会串到别的批次） */
    private String plateNo;

    /** 是否有效磅次：true-参与计量，false-留档不参与（同一批次至多一条为 true） */
    private Boolean effective;

    /** 备注（如复磅原因） */
    private String remark;

}
