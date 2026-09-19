package cn.iocoder.yudao.module.icbc.service.quota;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.icbc.controller.admin.quota.vo.SellerQuotaCheckRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.quota.vo.SellerQuotaGuidanceHandleReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.quota.vo.SellerQuotaGuidancePageReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.quota.vo.SellerQuotaRespVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.quota.SellerQuotaGuidanceDO;

import java.math.BigDecimal;

/**
 * 自然人出售者额度台账与风控（issue #12）。
 *
 * <p>额度是<b>自然人</b>的，不是租户的：同一个自然人在本平台多个租户下的反向开票额合并
 * 计算，连续 12 个月滚动窗口上限 500 万元。工行没有事前校验接口、也没有额度视图，跨平台
 * 累计更看不到——这个台账是平台自己的责任，也是唯一能「在票开出来之前拦住」的地方。
 *
 * <p>三个使用场景：
 * <ul>
 *   <li>收购登记时给收货员<b>余量提示</b>（{@link #checkQuota}）；</li>
 *   <li>开票申请前做<b>硬校验</b>（{@link #checkQuota} 的结论进前置校验项）；</li>
 *   <li>出售者自己查余量，<b>不依赖任何客户端</b>（公开令牌端点取 {@link #getQuota}）。</li>
 * </ul>
 *
 * <p>超限被拒后还要留下可跟进的 {@link SellerQuotaGuidanceDO}：拒绝本身不是流程的终点，
 * 引导出售者办理经营主体登记才是。
 */
public interface NaturalPersonQuotaService {

    /**
     * 取某个出售者的额度台账：滚动窗口内的已用 / 余量、按月销售额、1% 与 3% 分列。
     *
     * @param payeeId 出售者档案编号（任意租户均可，汇总时跨租户合并）
     * @return 额度台账
     */
    SellerQuotaRespVO getQuota(Long payeeId);

    /**
     * 判定「再开这笔金额会不会超 500 万」。
     *
     * <p>不抛异常：收购登记要的是提示，开票申请要的是逐条列出的失败原因与补齐方式。
     *
     * @param payeeId     出售者档案编号
     * @param applyAmount 本次金额（可为空，视为 0）
     * @return 结论；{@code passed=false} 时 {@code message} 说明哪里不满足、{@code remedy} 说明怎么补
     */
    SellerQuotaCheckRespVO checkQuota(Long payeeId, BigDecimal applyAmount);

    /**
     * 取某个自然人在某个月的<b>跨租户</b>净销售额。
     *
     * <p>税总 5 号公告第十三条的 10 万元免征线是「自然人 × 月」的，与租户无关：同一个人
     * 在多个回收企业卖货，免征额度只有一份。代办税费申报用它判定本月的销售额是否超线。
     *
     * @param payeeId 出售者档案编号
     * @param month   yyyy-MM
     * @return 该月跨租户净销售额（已开票 + 在途 − 红冲）；无销售返回 0
     */
    BigDecimal getCrossTenantMonthlyNetAmount(Long payeeId, String month);

    /**
     * 记录一条「引导出售者办理经营主体登记」的记录。
     *
     * <p>同一出售者同时只有一条未办结记录：再次触发只更新已用额度与最近触发时间，不新增。
     * 只有因 500 万上限被拒时才调用。
     *
     * @param payeeId 出售者档案编号
     * @param scene   触发场景，见 {@link cn.iocoder.yudao.module.icbc.enums.SellerQuotaTriggerSceneEnum}
     * @param bizNo   触发业务单号（收购单号 / 合作方订单号）
     */
    void recordGuidance(Long payeeId, String scene, String bizNo);

    /**
     * 分页查询额度超限引导记录
     */
    PageResult<SellerQuotaGuidanceDO> getGuidancePage(SellerQuotaGuidancePageReqVO reqVO);

    /**
     * 处理一条引导记录：已引导 / 已办结
     */
    void handleGuidance(SellerQuotaGuidanceHandleReqVO reqVO);

}
