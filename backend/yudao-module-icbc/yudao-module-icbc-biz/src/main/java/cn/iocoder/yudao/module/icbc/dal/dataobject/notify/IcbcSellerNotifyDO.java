package cn.iocoder.yudao.module.icbc.dal.dataobject.notify;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 出售者触达记录 DO（#36，ADR 0023）。
 *
 * <p>每一次「本该发的触达」都留一条记录，无论最终发没发出去：
 * <ul>
 *   <li><b>幂等</b>：{@code (tenant_id, biz_type, biz_key)} 唯一，同一业务事件只发一次，不重复轰炸；</li>
 *   <li><b>可解释</b>：没发出去要写清原因（开关关闭 / 未留手机号 / 没配入口 / 通道失败）；</li>
 *   <li><b>可转达</b>：记录里同时留着一次性令牌链接，收货员可复制转达——首次交易、从未留号的场景只有这条通路。</li>
 * </ul>
 */
@TableName("icbc_seller_notify")
@KeySequence("icbc_seller_notify_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IcbcSellerNotifyDO extends TenantBaseDO {

    /** 主键 */
    @TableId
    private Long id;

    /** 触达类型，枚举 {@link cn.iocoder.yudao.module.icbc.enums.SellerNotifyTypeEnum} */
    private String bizType;

    /**
     * 业务键（幂等键）：结算单用「结算单号:版本号」，付款异常用「合作方订单号:支付状态」，
     * 发票开出用「合作方订单号」。同一键只发一次。
     */
    private String bizKey;

    /** 短信模板编码，枚举 {@link cn.iocoder.yudao.module.icbc.enums.SellerNotifyTypeEnum#getTemplateCode()} */
    private String templateCode;

    /** 自然人主体编号（可空） */
    private Long naturalPersonId;

    /** 收方（出售者）档案编号（可空） */
    private Long payeeId;

    /** 出售者姓名快照 */
    private String sellerName;

    /** 接收手机号（留号才能发短信；为空即未留号） */
    private String mobile;

    /** 发送状态，枚举 {@link cn.iocoder.yudao.module.icbc.enums.SellerNotifyStatusEnum} */
    private Integer status;

    /** 短信发送日志编号（工单 ID），发送成功时回填 */
    private Long smsLogId;

    /** 短信正文（渲染后的最终文案，便于排查与人工转达） */
    private String content;

    /** 一次性令牌链接（打开即可查看，无需注册） */
    private String link;

    /** 失败原因 / 未发送说明 */
    private String errorMsg;

    /** 发送时间（实际交给通道的时刻） */
    private LocalDateTime sendTime;

}
