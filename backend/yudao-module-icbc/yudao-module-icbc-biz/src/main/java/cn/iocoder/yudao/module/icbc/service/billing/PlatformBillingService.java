package cn.iocoder.yudao.module.icbc.service.billing;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.icbc.controller.admin.billing.vo.IcbcBillingLedgerPageReqVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.billing.IcbcBillingLedgerDO;

import java.util.List;

/**
 * 平台计费计量 Service（#16）。
 *
 * <p>计费口径只有这一处：按「成功开具的报废产品收购发票张数」计量，蓝票被成功红冲后不再计入，
 * 红票本身不计。计量结果落 {@code icbc_billing_ledger}（平台自己的账，租户不可写）。
 */
public interface PlatformBillingService {

    /**
     * 重新计量某个租户某个期间的计费结果并落台账（同一租户 × 期间幂等覆盖）。
     *
     * @param tenantId    被计费的回收企业租户
     * @param periodMonth 计费期间（yyyy-MM）
     * @return 计量台账
     */
    IcbcBillingLedgerDO generate(Long tenantId, String periodMonth);

    /**
     * 重新计量某期间全部有开票的租户并落台账，返回本期各租户的计量结果。
     */
    List<IcbcBillingLedgerDO> generate(String periodMonth);

    /**
     * 分页查询计量台账。
     */
    PageResult<IcbcBillingLedgerDO> getPage(IcbcBillingLedgerPageReqVO reqVO);

}
