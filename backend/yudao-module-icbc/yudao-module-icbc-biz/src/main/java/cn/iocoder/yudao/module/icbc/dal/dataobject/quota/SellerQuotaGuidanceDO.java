package cn.iocoder.yudao.module.icbc.dal.dataobject.quota;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 出售者额度超限的「办理经营主体登记」引导记录 DO。
 *
 * <p>自然人出售者连续 12 个月反向开票累计销售额超过 500 万元后，回收企业不能再为其反向
 * 开票——他必须办理经营主体登记、以主体身份开票。工行只做事后补缴，事前拦截是平台的责任，
 * 所以平台除了拒绝开票，还要留下一条可跟进、可结案的记录：谁超了、超多少、因为哪笔业务被
 * 发现、引导到哪一步了。
 *
 * <p>一个出售者在同一租户下同时只有一条<b>未办结</b>的记录（{@link #status} 不是「已办结」）。
 * 再次触发不新增，只更新累计已用额度与最近触发时间——否则每被拒一次就多一条，清单会烂掉。
 */
@TableName("icbc_seller_quota_guidance")
@KeySequence("icbc_seller_quota_guidance_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class SellerQuotaGuidanceDO extends TenantBaseDO {

    @TableId
    private Long id;

    /** 出售者档案编号 */
    private Long payeeId;

    /** 出售者姓名 */
    private String sellerName;

    /** 身份证号码 */
    private String idCardNo;

    /**
     * 触发场景，见 {@link cn.iocoder.yudao.module.icbc.enums.SellerQuotaTriggerSceneEnum}
     */
    private String triggerScene;

    /** 触发业务单号（收购单号 / 合作方订单号） */
    private String triggerBizNo;

    /** 触发时连续 12 个月累计已用额度（元） */
    private BigDecimal usedAmount;

    /** 触发时窗口上限（元），500 万 */
    private BigDecimal capAmount;

    /** 引导状态：0-待引导，1-已引导，2-已办结 */
    private Integer status;

    /** 首次触发时间 */
    private LocalDateTime triggeredAt;

    /** 最近一次触发时间 */
    private LocalDateTime lastTriggeredAt;

    /** 处理时间 */
    private LocalDateTime handledAt;

    /** 处理说明 */
    private String handleRemark;

    /** 备注 */
    private String remark;

}
