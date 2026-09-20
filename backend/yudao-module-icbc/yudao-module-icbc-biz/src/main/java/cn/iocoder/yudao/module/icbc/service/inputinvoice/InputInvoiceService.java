package cn.iocoder.yudao.module.icbc.service.inputinvoice;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.icbc.controller.admin.inputinvoice.vo.InputInvoiceLinkReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.inputinvoice.vo.InputInvoiceLinkRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.inputinvoice.vo.InputInvoicePageReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.inputinvoice.vo.InputInvoiceRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.inputinvoice.vo.InputInvoiceSaveReqVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.inputinvoice.IcbcInputInvoiceDO;

import java.util.List;

/**
 * 进项收票 Service 接口（#49 T11，ADR 0029）。
 *
 * <p>登记单位供货方开给回收企业的进项发票，并勾稽到采购单据（收购单 / 采购订单 / 入库单），
 * 让票、货、款三者对得上。三条硬约束：
 * <ul>
 *     <li>**登记唯一**：按「销方 + 发票号码」唯一，同一张票不能重复登记；</li>
 *     <li>**勾稽不超限**：单张单据上的累计勾稽金额不得超过调用方给出的单据金额，一张票的累计
 *         勾稽金额不得超过其价税合计；</li>
 *     <li>**状态可筛选**：已登记 / 部分勾稽 / 已勾稽，由勾稽金额与价税合计推导。</li>
 * </ul>
 */
public interface InputInvoiceService {

    /**
     * 登记进项发票（同一销方 + 发票号码已登记时拒绝）。
     *
     * @param createReqVO 登记信息
     * @return 进项发票编号
     */
    Long createInvoice(InputInvoiceSaveReqVO createReqVO);

    /**
     * 修改进项发票（仅「已登记」且未勾稽的票可改）。
     *
     * @param updateReqVO 修改信息
     */
    void updateInvoice(InputInvoiceSaveReqVO updateReqVO);

    /**
     * 删除进项发票（仅未勾稽的票可删）。
     *
     * @param id 进项发票编号
     */
    void deleteInvoice(Long id);

    /**
     * 获得进项发票（不存在抛异常）。
     *
     * @param id 进项发票编号
     * @return 进项发票
     */
    IcbcInputInvoiceDO getInvoice(Long id);

    /**
     * 获得进项发票详情（含勾稽记录）。
     *
     * @param id 进项发票编号
     * @return 详情
     */
    InputInvoiceRespVO getDetail(Long id);

    /**
     * 获得进项发票分页。
     *
     * @param pageReqVO 分页条件
     * @return 分页
     */
    PageResult<InputInvoiceRespVO> getInvoicePage(InputInvoicePageReqVO pageReqVO);

    /**
     * 把进项票勾稽到一张单据。
     *
     * <p>金额上限按调用方给出的 {@code bizAmount} 校验，不依赖采购订单 / 入库单的实现。
     *
     * @param reqVO 勾稽信息
     * @return 勾稽记录
     */
    InputInvoiceLinkRespVO linkToBiz(InputInvoiceLinkReqVO reqVO);

    /**
     * 取消勾稽。
     *
     * @param linkId 勾稽记录编号
     */
    void unlink(Long linkId);

    /**
     * 获得某张进项票的勾稽记录。
     *
     * @param invoiceId 进项发票编号
     * @return 勾稽记录列表
     */
    List<InputInvoiceLinkRespVO> getLinkList(Long invoiceId);

}
