package cn.iocoder.yudao.module.icbc.dal.dataobject.settlement;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import cn.iocoder.yudao.module.icbc.enums.SettlementConfirmStatusEnum;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 结算单 DO（ADR 0018）。
 *
 * <p>按「一次到场批次」聚合同一出售者、同一场站的若干张收购单。**确认与异议都发生在结算单上**；
 * 付款与开票仍按收购单逐笔进行（工行一票一付）。
 *
 * <p>结算单**不维护**「是否结清」的状态：那由其下的收购单推导（还有未完成的不叫已结清，ADR 0021）。
 * {@code confirmStatus} 只表达「出售者是否认可这一版计量与计价事实」。
 */
@TableName("icbc_settlement")
@KeySequence("icbc_settlement_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IcbcSettlementDO extends TenantBaseDO {

    /** 主键 */
    @TableId
    private Long id;

    /** 结算单号（平台生成，唯一） */
    private String settlementNo;

    /** 出售者（收方）档案编号 */
    private Long payeeId;

    /** 自然人主体编号（平台级身份，跨企业复用） */
    private Long naturalPersonId;

    /** 出售者姓名快照 */
    private String sellerName;

    /** 出售者联系方式快照 */
    private String sellerMobile;

    /** 场站编号（ADR 0018：一次到场批次 = 同出售者 + 同场站；历史数据为空） */
    private Long stationId;

    /** 场站名称快照（生成时固化，避免场站改名后历史结算单对不上） */
    private String stationName;

    /** 离线批次键（现场端同一批用同一个值；在线为空） */
    private String batchKey;

    /** 生成时间（收货员点「结束本次收货」的时刻） */
    private LocalDateTime generateTime;

    /** 生成人（收货员用户编号） */
    private Long generatedBy;

    /** 当前生效版本指针 */
    private Long currentVersionId;

    /** 当前生效版本号 */
    private Integer currentVersionNo;

    // ==================== 确认留痕（ADR 0024：勾选 + 完整留痕） ====================

    /** 确认状态，枚举 {@link SettlementConfirmStatusEnum} */
    private Integer confirmStatus;

    /** 确认时间 */
    private LocalDateTime confirmTime;

    /** 确认时 IP */
    private String confirmIp;

    /** 确认时设备（User-Agent） */
    private String confirmDevice;

    /** 确认时结算单快照的哈希（证明他认可的是哪一版数据） */
    private String confirmHash;

    /** 确认人（登录凭证：会员用户编号） */
    private Long confirmMemberUserId;

    // ==================== 异议（ADR 0022：固定枚举 + 两个企业动作） ====================

    /** 最近一次异议原因（枚举） */
    private String disputeReason;

    /** 异议说明 */
    private String disputeNote;

    /** 最近一次异议时间 */
    private LocalDateTime disputeTime;

    /** 累计异议次数；连续 3 次以上提示转线下 */
    private Integer disputeCount;

    /** 企业处理说明（「不改但附说明」时必填） */
    private String enterpriseReplyNote;

    /** 企业最近一次处理时间 */
    private LocalDateTime enterpriseReplyTime;

    // ==================== 超时（到期不自动确认） ====================

    /** 下一步动作的截止时间；到期不自动确认，而是升级为线下签字 */
    private LocalDateTime deadlineTime;

    // ==================== 线下签字逃生门 ====================

    /** 线下签字确认书附件地址 */
    private String offlineSignFileUrl;

    /** 办理人 */
    private String offlineSignHandler;

    /** 线下签字确认时间 */
    private LocalDateTime offlineSignTime;

    /** 备注 */
    private String remark;

}
