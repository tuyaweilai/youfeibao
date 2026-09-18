package cn.iocoder.yudao.module.icbc.dal.dataobject.evidence;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 一票一档证据 DO。
 *
 * <p>系统能从业务表自动取到的证据（资金流的支付单、发票流的发票原件、信息流的
 * 台账条目）不落本表；本表只收需要人工补录的材料，主要是合同流与货物流。
 * {@code flow} 由 {@code evidenceType} 推导，落库时一并写入，便于按流统计齐备率。
 */
@TableName("icbc_evidence")
@KeySequence("icbc_evidence_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IcbcEvidenceDO extends TenantBaseDO {

    @TableId
    private Long id;

    /** 发票订单ID */
    private Long invoiceOrderId;

    /** 合作方订单号（一票一档的档号） */
    private String partnerOrderId;

    /** 所属流，枚举 {@link cn.iocoder.yudao.module.icbc.enums.EvidenceFlowEnum} */
    private String flow;

    /** 证据类型，枚举 {@link cn.iocoder.yudao.module.icbc.enums.IcbcEvidenceTypeEnum} */
    private String evidenceType;

    /** 证据标题（默认取证据类型名称，可覆盖） */
    private String title;

    /** 证据文件地址（外部 URL 或上传后的地址） */
    private String fileUrl;

    /** 证据文件名称 */
    private String fileName;

    /** 证据对应业务发生时间 */
    private LocalDateTime occurredTime;

    /** 备注 */
    private String remark;

}
