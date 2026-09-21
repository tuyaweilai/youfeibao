package cn.iocoder.yudao.module.icbc.service.esign;

import cn.iocoder.yudao.module.icbc.controller.admin.esign.vo.EsignActivateReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.esign.vo.EsignOpenConsoleRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.esign.vo.EsignTenantStatusRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.esign.vo.PlatformEsignQuotaSaveReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.esign.vo.PlatformEsignTenantRespVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.esign.IcbcEsignTenantDO;

import java.util.List;

/**
 * 租户级电子签章配置与开通 Service（#92，ADR 0036）。
 *
 * <p>章是**租户级**的：回收企业才是发起方，平台不代盖。所以开通流程按「企业 × 租户」走：
 * 管理员在本租户点「开通电子签」→ 拿到一次性控制台链接 → 在第三方完成企业认证与创建企业印章 →
 * 回后台确认，状态落「已激活 + 印章就位」。
 *
 * <p><b>子客编号由我们生成、持久化、不可变、不可重复</b>：它是第三方回执里唯一的租户锚点，
 * 回调靠它反查出是哪家回收企业（{@link #resolveTenantIdBySubCustomerNo(String)}）。
 */
public interface EsignTenantService {

    /**
     * 本租户的配置；没有就地创建（子客编号从第一次访问起就稳定）。
     */
    IcbcEsignTenantDO getOrCreateCurrent();

    /**
     * 本租户当前的电子签章状态（开通到哪一步、印章是否就位、额度还剩多少）。
     */
    EsignTenantStatusRespVO getStatus();

    /**
     * 开通入口：拿一枚一次性控制台链接去完成企业认证与制章。平台参数未配置时明确失败。
     */
    EsignOpenConsoleRespVO openConsole();

    /**
     * 确认已激活：企业认证通过 + 企业印章已创建。缺印章编号直接拒绝——「已激活 + 印章就位」是两件事。
     */
    void activate(EsignActivateReqVO reqVO);

    /**
     * 本租户是否已激活且印章就位。这是电子签章可用性的租户侧输入之一（另一个是平台参数齐备）。
     */
    boolean isTenantActivated();

    /**
     * 记一次合同额度消耗（#95）：**发起签署成功时**调用，一份合同组加一。
     *
     * <p>额度是否够用的判据只有 {@code EsignPort#isAvailable} 一处；本方法只负责把「已用」
     * 原子加一，不在这里重复判额度，免得两处口径漂移。
     */
    void consumeContract();

    /**
     * 按子客编号反查租户编号（回调路径，跨租户）。查不到返回 {@code null}，由调用方决定怎么处理——不猜。
     */
    Long resolveTenantIdBySubCustomerNo(String subCustomerNo);

    /**
     * 全平台租户的电子签章状态（平台运营只读，跨租户）：含回收企业名称，未开通的也列出来。
     */
    List<PlatformEsignTenantRespVO> listPlatformTenants();

    /**
     * 平台运营调整某租户的合同额度。
     */
    void updateQuota(PlatformEsignQuotaSaveReqVO reqVO);

}
