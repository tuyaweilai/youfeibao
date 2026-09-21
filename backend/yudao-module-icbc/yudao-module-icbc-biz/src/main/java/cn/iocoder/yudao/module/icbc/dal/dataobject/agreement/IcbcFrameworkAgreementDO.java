package cn.iocoder.yudao.module.icbc.dal.dataobject.agreement;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 框架收购协议 DO
 *
 * <p>每个出售者一份、长期有效（见 issue #6 验收）。税总 5 号公告第十七条要求收购合同
 * 或协议载明名称、数量、规格、回收期次、结算方式，故这几个字段是不可为空的核心要素。
 * 同一出售者重新签署时旧协议作废、保留历史，保证「留痕」。
 */
@TableName("icbc_framework_agreement")
@KeySequence("icbc_framework_agreement_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IcbcFrameworkAgreementDO extends TenantBaseDO {

    @TableId
    private Long id;

    /**
     * 出售者（收方）ID
     */
    private Long payeeId;

    /**
     * 协议编号
     */
    private String agreementNo;

    /**
     * 货物名称
     */
    private String productName;

    /**
     * 数量（含计量单位，如「5 吨」）
     */
    private String quantity;

    /**
     * 规格
     */
    private String specification;

    /**
     * 回收期次（如「2026 年 9 月第 1 期」）
     */
    private String recyclePeriod;

    /**
     * 结算方式（如「银行转账、过磅后 3 日内结清」）
     */
    private String settlementMethod;

    /**
     * 签署方式：ELECTRONIC-电子签章，PAPER-纸质签署
     */
    private String signMethod;

    /**
     * 第三方签署任务号（合同组任务号）。
     *
     * <p>电子签署发起成功后写入；回调靠它把「合同组整体签完」的通知路由回本协议
     * （第三方回调只带子客编号与任务号，不带我们的协议编号）。纸质协议为空。
     */
    private String signTaskId;

    /**
     * 签署时间
     */
    private LocalDateTime signedAt;

    /**
     * 协议**主文书**（框架收购协议）文件地址。
     *
     * <p>一个合同组里有两份文书，这是其中的主文书：一票一档证据链的「框架收购协议」条目、
     * 后台协议列表的下载入口都取它。告知函地址单独放 {@link #noticeFileUrl}，两份文书在证据链上
     * **分别成条**（ADR 0036 决策 3：证据上两份文书要能分别引用，所以不拼成一个 PDF）。
     */
    private String fileUrl;

    /**
     * 反向发票合规告知函（合同组里的第二份文书）文件地址。
     *
     * <p>与协议同属一个合同组、同一次签署完成，但独立成条进证据链（同归 {@code FRAMEWORK_AGREEMENT}
     * 这一类型 / 合同流，不新增证据类型、不新增第六流）。
     */
    private String noticeFileUrl;

    /**
     * 状态：0-待签署，1-生效，2-作废，见 {@link cn.iocoder.yudao.module.icbc.enums.FrameworkAgreementStatusEnum}
     */
    private Integer status;

    /**
     * 备注
     */
    private String remark;

}
