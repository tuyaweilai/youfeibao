package cn.iocoder.yudao.module.icbc.service.acquisition;

import cn.iocoder.yudao.module.icbc.dal.dataobject.acquisition.IcbcAcquisitionDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.invoice.InvoiceOrderDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.invoice.RedInvoiceDO;

import java.util.List;
import java.util.Map;

/**
 * 收购进度 Service（ADR 0038）。
 *
 * <p>收购单上的档位**只能**由这里派生并落库，业务代码不得直接写
 * {@code icbc_acquisition.status}。判定依据是该笔的开票单的四条状态线
 * （自然人确认 / 预开票 / 付款 / 开票），异常则以叠加标注给出、不占档位。
 *
 * <p>写入点是幂等的：通知重复、乱序、早到都不会把档位推错——每次都是拿当前真值重算。
 */
public interface AcquisitionProgressService {

    /**
     * 派生一笔收购的进度（纯函数，不落库）。{@code order} 为空表示这一笔还没发起开票申请。
     */
    AcquisitionProgress derive(IcbcAcquisitionDO acquisition, InvoiceOrderDO order);

    /**
     * 同上的完整形态：{@code redInvoice} 是这张蓝票最近一次红冲记录，可为空。
     *
     * <p>红冲**不回退档位**（货款真的付了、蓝票真的开过），只作为异常标注呈现。
     */
    AcquisitionProgress derive(IcbcAcquisitionDO acquisition, InvoiceOrderDO order, RedInvoiceDO redInvoice);

    /**
     * 批量派生（列表用，一次查库、不逐行 N+1）。
     *
     * @return 收购单编号 → 进度；列表里没有开票单的也会给出「已登记」
     */
    Map<Long, AcquisitionProgress> deriveFor(List<IcbcAcquisitionDO> acquisitions);

    /**
     * 把一笔收购的档位缓存刷成派生结果（幂等）。
     */
    void sync(IcbcAcquisitionDO acquisition);

    /**
     * 按合作方订单号收敛：付款 / 开票 / 预开票取消等回调都走这里。
     *
     * <p>查不到开票单时**不动**收购单——通知可能早于数据落库，留给重放。
     */
    void syncByPartnerOrderId(String partnerOrderId);

}
